package com.irongate.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.irongate.bridge.BridgeRouter
import com.irongate.model.AssistantState
import com.irongate.protocol.IrongateProtocol
import com.irongate.utils.Notifs
import kotlinx.coroutines.*

class BridgeService : Service() {

    companion object {
        private const val TAG = "BridgeService"
        const val ACTION_START = "com.irongate.START_BRIDGE"
        const val ACTION_STOP = "com.irongate.STOP_BRIDGE"
    }

    private val binder = BridgeBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val router = BridgeRouter()
    val protocol by lazy { IrongateProtocol(this, router) }

    inner class BridgeBinder : Binder() {
        fun getService(): BridgeService = this@BridgeService
    }

    override fun onCreate() {
        super.onCreate()
        Notifs.createChannels(this)
        Log.d(TAG, "BridgeService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val notification = Notifs.buildBridgeNotification(
            this,
            AssistantState.OFFLINE,
            AssistantState.OFFLINE
        )
        startForeground(Notifs.NOTIF_ID_BRIDGE, notification)

        startBridge()

        return START_STICKY
    }

    private fun startBridge() {
        router.start()
        protocol.startScheduler()

        // Update foreground notification as connection states change
        scope.launch {
            router.status.collect { status ->
                val notif = Notifs.buildBridgeNotification(this@BridgeService, status.ghost, status.gipsy)
                val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(Notifs.NOTIF_ID_BRIDGE, notif)
            }
        }

        Log.d(TAG, "Bridge started — GHOST:8765 GIPSY:8766")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        router.stop()
        protocol.stopScheduler()
        scope.cancel()
        Log.d(TAG, "BridgeService destroyed")
    }
}
