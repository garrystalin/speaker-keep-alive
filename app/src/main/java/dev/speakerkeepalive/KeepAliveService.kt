package dev.speakerkeepalive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import java.util.concurrent.Executors

class KeepAliveService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var wakeLock: PowerManager.WakeLock? = null

    private val tick = object : Runnable {
        override fun run() {
            executor.execute { runCatching { Beeper.play(applicationContext) } }
            handler.postDelayed(this, Prefs.intervalMin.get(this@KeepAliveService) * 60_000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        if (wakeLock == null) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "speakerkeepalive:tick").apply {
                setReferenceCounted(false)
                acquire()
            }
        }

        handler.removeCallbacks(tick)
        handler.post(tick) // первый сигнал сразу, дальше — по интервалу
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        executor.shutdown()
        wakeLock?.release()
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            )
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text, Prefs.intervalMin.get(this)))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "keepalive"
        private const val NOTIF_ID = 1

        fun start(c: Context) {
            val i = Intent(c, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i)
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, KeepAliveService::class.java))
        }
    }
}
