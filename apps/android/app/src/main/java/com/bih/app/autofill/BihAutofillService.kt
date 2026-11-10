package com.bih.app.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import com.bih.app.R
import com.bih.app.data.SettingsStore
import java.util.Locale

// Fills web/app form fields from the paperwork profile cache.
//
// Privacy rules (risk S11):
// - Reads ONLY the local profile cache. No network calls here, ever.
// - Never fills passwords, usernames, or card fields.
// - Nothing is filled until the user reviews every field on the
//   confirmation screen and taps "Fill".
// - onSaveRequest is ignored: we never capture what you type on sites.
class BihAutofillService : AutofillService() {

    data class Match(val id: AutofillId, val label: String, val value: String)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        val profile = SettingsStore(this).profileCache
        if (structure == null || profile.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val matches = mutableListOf<Match>()
        for (i in 0 until structure.windowNodeCount) {
            collect(structure.getWindowNodeAt(i).rootViewNode, profile, matches)
        }
        if (matches.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val intent = FillConfirmationActivity.buildIntent(this, matches)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val sender = pending?.intentSender
        if (sender == null) {
            callback.onSuccess(null)
            return
        }

        val presentation = RemoteViews(packageName, R.layout.autofill_banner)
        val response = FillResponse.Builder()
            .setAuthentication(matches.map { it.id }.toTypedArray(), sender, presentation)
            .build()
        callback.onSuccess(response)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Never capture or store what the user types into other apps/sites.
        callback.onSuccess()
    }

    private fun collect(
        node: AssistStructure.ViewNode,
        profile: Map<String, String>,
        out: MutableList<Match>,
    ) {
        val key = profileKeyFor(node)
        if (key != null) {
            val value = profile[key]
            val id = node.autofillId
            if (!value.isNullOrBlank() && id != null) {
                val label = node.hint?.takeIf { it.isNotBlank() } ?: labelFor(key)
                out.add(Match(id, label, value))
            }
        }
        for (i in 0 until node.childCount) {
            collect(node.getChildAt(i), profile, out)
        }
    }

    private fun labelFor(key: String) = when (key) {
        "full_name" -> "Full name"
        "dob" -> "Date of birth"
        "address" -> "Address"
        "phone" -> "Phone"
        "email" -> "Email"
        else -> key
    }

    // Conservative mapping from a form field to a profile key.
    // Returns null for anything we're not confident about (risk S11).
    private fun profileKeyFor(node: AssistStructure.ViewNode): String? {
        if (node.autofillId == null) return null
        if (node.autofillType != View.AUTOFILL_TYPE_TEXT) return null

        val inputType = node.inputType
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val klass = inputType and InputType.TYPE_MASK_CLASS
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            (klass == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        ) {
            return null
        }

        val hints = node.autofillHints
            ?.map { it.lowercase(Locale.US) }
            ?: emptyList()
        if (hints.any { it.contains("password") || it.contains("creditcard") || it == "username" }) {
            return null
        }

        val html = buildString {
            node.htmlInfo?.attributes?.forEach { attr ->
                append(attr.first).append('=').append(attr.second).append(';')
            }
        }.lowercase(Locale.US)
        if (html.contains("type=password") || html.contains("type=hidden")) return null

        val hay = "$html ${node.idEntry?.lowercase(Locale.US).orEmpty()} ${hints.joinToString(" ")}"
        fun has(vararg words: String) = words.any { Regex("\\b$it\\b").containsMatchIn(hay) }

        return when {
            has("email") -> "email"
            has("phone", "tel", "telephone", "mobile") -> "phone"
            has("dob", "birthdate", "birthday") || hay.contains("birth") -> "dob"
            has("address", "postal", "street") -> "address"
            // User request: signature boxes get the typed full name (a
            // legally recognised e-signature on most online forms).
            has("signature") || hay.contains("sign here") -> "full_name"
            has("name", "fullname", "surname") && !has("username", "login") -> "full_name"
            else -> null
        }
    }
}
