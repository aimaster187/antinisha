package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.domain.AssistantRuntimeCoordinator
import kotlin.math.abs

class FloatingOrbOverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun isShowing(): Boolean = overlayView != null

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay(sizeDp: Int = 58): Boolean {
        if (!canDrawOverlays()) return false
        if (overlayView != null) return true

        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt().coerceIn(96, 260)

        val container = FrameLayout(context).apply {
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF38BDF8.toInt(), 0xFF7C3AED.toInt(), 0xFF6D28D9.toInt())
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke((2 * density).toInt(), 0xFFFFFFFF.toInt())
            }
            background = bg
            elevation = 14f * density
            val label = TextView(context).apply {
                text = "N"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 18f
                gravity = Gravity.CENTER
            }
            addView(label, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (96 * density).toInt()
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var downTime = 0L

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    params.x = initialX + dx
                    params.y = (initialY - dy).coerceAtLeast(24)
                    runCatching { windowManager.updateViewLayout(container, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = abs(event.rawX - touchX) > 14f || abs(event.rawY - touchY) > 14f
                    val heldMs = System.currentTimeMillis() - downTime
                    if (!moved) {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            if (heldMs > 550) {
                                putExtra("extra_open_orb_settings", true)
                            } else {
                                AssistantRuntimeCoordinator.triggerVoiceActivation()
                                putExtra(NishaAssistantService.EXTRA_OPEN_VOICE, true)
                            }
                        }
                        context.startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }

        return try {
            windowManager.addView(container, params)
            overlayView = container
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hideOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
            overlayView = null
        }
    }
}
