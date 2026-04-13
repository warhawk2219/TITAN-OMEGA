package com.irongate.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.view.View
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import com.irongate.model.AssistantState

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
    }

    private fun createOverlay() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(200, 0, 0, 0))
            setPadding(24, 12, 24, 12)
        }

        val ghostDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(16, 16).also {
                it.marginEnd = 8
                it.gravity = Gravity.CENTER_VERTICAL
            }
            setBackgroundColor(Color.parseColor("#44ff88"))
        }

        val ghostLabel = TextView(this).apply {
            text = "GHOST"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 16, 0)
        }

        val gipsyDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(16, 16).also {
                it.marginEnd = 8
                it.gravity = Gravity.CENTER_VERTICAL
            }
            setBackgroundColor(Color.parseColor("#44ff88"))
        }

        val gipsyLabel = TextView(this).apply {
            text = "GIPSY"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.MONOSPACE
        }

        layout.addView(ghostDot)
        layout.addView(ghostLabel)
        layout.addView(gipsyDot)
        layout.addView(gipsyLabel)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 120
        }

        floatingView = layout
        windowManager?.addView(layout, params)
    }

    fun updateStatus(ghost: AssistantState, gipsy: AssistantState) {
        // Update dot colors based on state
        // Called from service binding
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager?.removeView(it) }
    }
}
