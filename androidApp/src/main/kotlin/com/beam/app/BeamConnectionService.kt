package com.beam.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper

private const val CHANNEL_ID = "beam_connection"
private const val NOTIFICATION_ID = 1
private const val INACTIVITY_TIMEOUT_MS = 10 * 60 * 1000L

/**
 * Keeps BeamCore's discovery+server alive while the app is backgrounded, so a Share-sheet send
 * or a scan-to-pair doesn't have to wait for cold-start discovery. Self-stops after a period of
 * no app activity — MainActivity re-pings this service (via onStartCommand) each time it resumes.
 */
class BeamConnectionService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        BeamCore.ensureStarted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(stopRunnable)
        handler.postDelayed(stopRunnable, INACTIVITY_TIMEOUT_MS)
        return START_STICKY
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Beam connectivity", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Beam is discoverable")
            .setContentText("Ready to receive from paired devices")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        BeamCore.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
