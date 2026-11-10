package com.bih.app.autofill

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bih.app.data.SettingsStore
import java.util.Locale

// ---------------------------------------------------------------------------
// USER'S IDEA (Nov 2026): "I need cross-app integration because of my brain
// injury — a way to view whatever is on screen and interact with it: fill
// out what's on the page with my private details, or send a message."
//
// This is the Android Accessibility Service that makes it possible. It can
// see any app's screen and act on it — the same Android mechanism TalkBack
// uses. B.I.H. is an assistive app, so this is its legitimate purpose.
//
// Safety design (risk S12 — this service is powerful by nature):
//  - It NEVER watches the screen continuously. It only looks when the user
//    taps the system accessibility button and chooses B.I.H.
//  - It shows a confirmation screen listing every box + value BEFORE
//    filling anything. Nothing fills silently.
//  - It only fills boxes that are currently EMPTY — it never overwrites
//    something the user already typed.
//  - It never touches password fields, never makes network calls, and
//    reads only the on-device encrypted profile cache.
//  - "Send/Submit" is tapped only if the user explicitly ticks that box
//    on the confirmation screen.
// ---------------------------------------------------------------------------
class BihAccessibilityService : AccessibilityService() {

    private var buttonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null

    // The confirmation screen talks back to us through this broadcast.
    private val fillConfirmedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val includeSubmit = intent.getBooleanExtra(EXTRA_INCLUDE_SUBMIT, false)
            performFill(includeSubmit)
        }
    }

    override fun onServiceConnected() {
        ContextCompat.registerReceiver(
            this, fillConfirmedReceiver,
            IntentFilter(ACTION_FILL_CONFIRMED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // The system accessibility button (a small floating button the user
        // can enable in Android settings) triggers our "fill this screen".
        val cb = object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) {
                proposeFill()
            }
        }
        buttonCallback = cb
        accessibilityButtonController.registerAccessibilityButtonCallback(cb)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        buttonCallback?.let { accessibilityButtonController.unregisterAccessibilityButtonCallback(it) }
        buttonCallback = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    // Step 1: look at the current screen, find empty boxes we recognise,
    // and ask the user to review before anything happens.
    private fun proposeFill() {
        val profile = SettingsStore(this).profileCache
        if (profile.isEmpty()) {
            toast("First add your details in B.I.H. → Forms → My details")
            return
        }
        val root = rootInActiveWindow
        if (root == null) {
            toast("Nothing on screen to read")
            return
        }
        val matches = mutableListOf<Pair<String, String>>() // label -> value
        collectMatches(root, profile, matches)
        val hasSubmit = findSubmit(root) != null
        root.recycle()

        if (matches.isEmpty()) {
            toast("No empty boxes I recognise on this screen")
            return
        }
        startActivity(
            ScreenFillConfirmationActivity.buildIntent(
                this,
                ArrayList(matches.map { it.first }),
                ArrayList(matches.map { it.second }),
                hasSubmit,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    // Step 2: only runs AFTER the user tapped "Fill" on the confirmation
    // screen. We re-scan the screen fresh (it may have changed) and fill
    // each recognised empty box.
    private fun performFill(includeSubmit: Boolean) {
        val profile = SettingsStore(this).profileCache
        val root = rootInActiveWindow ?: return
        val matched = mutableListOf<Pair<AccessibilityNodeInfo, String>>()
        collectNodes(root, profile, matched)

        matched.forEach { (node, value) ->
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            node.recycle()
        }
        if (includeSubmit) {
            findSubmit(root)?.let { submit ->
                submit.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                submit.recycle()
            }
        }
        root.recycle()
        toast(
            if (matched.isEmpty()) "The screen changed — nothing was filled"
            else "Filled ${matched.size} box(es). Check them before sending."
        )
    }

    // --- Screen scanning ---------------------------------------------------

    private fun collectMatches(
        node: AccessibilityNodeInfo,
        profile: Map<String, String>,
        out: MutableList<Pair<String, String>>,
    ) {
        profileKeyFor(node)?.let { key ->
            val value = profile[key]
            if (!value.isNullOrBlank() && node.text.isNullOrBlank()) {
                val label = node.hintText?.toString()?.takeIf { it.isNotBlank() } ?: labelFor(key)
                out.add(label to value)
            }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectMatches(it, profile, out) }
        }
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        profile: Map<String, String>,
        out: MutableList<Pair<AccessibilityNodeInfo, String>>,
    ) {
        profileKeyFor(node)?.let { key ->
            val value = profile[key]
            if (!value.isNullOrBlank() && node.text.isNullOrBlank()) {
                out.add(AccessibilityNodeInfo.obtain(node) to value)
            }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodes(it, profile, out) }
        }
    }

    private fun findSubmit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = node.className?.toString().orEmpty()
        val looksClickable = node.isClickable || className.contains("Button", ignoreCase = true)
        if (looksClickable) {
            val text = listOfNotNull(
                node.text?.toString(),
                node.contentDescription?.toString(),
            ).joinToString(" ").lowercase(Locale.US)
            if (Regex("\\b(send|submit|sign|continue|next|apply|confirm|done)\\b")
                .containsMatchIn(text)
            ) {
                return AccessibilityNodeInfo.obtain(node)
            }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findSubmit(child)?.let { return it }
            }
        }
        return null
    }

    // Conservative field matching, same rulebook as the autofill service.
    // USER REQUEST: "signature" boxes are filled with the full name — a
    // typed name is a legally recognised e-signature for most online forms.
    private fun profileKeyFor(node: AccessibilityNodeInfo): String? {
        val className = node.className?.toString().orEmpty()
        val editable = node.isEditable || className.contains("EditText", ignoreCase = true)
        if (!editable || node.isPassword) return null
        val hay = listOfNotNull(
            node.hintText?.toString(),
            node.contentDescription?.toString(),
            node.viewIdResourceName,
            node.text?.toString(),
        ).joinToString(" ").lowercase(Locale.US)
        fun has(vararg words: String) = words.any { Regex("\\b$it\\b").containsMatchIn(hay) }
        return when {
            has("email") -> "email"
            has("phone", "tel", "telephone", "mobile") -> "phone"
            has("dob", "birthdate", "birthday") || hay.contains("birth") -> "dob"
            has("address", "postal", "street") -> "address"
            has("signature") || hay.contains("sign here") -> "full_name"
            has("name", "fullname", "surname") && !has("username", "login") -> "full_name"
            else -> null
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

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {
        const val ACTION_FILL_CONFIRMED = "com.bih.app.action.FILL_CONFIRMED"
        const val EXTRA_INCLUDE_SUBMIT = "com.bih.app.extra.INCLUDE_SUBMIT"
    }
}
