package com.example.volumegestureapp

import android.app.Activity
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.view.View

class SettingsActivity : Activity() {

    private lateinit var settingsManager: SettingsManager
    
    // UI elements
    private lateinit var viewEdgePreview: View
    private lateinit var viewPopupPreview: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        settingsManager = SettingsManager(this)
        
        // Apply theme before inflating view
        when (settingsManager.theme) {
            "light" -> setTheme(android.R.style.Theme_Material_Light_NoActionBar)
            "dark" -> setTheme(android.R.style.Theme_Material_NoActionBar)
            else -> setTheme(android.R.style.Theme_Material_Light_NoActionBar) // Fallback/default Light
        }
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupThemeSettings()
        setupTriggerSettings()
        setupSidebarLockSettings()
        setupEdgeSettings()
        setupPopupSettings()
    }

    private fun setupSidebarLockSettings() {
        val switch = findViewById<Switch>(R.id.switch_sidebar_locked)
        switch.isChecked = settingsManager.sidebarLocked
        switch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.sidebarLocked = isChecked
        }
    }

    private fun setupTriggerSettings() {
        val rgTriggerMode = findViewById<RadioGroup>(R.id.rg_trigger_mode)
        
        when (settingsManager.triggerMode) {
            "edge_swipe" -> rgTriggerMode.check(R.id.rb_trigger_edge_swipe)
            else -> rgTriggerMode.check(R.id.rb_trigger_long_press)
        }

        rgTriggerMode.setOnCheckedChangeListener { _, checkedId ->
            val newMode = when (checkedId) {
                R.id.rb_trigger_edge_swipe -> "edge_swipe"
                else -> "long_press"
            }
            if (newMode != settingsManager.triggerMode) {
                settingsManager.triggerMode = newMode
            }
        }
    }

    private fun setupThemeSettings() {
        val rgTheme = findViewById<RadioGroup>(R.id.rg_theme)
        
        when (settingsManager.theme) {
            "light" -> rgTheme.check(R.id.rb_theme_light)
            "dark" -> rgTheme.check(R.id.rb_theme_dark)
            else -> rgTheme.check(R.id.rb_theme_system)
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                R.id.rb_theme_light -> "light"
                R.id.rb_theme_dark -> "dark"
                else -> "system"
            }
            if (newTheme != settingsManager.theme) {
                settingsManager.theme = newTheme
                recreate() // Re-apply theme dynamically
            }
        }
    }

    private fun setupEdgeSettings() {
        viewEdgePreview = findViewById(R.id.view_edge_preview)
        
        val sbEdgeA = findViewById<SeekBar>(R.id.sb_edge_a)
        val sbEdgeR = findViewById<SeekBar>(R.id.sb_edge_r)
        val sbEdgeG = findViewById<SeekBar>(R.id.sb_edge_g)
        val sbEdgeB = findViewById<SeekBar>(R.id.sb_edge_b)

        val sbEdgeWidth = findViewById<SeekBar>(R.id.sb_edge_width)
        val sbEdgeHeight = findViewById<SeekBar>(R.id.sb_edge_height)
        
        val tvEdgeWidth = findViewById<TextView>(R.id.tv_edge_width)
        val tvEdgeHeight = findViewById<TextView>(R.id.tv_edge_height)

        // Init values
        sbEdgeA.progress = settingsManager.edgeColorA
        sbEdgeR.progress = settingsManager.edgeColorR
        sbEdgeG.progress = settingsManager.edgeColorG
        sbEdgeB.progress = settingsManager.edgeColorB
        
        sbEdgeWidth.progress = settingsManager.edgeWidth
        sbEdgeHeight.progress = settingsManager.edgeHeight

        updateEdgePreview()
        tvEdgeWidth.text = "Pill Width (${settingsManager.edgeWidth}dp)"
        tvEdgeHeight.text = "Pill Height (${settingsManager.edgeHeight}dp)"

        val colorListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    when (seekBar?.id) {
                        R.id.sb_edge_a -> settingsManager.edgeColorA = progress
                        R.id.sb_edge_r -> settingsManager.edgeColorR = progress
                        R.id.sb_edge_g -> settingsManager.edgeColorG = progress
                        R.id.sb_edge_b -> settingsManager.edgeColorB = progress
                    }
                    updateEdgePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        sbEdgeA.setOnSeekBarChangeListener(colorListener)
        sbEdgeR.setOnSeekBarChangeListener(colorListener)
        sbEdgeG.setOnSeekBarChangeListener(colorListener)
        sbEdgeB.setOnSeekBarChangeListener(colorListener)

        sbEdgeWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settingsManager.edgeWidth = progress
                    tvEdgeWidth.text = "Pill Width (${progress}dp)"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbEdgeHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settingsManager.edgeHeight = progress
                    tvEdgeHeight.text = "Pill Height (${progress}dp)"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupPopupSettings() {
        viewPopupPreview = findViewById(R.id.view_popup_preview)
        
        val sbPopupA = findViewById<SeekBar>(R.id.sb_popup_a)
        val sbPopupR = findViewById<SeekBar>(R.id.sb_popup_r)
        val sbPopupG = findViewById<SeekBar>(R.id.sb_popup_g)
        val sbPopupB = findViewById<SeekBar>(R.id.sb_popup_b)

        val sbPopupWidth = findViewById<SeekBar>(R.id.sb_popup_width)
        val sbPopupHeight = findViewById<SeekBar>(R.id.sb_popup_height)
        
        val tvPopupWidth = findViewById<TextView>(R.id.tv_popup_width)
        val tvPopupHeight = findViewById<TextView>(R.id.tv_popup_height)

        // Init values
        sbPopupA.progress = settingsManager.popupColorA
        sbPopupR.progress = settingsManager.popupColorR
        sbPopupG.progress = settingsManager.popupColorG
        sbPopupB.progress = settingsManager.popupColorB
        
        sbPopupWidth.progress = settingsManager.popupWidth
        sbPopupHeight.progress = settingsManager.popupHeight

        updatePopupPreview()
        tvPopupWidth.text = "Popup Width (${settingsManager.popupWidth}dp)"
        tvPopupHeight.text = "Popup Height (${settingsManager.popupHeight}dp)"

        val colorListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    when (seekBar?.id) {
                        R.id.sb_popup_a -> settingsManager.popupColorA = progress
                        R.id.sb_popup_r -> settingsManager.popupColorR = progress
                        R.id.sb_popup_g -> settingsManager.popupColorG = progress
                        R.id.sb_popup_b -> settingsManager.popupColorB = progress
                    }
                    updatePopupPreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        sbPopupA.setOnSeekBarChangeListener(colorListener)
        sbPopupR.setOnSeekBarChangeListener(colorListener)
        sbPopupG.setOnSeekBarChangeListener(colorListener)
        sbPopupB.setOnSeekBarChangeListener(colorListener)

        sbPopupWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settingsManager.popupWidth = progress
                    tvPopupWidth.text = "Popup Width (${progress}dp)"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbPopupHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settingsManager.popupHeight = progress
                    tvPopupHeight.text = "Popup Height (${progress}dp)"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateEdgePreview() {
        viewEdgePreview.setBackgroundColor(settingsManager.edgeColorInt)
    }

    private fun updatePopupPreview() {
        viewPopupPreview.setBackgroundColor(settingsManager.popupColorInt)
    }
}
