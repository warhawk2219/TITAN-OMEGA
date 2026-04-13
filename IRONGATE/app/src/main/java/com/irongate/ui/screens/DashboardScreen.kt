package com.irongate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irongate.model.*
import com.irongate.protocol.ProtocolState
import com.irongate.ui.components.*
import com.irongate.ui.theme.IronColors
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    connectionStatus: ConnectionStatus,
    protocolState: ProtocolState,
    onCommenceProtocol: () -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Black)
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {

        SectionTitle("CONNECTION STATUS")

        // Connection cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ConnectionCard(
                name = "GHOST",
                state = connectionStatus.ghost,
                port = 8765,
                latencyMs = connectionStatus.ghostLatencyMs,
                modifier = Modifier.weight(1f)
            )
            ConnectionCard(
                name = "GIPSY",
                state = connectionStatus.gipsy,
                port = 8766,
                latencyMs = connectionStatus.gipsyLatencyMs,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // IRONGATE Protocol Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IronColors.NearBlack)
                .border(1.dp, IronColors.Dark3)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PixelLabel("IRONGATE PROTOCOL", fontSize = 7.sp, color = IronColors.White)
                    ProtocolBadge(protocolState.result)
                }

                Spacer(Modifier.height(14.dp))

                DataRow("STATUS", if (protocolState.running) "RUNNING" else "STANDBY")
                DataRow("INTERVAL", "60 MIN")
                DataRow("BUFFER", "${connectionStatus.msgsBuffered} / 100")
                DataRow("MSGS ROUTED", "${connectionStatus.msgsRouted}")

                if (protocolState.running) {
                    Spacer(Modifier.height(10.dp))
                    MonoText(protocolState.stepText, fontSize = 10.sp, color = IronColors.Gray2)
                    Spacer(Modifier.height(8.dp))
                    IronProgressBar(protocolState.progress)
                }

                Spacer(Modifier.height(14.dp))

                IronButton(
                    text = "COMMENCE IRONGATE PROTOCOL",
                    onClick = onCommenceProtocol,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = IronColors.Dark4,
                    enabled = !protocolState.running
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionTitle("BRIDGE HEALTH")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IronColors.NearBlack)
                .border(1.dp, IronColors.Dark2)
                .padding(14.dp)
        ) {
            Column {
                DataRow("UPTIME", formatUptime(connectionStatus.uptimeSeconds))
                DataRow("GHOST LATENCY",
                    if (connectionStatus.ghost == AssistantState.ONLINE) "${connectionStatus.ghostLatencyMs}ms" else "---")
                DataRow("GIPSY LATENCY",
                    if (connectionStatus.gipsy == AssistantState.ONLINE) "${connectionStatus.gipsyLatencyMs}ms" else "---")
                DataRow("HEARTBEAT", "10s · STABLE",
                    valueColor = IronColors.Online)
                DataRow("SOCKET STATUS",
                    when {
                        connectionStatus.ghost == AssistantState.ONLINE &&
                        connectionStatus.gipsy == AssistantState.ONLINE -> "BOTH OPEN"
                        connectionStatus.ghost == AssistantState.OFFLINE &&
                        connectionStatus.gipsy == AssistantState.OFFLINE -> "BOTH CLOSED"
                        else -> "PARTIAL"
                    },
                    valueColor = when {
                        connectionStatus.ghost == AssistantState.ONLINE &&
                        connectionStatus.gipsy == AssistantState.ONLINE -> IronColors.Online
                        else -> IronColors.Warn
                    }
                )
                DataRow("JSON ERRORS", "0")
                DataRow("PROTOCOL INTERVAL", "EVERY 60 MIN")
            }
        }

        Spacer(Modifier.height(80.dp)) // bottom nav clearance
    }
}

private fun formatUptime(seconds: Long): String {
    val h = TimeUnit.SECONDS.toHours(seconds)
    val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
