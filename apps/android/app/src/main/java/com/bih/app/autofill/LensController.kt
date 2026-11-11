package com.bih.app.autofill

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.bih.app.R

// ---------------------------------------------------------------------------
// The READ LENS (user request 2026-11-11): "Could the floating button act
// more like a magnifying accessory and not copy every single word on a
// page?"
//
// A small draggable overlay: drag the lens over a paragraph, tap ✔️, and
// B.I.H. reads/saves ONLY the text under the lens — the smallest node with
// real text whose bounds contain the lens center. 🔍 picks the lens text
// (and falls back to a whole-screen capture if the lens finds nothing);
// the full-screen flow stays one tap away in the chooser.
//
// Same safety rulebook as everything in this service: reads only on the
// user's tap, never passwords, never stores anything itself.
// ---------------------------------------------------------------------------
class LensController(private val service: AccessibilityService) {

    private var overlay: View? = null
    private val wm get() = service.getSystemService(WindowManager::class.java)

    fun isShowing() = overlay != null

    fun show() {
        if (overlay != null) return
        val view = View.inflate(service, R.layout.read_lens, null)

        val size = (service.resources.displayMetrics.widthPixels * 0.6f).toInt()
        val params = WindowManager.LayoutParams(
            size, (size * 0.7f).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (service.resources.displayMetrics.widthPixels - size) / 2
            y = (service.resources.displayMetrics.heightPixels - size) / 3
        }

        // Drag the lens anywhere by its handle; tap a button to act.
        val handle = view.findViewById<View>(R.id.lens_drag_handle)
        handle.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f; private var startY = 0f
            private var startParamX = 0; private var startParamY = 0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.rawX; startY = e.rawY
                        startParamX = params.x; startParamY = params.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = startParamX + (e.rawX - startX).toInt()
                        params.y = startParamY + (e.rawY - startY).toInt()
                        wm?.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })

        view.findViewById<View>(R.id.lens_confirm).setOnClickListener { confirm(view) }
        view.findViewById<View>(R.id.lens_fullscreen).setOnClickListener {
            hide()
            // Fall back to the whole-screen chooser.
            service.sendBroadcast(
                Intent(BihAccessibilityService.ACTION_FULLSCREEN_PICK).setPackage(service.packageName),
            )
        }
        view.findViewById<View>(R.id.lens_cancel).setOnClickListener { hide() }

        wm?.addView(view, params)
        overlay = view
    }

    fun hide() {
        overlay?.let { runCatching { wm?.removeView(it) } }
        overlay = null
    }

    // Read ONLY the text under the lens center.
    private fun confirm(view: View) {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val cx = loc[0] + view.width / 2
        val cy = loc[1] + view.height / 2
        val root = service.rootInActiveWindow
        if (root == null) {
            toast("Nothing on screen to read")
            return
        }
        val picked = deepestTextAt(root, cx, cy)
        val text = picked?.trim().orEmpty()
        root.recycle()
        if (text.isBlank()) {
            toast("Nothing readable under the lens — try 🔍 for the whole screen")
            return
        }
        hide()
        service.startActivity(
            ReadScreenActivity.buildIntent(service, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    // The smallest text-bearing node under the point, so the lens grabs a
    // paragraph instead of the whole page.
    private fun deepestTextAt(node: AccessibilityNodeInfo, x: Int, y: Int): String? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.contains(x, y)) return null
        if (node.isPassword) return null
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                deepestTextAt(child, x, y)?.let { child.recycle(); return it }
                child.recycle()
            }
        }
        val own = node.text?.toString()?.takeIf { it.isNotBlank() }
            ?: node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
        return own
    }

    private fun toast(message: String) =
        Toast.makeText(service, message, Toast.LENGTH_LONG).show()
}
