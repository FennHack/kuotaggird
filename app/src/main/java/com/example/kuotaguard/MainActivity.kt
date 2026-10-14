package com.example.kuotaguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val askNotif =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startMonitor() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val info = TextView(this).apply {
            text = "Memberi notifikasi jika data seluler terpakai terus selama 10 detik."
            textSize = 16f
        }
        val start = Button(this).apply {
            text = "Aktifkan pemantau"
            setOnClickListener { ensurePermissionThenStart() }
        }
        val stop = Button(this).apply {
            text = "Matikan pemantau"
            setOnClickListener { stopService(Intent(this@MainActivity, MonitorService::class.java)) }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(info); addView(start); addView(stop)
        })
    }

    private fun ensurePermissionThenStart() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startMonitor() else askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startMonitor() {
        ContextCompat.startForegroundService(this, Intent(this, MonitorService::class.java))
    }
}
