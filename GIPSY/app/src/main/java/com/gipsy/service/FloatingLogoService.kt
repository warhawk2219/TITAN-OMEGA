package com.gipsy.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.gipsy.R
import com.gipsy.data.models.GipsyListenState
import com.gipsy.ui.theme.GipsyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class FloatingLogoService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: ComposeView? = null

    val listenState = MutableStateFlow(GipsyListenState.IDLE)

    companion object {
        const val ACTION_SHOW = "com.gipsy.SHOW_LOGO"
        const val ACTION_HIDE = "com.gipsy.HIDE_LOGO"
        const val ACTION_STATE = "com.gipsy.SET_STATE"
        const val EXTRA_STATE  = "state"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> listenState.value = GipsyListenState.WAKE_WORD_DETECTED
            ACTION_HIDE -> listenState.value = GipsyListenState.IDLE
            ACTION_STATE -> {
                val stateName = intent.getStringExtra(EXTRA_STATE) ?: return START_STICKY
                listenState.value = GipsyListenState.valueOf(stateName)
            }
        }
        return START_STICKY
    }

    private fun createFloatingView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16
            y = 120
        }

        val lifecycleOwner = FloatingLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            })
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                GipsyTheme {
                    FloatingLogoContent(listenState = listenState)
                }
            }
        }

        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
    }
}

@Composable
fun FloatingLogoContent(listenState: MutableStateFlow<GipsyListenState>) {
    val state by listenState.collectAsState()

    val visible = state != GipsyListenState.IDLE

    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoAlpha"
    )

    val dimAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dimAlpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.5f),
        exit = fadeOut(tween(500))
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_gipsy_logo),
            contentDescription = "GIPSY listening",
            modifier = Modifier
                .size(40.dp)
                .alpha(
                    when (state) {
                        GipsyListenState.LISTENING            -> pulseAlpha
                        GipsyListenState.PROCESSING           -> pulseAlpha
                        GipsyListenState.WAITING_FOR_GHOST    -> dimAlpha
                        GipsyListenState.WAKE_WORD_DETECTED   -> 1f
                        else                                   -> 1f
                    }
                )
        )
    }
}

// ── LIFECYCLE OWNER FOR COMPOSE VIEW ─────────────────────────
class FloatingLifecycleOwner : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    fun performRestore(savedState: android.os.Bundle?) = savedStateRegistryController.performRestore(savedState)
}

private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
