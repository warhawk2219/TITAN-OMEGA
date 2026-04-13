package com.ghost.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ghost.R

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Skull logo overlay
    private var logoView: View? = null
    private var logoAnimator: ValueAnimator? = null

    // HUD overlay
    private var hudView: View? = null

    // Focus timer overlay
    private var focusView: View? = null

    // Silent mode text overlay
    private var silentView: View? = null

    enum class LogoState {
        HIDDEN, LISTENING, PROCESSING, RESPONDING
    }

    private val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else
        WindowManager.LayoutParams.TYPE_PHONE

    // ===== SKULL LOGO OVERLAY =====

    fun showLogoOverlay(state: LogoState) {
        if (logoView == null) {
            createLogoOverlay()
        }

        when (state) {
            LogoState.HIDDEN -> hideLogoOverlay()
            LogoState.LISTENING -> animateLogoPulse(slow = true)
            LogoState.PROCESSING -> animateLogoPulse(slow = false)
            LogoState.RESPONDING -> stopLogoAnimation()
        }
    }

    private fun createLogoOverlay() {
        val params = WindowManager.LayoutParams(
            80.dp,
            80.dp,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 12
            y = 80
        }

        val container = FrameLayout(context)
        container.setBackgroundColor(0xCC000000.toInt())

        // Corner brackets
        val topLeft = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(12, 12).also {
                it.gravity = Gravity.TOP or Gravity.START
            }
            setBackgroundColor(0xFFE8E8E8.toInt())
        }

        // Ghost skull logo
        val logo = ImageView(context).apply {
            setImageResource(R.drawable.ghost_logo)
            layoutParams = FrameLayout.LayoutParams(56, 56).also {
                it.gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        container.addView(logo)
        logoView = container

        try {
            windowManager.addView(container, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun animateLogoPulse(slow: Boolean) {
        logoAnimator?.cancel()
        val duration = if (slow) 1200L else 500L
        logoAnimator = ValueAnimator.ofFloat(1f, 0.4f, 1f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                logoView?.alpha = animator.animatedValue as Float
            }
            start()
        }
    }

    private fun stopLogoAnimation() {
        logoAnimator?.cancel()
        logoView?.alpha = 1f
    }

    fun hideLogoOverlay(delay: Long = 0L) {
        logoView?.let { view ->
            view.animate()
                .alpha(0f)
                .setStartDelay(delay)
                .setDuration(400)
                .withEndAction {
                    try {
                        windowManager.removeView(view)
                    } catch (e: Exception) {}
                    logoView = null
                    logoAnimator?.cancel()
                }
                .start()
        }
    }

    // ===== HUD OVERLAY =====

    fun showHUD(status: String, activeProtocol: String?) {
        if (hudView == null) createHUDOverlay()
        updateHUD(status, activeProtocol)
    }

    private fun createHUDOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 120
        }

        val hud = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE000000.toInt())
            setPadding(16, 10, 16, 10)
        }

        // HUD content built programmatically
        val idText = TextView(context).apply {
            text = "GHOST // SYS"
            setTextColor(0xFF444444.toInt())
            textSize = 7f
        }

        val nameText = TextView(context).apply {
            text = "GHOST"
            setTextColor(0xFFE8E8E8.toInt())
            textSize = 10f
            tag = "name"
        }

        val statusText = TextView(context).apply {
            text = "STANDBY — LISTENING"
            setTextColor(0xFF444444.toInt())
            textSize = 7f
            tag = "status"
        }

        val divider = View(context).apply {
            setBackgroundColor(0xFF1A1A1A.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.setMargins(0, 6, 0, 6) }
        }

        val protoLabel = TextView(context).apply {
            text = "ACTIVE PROTOCOL"
            setTextColor(0xFF444444.toInt())
            textSize = 7f
        }

        val protoText = TextView(context).apply {
            text = "-- NONE --"
            setTextColor(0xFF222222.toInt())
            textSize = 9f
            tag = "protocol"
        }

        val ticker = TextView(context).apply {
            text = "...... SYS OK ...... MIC LIVE ...... ID READY ......"
            setTextColor(0xFF222222.toInt())
            textSize = 6f
            tag = "ticker"
        }

        hud.addView(idText)
        hud.addView(nameText)
        hud.addView(statusText)
        hud.addView(divider)
        hud.addView(protoLabel)
        hud.addView(protoText)
        hud.addView(ticker)

        hudView = hud

        try {
            windowManager.addView(hud, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateHUD(status: String, activeProtocol: String?) {
        hudView?.let { hud ->
            hud.findViewWithTag<TextView>("status")?.text = status
            val protoView = hud.findViewWithTag<TextView>("protocol")
            if (activeProtocol != null) {
                protoView?.text = activeProtocol.uppercase()
                protoView?.setTextColor(0xFFE8E8E8.toInt())
            } else {
                protoView?.text = "-- NONE --"
                protoView?.setTextColor(0xFF222222.toInt())
            }
        }
    }

    fun hideHUD() {
        hudView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            hudView = null
        }
    }

    // ===== FOCUS TIMER OVERLAY =====

    fun showFocusTimer(durationSeconds: Int, onTick: (Int) -> Unit, onComplete: () -> Unit) {
        if (focusView == null) createFocusOverlay()

        val animator = ValueAnimator.ofInt(durationSeconds, 0).apply {
            duration = (durationSeconds * 1000L)
            interpolator = android.animation.LinearInterpolator()
            addUpdateListener { anim ->
                val remaining = anim.animatedValue as Int
                updateFocusTimer(remaining)
                onTick(remaining)
            }
            android.animation.Animator.AnimatorListener
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(p0: android.animation.Animator) {}
                override fun onAnimationCancel(p0: android.animation.Animator) {}
                override fun onAnimationRepeat(p0: android.animation.Animator) {}
                override fun onAnimationEnd(p0: android.animation.Animator) {
                    onComplete()
                    hideFocusTimer()
                }
            })
            start()
        }
    }

    private fun createFocusOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 12
            y = 100
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE000000.toInt())
            setPadding(12, 8, 12, 8)
        }

        val label = TextView(context).apply {
            text = "FOCUS"
            setTextColor(0xFF555555.toInt())
            textSize = 6f
        }

        val timer = TextView(context).apply {
            text = "45:00"
            setTextColor(0xFFE8E8E8.toInt())
            textSize = 12f
            tag = "focus_timer"
        }

        container.addView(label)
        container.addView(timer)
        focusView = container

        try {
            windowManager.addView(container, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateFocusTimer(seconds: Int) {
        val mins = seconds / 60
        val secs = seconds % 60
        focusView?.findViewWithTag<TextView>("focus_timer")?.text =
            String.format("%02d:%02d", mins, secs)
    }

    fun hideFocusTimer() {
        focusView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            focusView = null
        }
    }

    // ===== SILENT MODE TEXT OVERLAY =====

    fun showSilentMessage(text: String) {
        if (silentView == null) createSilentOverlay()
        silentView?.findViewWithTag<TextView>("silent_text")?.text = text
    }

    private fun createSilentOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 200
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE000000.toInt())
            setPadding(16, 10, 16, 10)
        }

        val label = TextView(context).apply {
            text = "GHOST"
            setTextColor(0xFF444444.toInt())
            textSize = 6f
        }

        val text = TextView(context).apply {
            setTextColor(0xFFE8E8E8.toInt())
            textSize = 9f
            tag = "silent_text"
        }

        container.addView(label)
        container.addView(text)
        silentView = container

        try {
            windowManager.addView(container, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideSilentOverlay() {
        silentView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            silentView = null
        }
    }

    fun destroyAll() {
        hideLogoOverlay()
        hideHUD()
        hideFocusTimer()
        hideSilentOverlay()
    }

    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density).toInt()
}
