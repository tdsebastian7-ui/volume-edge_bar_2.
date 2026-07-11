package com.example.volumegestureapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button

class MainActivity : Activity() {
    private lateinit var settingsManager: SettingsManager
    private var currentTheme: String = "system"

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsManager = SettingsManager(this)
        currentTheme = settingsManager.theme
        when (currentTheme) {
            "light" -> setTheme(android.R.style.Theme_Material_Light_NoActionBar)
            "dark" -> setTheme(android.R.style.Theme_Material_NoActionBar)
            else -> setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val btnStartService = findViewById<Button>(R.id.btn_start_service)
        btnStartService.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        val btnSettings = findViewById<Button>(R.id.btn_settings)
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // If theme has changed in settings, recreate activity
        if (settingsManager.theme != currentTheme) {
            recreate()
        }
    }
}
