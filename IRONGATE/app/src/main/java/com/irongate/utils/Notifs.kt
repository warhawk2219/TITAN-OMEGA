package com.irongate.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.irongate.MainActivity
import com.irongate.R
import com.irongate.model.AssistantState
import com.irongate.model.ProtocolResult

object Notifs {

    const val CHANNEL_BRIDGE = "irongate_bridge"
    const val CHANNEL_PROTOCOL = "irongate_protocol"
    const val NOTIF_ID_BRIDGE = 1001
    const val NOTIF_ID_PROTOCOL = 1002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val bridgeCh = NotificationChannel(
            CHANNEL_BRIDGE,
            "Bridge Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent bridge connection status"
            setShowBadge(false)
        }

        val protocolCh = NotificationChannel(
            CHANNEL_PROTOCOL,
            "IRONGATE Protocol",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Hourly protocol results"
            setShowBadge(false)
            setSound(null, null)
        }

        nm.createNotificationChannel(bridgeCh)
        nm.createNotificationChannel(protocolCh)
    }

    fun buildBridgeNotification(
        context: Context,
        ghostState: AssistantState,
        gipsyState: AssistantState
    ): Notification {
        val ghostText = if (ghostState == AssistantState.ONLINE) "GHOST connected" else "GHOST offline"
        val gipsyText = if (gipsyState == AssistantState.ONLINE) "GIPSY connected" else "GIPSY offline"

        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_BRIDGE)
            .setContentTitle("IRONGATE: Active")
            .setContentText("$ghostText · $gipsyText")
            .setSmallIcon(R.drawable.ic_irongate_notif)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pi)
            .build()
    }

    fun sendProtocolResult(context: Context, result: ProtocolResult) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val resultText = when (result) {
            ProtocolResult.NOMINAL -> "All clear."
            ProtocolResult.CAUTION -> "Something's down."
            ProtocolResult.BLACKOUT -> "Blackout."
        }

        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_PROTOCOL)
            .setContentTitle("IRONGATE Protocol Complete")
            .setContentText(resultText)
            .setSmallIcon(R.drawable.ic_irongate_notif)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(NOTIF_ID_PROTOCOL, notif)
    }
}
