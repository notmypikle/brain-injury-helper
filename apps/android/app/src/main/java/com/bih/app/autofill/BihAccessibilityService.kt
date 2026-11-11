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
// USER NEED (Nov 2026): cross-app help — view whatever is on screen and
// interact with it: fill out what's on the page with saved private
// details, or send a message. A core accessibility need for our users.
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
    private val lens = LensController(this)

    // The lens's 🔍 button asks for the whole-screen flow via this bridge.
    private val fullscreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            proposeHelp()
        }
    }

    // The confirmation screen talks back to us through this broadcast,
    // returning the exact list the user reviewed.
    private val fillConfirmedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val includeSubmit = intent.getBooleanExtra(EXTRA_INCLUDE_SUBMIT, false)
            val labels = intent.getStringArrayListExtra(EXTRA_CONFIRM_LABELS) ?: return
            val values = intent.getStringArrayListExtra(EXTRA_CONFIRM_VALUES) ?: return
            performFill(includeSubmit, labels.zip(values))
        }
    }

    override fun onServiceConnected() {
        ContextCompat.registerReceiver(
            this, fillConfirmedReceiver,
            IntentFilter(ACTION_FILL_CONFIRMED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this, fullscreenReceiver,
            IntentFilter(ACTION_FULLSCREEN_PICK),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = lens.show()
            },
            IntentFilter(ACTION_SHOW_LENS),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // The system accessibility button (a small floating button the user
        // can enable in Android settings) triggers our "fill this screen".
        val cb = object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) {
                proposeHelp()
            }
        }
        buttonCallback = cb
        accessibilityButtonController.registerAccessibilityButtonCallback(cb)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        buttonCallback?.let { accessibilityButtonController.unregisterAccessibilityButtonCallback(it) }
        buttonCallback = null
        lens.hide()
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    // Step 1: look at the current screen ONCE, then offer the user the
    // things we can do with it (read it / fill it) — one simple choice.
    // User request 2026-11-11: "can it read what's on the screen when I
    // need it to, for help understanding texts, emails and documents?"
    private fun proposeHelp() {
        val root = rootInActiveWindow
        if (root == null) {
            toast("Nothing on screen to read")
            return
        }
        val screenText = collectScreenText(root)
        val profile = SettingsStore(this).profileCache
        val matches = mutableListOf<Pair<String, String>>() // label -> value
        if (profile.isNotEmpty()) {
            collectMatches(root, profile, matches)
        }
        val hasSubmit = findSubmit(root) != null
        root.recycle()

        if (screenText.isBlank() && matches.isEmpty()) {
            toast("Nothing readable on this screen")
            return
        }
        startActivity(
            AssistMenuActivity.buildIntent(
                this,
                screenText,
                ArrayList(matches.map { it.first }),
                ArrayList(matches.map { it.second }),
                hasSubmit,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    // Gather the screen's visible text, in reading order, de-duplicated.
    // Password fields are NEVER read (same safety rulebook as filling).
    private fun collectScreenText(root: AccessibilityNodeInfo): String {
        val parts = LinkedHashSet<String>()
        collectText(root, parts)
        var text = parts.joinToString("\n")
        if (text.length > MAX_SCREEN_TEXT) {
            text = text.take(MAX_SCREEN_TEXT) + "\n… (screen text shortened)"
        }
        return text
    }

    private fun collectText(node: AccessibilityNodeInfo, out: MutableSet<String>) {
        if (!node.isPassword) {
            node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(out::add)
            node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(out::add)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, out) }
        }
    }

    // Step 2: only runs AFTER the user tapped "Fill" on the confirmation
    // screen. Security review fix (Nov 2026): we fill ONLY the boxes the
    // user saw and confirmed — a hostile app can't sneak new boxes in while
    // the confirmation screen is showing. The screen is re-scanned to find
    // the live nodes, but every fill must match a confirmed label+value.
    private fun performFill(includeSubmit: Boolean, confirmed: List<Pair<String, String>>) {
        val profile = SettingsStore(this).profileCache
        val root = rootInActiveWindow ?: return
        val matched = mutableListOf<Pair<AccessibilityNodeInfo, String>>()
        collectConfirmedNodes(root, profile, confirmed.toSet(), matched)

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
                out.add(labelForNode(node, key) to value)
            }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectMatches(it, profile, out) }
        }
    }

    private fun labelForNode(node: AccessibilityNodeInfo, key: String): String =
        node.hintText?.toString()?.takeIf { it.isNotBlank() } ?: labelFor(key)

    private fun collectConfirmedNodes(
        node: AccessibilityNodeInfo,
        profile: Map<String, String>,
        confirmed: Set<Pair<String, String>>,
        out: MutableList<Pair<AccessibilityNodeInfo, String>>,
    ) {
        profileKeyFor(node)?.let { key ->
            val value = profile[key]
            if (!value.isNullOrBlank() && node.text.isNullOrBlank()) {
                val label = labelForNode(node, key)
                if ((label to value) in confirmed) {
                    out.add(AccessibilityNodeInfo.obtain(node) to value)
                }
            }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectConfirmedNodes(it, profile, confirmed, out) }
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
        const val EXTRA_CONFIRM_LABELS = "com.bih.app.extra.CONFIRM_LABELS"
        const val EXTRA_CONFIRM_VALUES = "com.bih.app.extra.CONFIRM_VALUES"
        const val ACTION_FULLSCREEN_PICK = "com.bih.app.action.FULLSCREEN_PICK"
        const val ACTION_SHOW_LENS = "com.bih.app.action.SHOW_LENS"
        private const val MAX_SCREEN_TEXT = 20000 // ~10 pages: documents can be a lot (user, 2026-11-11)
    }
}
