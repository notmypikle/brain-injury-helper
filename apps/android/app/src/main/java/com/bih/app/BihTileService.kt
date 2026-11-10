package com.bih.app

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.TileService

// USER REQUEST (2026-11-10): instant access to B.I.H. from anywhere, like
// Gemini/Bixby. This Quick Settings tile puts B.I.H. one swipe-and-tap
// away on every screen, even when the app is closed. (A spoken wake word
// needs phone-maker privileges — see PROJECT_STATUS.md notes.)
class BihTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startActivityAndCollapse(pending)
    }
}
