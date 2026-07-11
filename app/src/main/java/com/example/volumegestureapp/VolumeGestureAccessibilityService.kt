package com.example.volumegestureapp

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.content.SharedPreferences


class VolumeGestureAccessibilityService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = "VolumeGestureService"

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private var isAnimatingOut = false

    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var leftEdgeView: View? = null
    private var rightEdgeView: View? = null
    private var leftParams: WindowManager.LayoutParams? = null
    private var rightParams: WindowManager.LayoutParams? = null
    private var initialY = 0
    private var isDragging = false
    private var isSwipeTriggered = false
    
    private lateinit var settingsManager: SettingsManager


    // Runnable to auto-dismiss the overlay after inactivity
    private val dismissRunnable = Runnable { removeOverlay() }

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isEdgeLongPressTriggered = false
private var lastTouchY = 0f
private var activePill: View? = null
private var isVolumeScrollMode = false
private var volumeScrollStartY = 0f
private var volumeScrollStartVal = 0
private var lastClickedSideLeft = true
    private val edgeLongPressRunnable = Runnable {
        Log.i(TAG, "edgeLongPressRunnable: 600ms hold reached, entering volume scroll mode.")
        isEdgeLongPressTriggered = true
        isVolumeScrollMode = true
        // Record start Y for volume adjustment
        volumeScrollStartY = lastTouchY
        // Capture current volume baseline
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volumeScrollStartVal = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        // Update pill visual to indicate active mode
        activePill?.background = getPillDrawableForVolumeScroll(true)
        // Show the beautiful glassmorphic overlay near the pill
        showOverlay()
    }

    private fun getPillDrawable(pressed: Boolean): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 100f
            val baseColor = settingsManager.edgeColorInt
            if (pressed) {
                val alpha = (android.graphics.Color.alpha(baseColor) * 2).coerceAtMost(255)
                val color = android.graphics.Color.argb(
                    alpha,
                    android.graphics.Color.red(baseColor),
                    android.graphics.Color.green(baseColor),
                    android.graphics.Color.blue(baseColor)
                )
                setColor(color)
                setStroke(2, android.graphics.Color.argb(alpha, 0, 0, 0))
            } else {
                setColor(baseColor)
                setStroke(2, android.graphics.Color.argb((android.graphics.Color.alpha(baseColor) * 1.3).toInt().coerceAtMost(255), 0, 0, 0))
            }
        }
    }

    private fun getPillDrawableForVolumeScroll(active: Boolean): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 100f
            if (active) {
                setColor(android.graphics.Color.parseColor("#FF1A73E8")) // Vibrant blue accent
                setStroke(3, android.graphics.Color.parseColor("#FF1A1A1F")) // Dark outer border
            } else {
                val baseColor = settingsManager.edgeColorInt
                setColor(baseColor)
                setStroke(2, android.graphics.Color.argb((android.graphics.Color.alpha(baseColor) * 1.3).toInt().coerceAtMost(255), 0, 0, 0))
            }
        }
    }


    private val edgeTouchListener = View.OnTouchListener { view, event ->
        val isLeft = (view == leftEdgeView)
        val params = if (isLeft) leftParams else rightParams
        val pill = (view as? FrameLayout)?.getChildAt(0)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Determine which edge pill was touched
                lastClickedSideLeft = isLeft
                touchStartX = event.rawX
                touchStartY = event.rawY
                // Store raw Y for long‑press detection
                lastTouchY = event.rawY
                initialY = params?.y ?: 0
                isDragging = false
                isEdgeLongPressTriggered = false
                isSwipeTriggered = false
                activePill = pill
                
                pill?.background = getPillDrawable(true)
                
                if (settingsManager.triggerMode == "long_press") {
                    mainHandler.removeCallbacks(edgeLongPressRunnable)
                    mainHandler.postDelayed(edgeLongPressRunnable, 600)
                }
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.rawY - touchStartY
                if (settingsManager.triggerMode == "edge_swipe") {
                    if (isSwipeTriggered) {
                        return@OnTouchListener true
                    }
                    if (!isDragging && Math.abs(deltaY) > 30) {
                        isDragging = true
                    }
                    if (isDragging && params != null && windowManager != null && !settingsManager.sidebarLocked) {
                        // Normal edge‑pill drag to reposition
                        params.y = (initialY + deltaY).toInt()
                        val screenHeight = resources.displayMetrics.heightPixels
                        val maxOffset = screenHeight / 2 - 100
                        if (params.y > maxOffset) params.y = maxOffset
                        if (params.y < -maxOffset) params.y = -maxOffset
                        windowManager?.updateViewLayout(view, params)
                    } else if (!isDragging) {
                        // Check for horizontal inward swipe
                        val density = resources.displayMetrics.density
                        val swipeThreshold = 40 * density
                        val deltaXInward = if (isLeft) (event.rawX - touchStartX) else (touchStartX - event.rawX)
                        val deltaYAbs = Math.abs(deltaY)
                        if (deltaXInward > swipeThreshold && deltaXInward > deltaYAbs) {
                            isSwipeTriggered = true
                            showOverlay()
                        }
                    }
                    true
                } else {
                    if (!isDragging && Math.abs(deltaY) > 30) {
                        isDragging = true
                        mainHandler.removeCallbacks(edgeLongPressRunnable)
                    }
                    // If we are in volume scroll mode, adjust volume based on vertical movement
                    if (isVolumeScrollMode) {
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val density = resources.displayMetrics.density
                        val pixelsPerStep = 20 * density // 20dp per volume step
                        val offsetY = event.rawY - volumeScrollStartY
                        val steps = (-offsetY / pixelsPerStep).toInt()
                        var newVol = volumeScrollStartVal + steps
                        if (newVol > maxVolume) newVol = maxVolume
                        if (newVol < 0) newVol = 0
                        if (newVol != audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                Log.i(TAG, "Volume scroll: adjusted to $newVol (steps=$steps)")
                                
                                // Dynamically update overlay UI if showing
                                if (isOverlayShowing && overlayView != null) {
                                    val seekBar = overlayView?.findViewById<SeekBar>(R.id.volume_seekbar)
                                    val tvPercentage = overlayView?.findViewById<TextView>(R.id.tv_volume_percentage)
                                    seekBar?.progress = newVol
                                    tvPercentage?.text = "${(newVol * 100) / if (maxVolume > 0) maxVolume else 1}%"
                                    resetDismissTimer()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Volume scroll adjust failed", e)
                            }
                        }
                        true
                    } else if (isDragging && params != null && windowManager != null && !settingsManager.sidebarLocked) {
                        // Normal edge‑pill drag to reposition
                        params.y = (initialY + deltaY).toInt()
                        val screenHeight = resources.displayMetrics.heightPixels
                        val maxOffset = screenHeight / 2 - 100
                        if (params.y > maxOffset) params.y = maxOffset
                        if (params.y < -maxOffset) params.y = -maxOffset
                        windowManager?.updateViewLayout(view, params)
                        true
                    } else {
                        true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(edgeLongPressRunnable)
                isVolumeScrollMode = false
                isSwipeTriggered = false
                pill?.background = getPillDrawable(false)
                activePill?.background = getPillDrawable(false)
                activePill = null
                // Start auto-dismiss countdown the moment the user releases the pill
                if (isOverlayShowing) {
                    resetDismissTimer()
                }
                true
            }
            else -> false
        }
    }

    private fun dispatchBackGesture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.i(TAG, "dispatchBackGesture: Dispatching programmatic back swipe using GestureDescription")
            val path = android.graphics.Path()
            val displayMetrics = resources.displayMetrics
            val startX = displayMetrics.widthPixels.toFloat() - 5f
            val endX = startX - 150f
            val y = displayMetrics.heightPixels.toFloat() / 2f
            path.moveTo(startX, y)
            path.lineTo(endX, y)

            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 100)
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(stroke)
                .build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.i(TAG, "dispatchBackGesture: Gesture successfully completed")
                }
                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.w(TAG, "dispatchBackGesture: Gesture cancelled, falling back to performGlobalAction")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }, null)
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private var volumeReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        Log.i(TAG, "onCreate: Initializing Accessibility Service")
        super.onCreate()
        settingsManager = SettingsManager(this)
        settingsManager.registerListener(this)
    }

    override fun onServiceConnected() {
        Log.i(TAG, "onServiceConnected: Service successfully connected to system Accessibility manager")
        super.onServiceConnected()
        try {
            startForegroundServiceWithNotification()
            Log.i(TAG, "onServiceConnected: startForegroundServiceWithNotification successful")
        } catch (e: Exception) {
            Log.e(TAG, "onServiceConnected: Failed to start foreground service", e)
        }
        setupEdgeOverlays()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // No-op. Back swipe gestures are handled natively by the OS without service interference.
    }

    override fun onInterrupt() {
        Log.i(TAG, "onInterrupt: Service interrupted")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: Shutting down Accessibility Service")
        if (::settingsManager.isInitialized) {
            settingsManager.unregisterListener(this)
        }
        removeOverlay()
        removeEdgeOverlays()
        super.onDestroy()
    }


    override fun onKeyEvent(event: KeyEvent?): Boolean {
        // No-op. Back button and volume buttons work normally as standard system flows.
        return super.onKeyEvent(event)
    }

    private fun resetDismissTimer() {
        mainHandler.removeCallbacks(dismissRunnable)
        mainHandler.postDelayed(dismissRunnable, 1000)
    }

    private fun showOverlay() {
        Log.i(TAG, "showOverlay: Attempting to display floating overlay view. isOverlayShowing=$isOverlayShowing")
        if (isOverlayShowing) {
            // Already visible — just reset the dismiss timer
            resetDismissTimer()
            return
        }

        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            // Proactively remove any existing overlay view immediately to prevent window stacking issues
            overlayView?.let { existingView ->
                try {
                    if (existingView.parent != null) {
                        wm.removeViewImmediate(existingView)
                        Log.i(TAG, "showOverlay: Cleaned up existing dangling overlayView synchronously.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "showOverlay: Failed to synchronously remove existing overlayView", e)
                }
                overlayView = null
            }

            val inflater = LayoutInflater.from(this)
            val overlayLayout = inflater.inflate(R.layout.volume_overlay, null)

            // Float above any active application using TYPE_ACCESSIBILITY_OVERLAY
            val overlayGravity = if (lastClickedSideLeft) Gravity.LEFT or Gravity.CENTER_VERTICAL else Gravity.RIGHT or Gravity.CENTER_VERTICAL
            var wmFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                wmFlags = wmFlags or 0x00000080 // FLAG_BLUR_BEHIND
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                wmFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = overlayGravity
                // Align vertically with the edge pill's current Y position
                y = initialY
                // Small horizontal offset to keep a comfortable distance from the edge pill
                x = (12 * resources.displayMetrics.density).toInt()
                windowAnimations = 0 // Disable default window animations to use custom animations
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blurBehindRadius = (24 * resources.displayMetrics.density).toInt() // Ultra premium blur depth
                }
            }

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val tvPercentage = overlayLayout.findViewById<TextView>(R.id.tv_volume_percentage)
            val seekBar = overlayLayout.findViewById<SeekBar>(R.id.volume_seekbar)
            val cardContainer = (overlayLayout as FrameLayout).getChildAt(0)

            val density = resources.displayMetrics.density
            val userWidth = settingsManager.popupWidth
            val userHeight = settingsManager.popupHeight

            cardContainer.layoutParams = FrameLayout.LayoutParams(
                (userWidth * density).toInt(),
                (userHeight * density).toInt(),
                Gravity.CENTER
            )

            // Dynamic Background Gradient
            val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                
                val startColor = settingsManager.popupColorInt
                val endColor = android.graphics.Color.argb(
                    (android.graphics.Color.alpha(startColor) * 0.9).toInt(),
                    (android.graphics.Color.red(startColor) * 0.95).toInt(),
                    (android.graphics.Color.green(startColor) * 0.95).toInt(),
                    (android.graphics.Color.blue(startColor) * 0.95).toInt()
                )
                colors = intArrayOf(startColor, endColor)
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                
                val isDarkTheme = settingsManager.theme == "dark" || 
                    (settingsManager.theme == "system" && (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
                
                if (isDarkTheme) {
                    setStroke(1, android.graphics.Color.parseColor("#44FFFFFF"))
                } else {
                    setStroke(1, android.graphics.Color.parseColor("#33000000"))
                }
            }
            cardContainer.background = bgDrawable

            val isDarkTheme = settingsManager.theme == "dark" || 
                (settingsManager.theme == "system" && (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)

            val primaryColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1A1A1F")
            val btnBgTint = if (isDarkTheme) android.graphics.Color.parseColor("#33FFFFFF") else android.graphics.Color.parseColor("#12000000")

            tvPercentage.setTextColor(primaryColor)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                seekBar.progressTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                seekBar.thumbTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            }
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            seekBar.max = maxVolume
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            seekBar.progress = currentVolume
            tvPercentage.text = "${(currentVolume * 100) / if (maxVolume > 0) maxVolume else 1}%"





            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                        tvPercentage.text = "${(progress * 100) / if (maxVolume > 0) maxVolume else 1}%"
                        resetDismissTimer()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    mainHandler.removeCallbacks(dismissRunnable)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    resetDismissTimer()
                }
            })

            // Scroll vertical gesture (incremental scroll) to adjust volume on card container
            var lastY = 0f
            var accumulatedDeltaY = 0f
            val pixelsPerStep = 20 * density // 20dp of vertical movement = 1 volume step
            
            val scrollTouchListener = View.OnTouchListener { _: View, event: MotionEvent ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastY = event.rawY
                        accumulatedDeltaY = 0f
                        mainHandler.removeCallbacks(dismissRunnable)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = lastY - event.rawY // swiping up is positive deltaY
                        lastY = event.rawY
                        accumulatedDeltaY += deltaY
                        
                        val steps = (accumulatedDeltaY / pixelsPerStep).toInt()
                        if (steps != 0) {
                            accumulatedDeltaY -= steps * pixelsPerStep
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            var newVolume = currentVol + steps
                            if (newVolume > maxVolume) newVolume = maxVolume
                            if (newVolume < 0) newVolume = 0
                            
                            if (newVolume != currentVol) {
                                try {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                    Log.i(TAG, "Card scroll (incremental): Adjusted volume to $newVolume (steps=$steps)")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Card scroll adjust volume failed", e)
                                }
                                val updatedVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                seekBar.progress = updatedVol
                                tvPercentage.text = "${(updatedVol * 100) / if (maxVolume > 0) maxVolume else 1}%"
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        resetDismissTimer()
                        true
                    }
                    else -> false
                }
            }

            // Set the scroll gesture touch listener on both the card container background and the root overlayLayout FrameLayout
            cardContainer.setOnTouchListener(scrollTouchListener)
            overlayLayout.setOnTouchListener(scrollTouchListener)

            // Register broadcast receiver to dynamically sync physical key presses
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                        val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                        if (streamType == AudioManager.STREAM_MUSIC) {
                            val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            seekBar.progress = vol
                            tvPercentage.text = "${(vol * 100) / if (maxVolume > 0) maxVolume else 1}%"
                        }
                    }
                }
            }
            registerReceiver(receiver, filter)
            volumeReceiver = receiver

            Log.i(TAG, "showOverlay: Adding view to WindowManager")
            wm.addView(overlayLayout, params)
            overlayView = overlayLayout
            isOverlayShowing = true
            
            // Run premium custom enter slide-in animation from edge
            try {
                val enterAnimRes = if (lastClickedSideLeft) R.anim.slide_in_left else R.anim.slide_in_right
                val enterAnim = android.view.animation.AnimationUtils.loadAnimation(this, enterAnimRes)
                overlayLayout.startAnimation(enterAnim)
            } catch (animEx: Exception) {
                Log.e(TAG, "Failed to run enter animation", animEx)
            }
            
            resetDismissTimer()
            Log.i(TAG, "showOverlay: Overlay successfully displayed and timer scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "showOverlay: ERROR while creating or adding floating window", e)
        }
    }

    private fun removeOverlay() {
        Log.i(TAG, "removeOverlay called. isOverlayShowing=$isOverlayShowing")
        if (!isOverlayShowing) return
        // Mark as hidden immediately so no repeat calls sneak in
        isOverlayShowing = false

        // Unregister broadcast receiver
        try {
            volumeReceiver?.let {
                unregisterReceiver(it)
                volumeReceiver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "removeOverlay: Error unregistering receiver", e)
        }

        // Cancel the dismiss timer
        mainHandler.removeCallbacks(dismissRunnable)

        val wm = windowManager
        val view = overlayView
        overlayView = null

        if (wm == null || view == null) {
            Log.w(TAG, "removeOverlay: nothing to remove")
            return
        }

        // Play a quick alpha fade-out (150ms) using modern, reliable ViewPropertyAnimator.
        // This is 100% reliable for WindowManager views unlike the old AnimationListener.
        try {
            isAnimatingOut = true
            view.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    try {
                        if (view.parent != null) {
                            wm.removeView(view)
                            Log.i(TAG, "removeOverlay: view successfully removed from window manager after fade")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "removeOverlay: wm.removeView failed inside endAction", e)
                    } finally {
                        isAnimatingOut = false
                    }
                }
                .start()
        } catch (e: Exception) {
            // Fallback: If ViewPropertyAnimator fails or throws, remove immediately
            Log.e(TAG, "removeOverlay: ViewPropertyAnimator failed, removing view instantly", e)
            try {
                if (view.parent != null) {
                    wm.removeViewImmediate(view)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "removeOverlay: fallback wm.removeViewImmediate failed", ex)
            } finally {
                isAnimatingOut = false
            }
        }
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "volume_gesture_service_channel"
        val notificationId = 1
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Volume Gesture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Volume Gesture Service running in the background"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Volume Gesture Service Active")
                .setContentText("Long press back button/gesture to control volume")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Volume Gesture Service Active")
                .setContentText("Long press back button/gesture to control volume")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(notificationId, notification, 1073741824) // 1073741824 is ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun setupEdgeOverlays() {
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm
            val density = resources.displayMetrics.density
            
            val userPillWidth = settingsManager.edgeWidth
            val userPillHeight = settingsManager.edgeHeight
            
            val containerWidth = ((userPillWidth + 26) * density).toInt()
            val containerHeight = ((userPillHeight + 40) * density).toInt()
            val pillWidth = (userPillWidth * density).toInt()
            val pillHeight = (userPillHeight * density).toInt()

            // Left edge overlay container
            val leftContainer = FrameLayout(this).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setOnTouchListener(edgeTouchListener)
            }
            val leftPill = View(this).apply {
                id = 1001
                background = getPillDrawable(false)
            }
            val leftPillParams = FrameLayout.LayoutParams(
                pillWidth,
                pillHeight,
                Gravity.CENTER_VERTICAL or Gravity.LEFT
            )
            leftContainer.addView(leftPill, leftPillParams)

            val paramsLeft = WindowManager.LayoutParams(
                containerWidth,
                containerHeight,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                y = 0
            }
            wm.addView(leftContainer, paramsLeft)
            leftEdgeView = leftContainer
            leftParams = paramsLeft

            // Right edge overlay container
            val rightContainer = FrameLayout(this).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setOnTouchListener(edgeTouchListener)
            }
            val rightPill = View(this).apply {
                id = 1002
                background = getPillDrawable(false)
            }
            val rightPillParams = FrameLayout.LayoutParams(
                pillWidth,
                pillHeight,
                Gravity.CENTER_VERTICAL or Gravity.RIGHT
            )
            rightContainer.addView(rightPill, rightPillParams)

            val paramsRight = WindowManager.LayoutParams(
                containerWidth,
                containerHeight,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                y = 0
            }
            wm.addView(rightContainer, paramsRight)
            rightEdgeView = rightContainer
            rightParams = paramsRight

            Log.i(TAG, "setupEdgeOverlays: Left and Right edge gesture handles successfully added on screen edges")
        } catch (e: Exception) {
            Log.e(TAG, "setupEdgeOverlays: Failed to create edge overlays", e)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            SettingsManager.KEY_EDGE_COLOR_A,
            SettingsManager.KEY_EDGE_COLOR_R,
            SettingsManager.KEY_EDGE_COLOR_G,
            SettingsManager.KEY_EDGE_COLOR_B -> {
                leftEdgeView?.findViewById<View>(1001)?.background = getPillDrawable(false)
                rightEdgeView?.findViewById<View>(1002)?.background = getPillDrawable(false)
            }
            SettingsManager.KEY_EDGE_WIDTH,
            SettingsManager.KEY_EDGE_HEIGHT -> {
                updateEdgeSizes()
            }
        }
    }

    private fun updateEdgeSizes() {
        val wm = windowManager ?: return
        val density = resources.displayMetrics.density
        
        val userPillWidth = settingsManager.edgeWidth
        val userPillHeight = settingsManager.edgeHeight
        
        val containerWidth = ((userPillWidth + 26) * density).toInt()
        val containerHeight = ((userPillHeight + 40) * density).toInt()
        val pillWidth = (userPillWidth * density).toInt()
        val pillHeight = (userPillHeight * density).toInt()

        leftEdgeView?.let { container ->
            leftParams?.apply {
                width = containerWidth
                height = containerHeight
            }
            container.findViewById<View>(1001)?.layoutParams = FrameLayout.LayoutParams(
                pillWidth,
                pillHeight,
                Gravity.CENTER_VERTICAL or Gravity.LEFT
            )
            try {
                wm.updateViewLayout(container, leftParams)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating left edge view layout", e)
            }
        }

        rightEdgeView?.let { container ->
            rightParams?.apply {
                width = containerWidth
                height = containerHeight
            }
            container.findViewById<View>(1002)?.layoutParams = FrameLayout.LayoutParams(
                pillWidth,
                pillHeight,
                Gravity.CENTER_VERTICAL or Gravity.RIGHT
            )
            try {
                wm.updateViewLayout(container, rightParams)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating right edge view layout", e)
            }
        }
    }


    private fun removeEdgeOverlays() {
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            leftEdgeView?.let {
                wm.removeView(it)
                leftEdgeView = null
            }
            rightEdgeView?.let {
                wm.removeView(it)
                rightEdgeView = null
            }
            Log.i(TAG, "removeEdgeOverlays: Left and Right edge overlays removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "removeEdgeOverlays: Error removing edge overlays", e)
        }
    }
}
