package com.example.kuotaguard

import android.app.*
import android.content.Intent
import android.net.TrafficStats
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat

class MonitorService : Service() {

    companion object {
        private const val CH_STATUS = "status"
        private const val CH_ALERT = "alert"
        private const val NOTIF_STATUS = 1
        private const val NOTIF_ALERT = 2

        private const val SECONDS_NEEDED = 10
        private const val BYTES_PER_SEC = 2 * 1024L
        private const val COOLDOWN_MS = 60_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastBytes = 0L
    private var streak = 0
    private var lastAlert = 0L

    private val tick = object : Runnable {
        override fun run() {
            val now = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
            if (now < 0) { stopSelf(); return }

            val delta = now - lastBytes
            lastBytes = now

            streak = if (delta > BYTES_PER_SEC) streak + 1 else 0

            val t = System.currentTimeMillis()
            if (streak >= SECONDS_NEEDED && t - lastAlert > COOLDOWN_MS) {
                showAlert()
                lastAlert = t
                streak = 0
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_STATUS, "Status pemantau", NotificationManager.IMPORTANCE_MIN)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Peringatan kuota", NotificationManager.IMPORTANCE_HIGH)
        )

        val status = NotificationCompat.Builder(this, CH_STATUS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("KuotaGuard aktif")
            .setOngoing(true)
            .build()
        startForeground(NOTIF_STATUS, status)

        lastBytes = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
        handler.postDelayed(tick, 1000)
    }

    private fun showAlert() {
        val panel = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            this, 0, panel, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val n = NotificationCompat.Builder(this, CH_ALERT)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Kamu sedang memakai kuota!")
            .setContentText("Ketuk untuk matikan data / nyalakan Wi-Fi.")
            .setContentIntent(pi)
            .addAction(0, "Buka panel koneksi", pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        getSystemService(NotificationManager::class.java).notify(NOTIF_ALERT, n)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
