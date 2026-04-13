package com.ghost.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.wear.compose.material.Text
import com.ghost.ui.screens.*
import com.ghost.ui.theme.*

sealed class Screen(val route: String, val label: String) {
    object Debrief : Screen("debrief", "DEBRIEF")
    object Comms : Screen("comms", "COMMS")
    object Protocols : Screen("protocols", "PROTOCOLS")
    object Records : Screen("records", "RECORDS")
    object Recon : Screen("recon", "RECON")
}

@Composable
fun GhostNavGraph(navController: NavHostController) {
    val screens = listOf(
        Screen.Debrief,
        Screen.Comms,
        Screen.Protocols,
        Screen.Records,
        Screen.Recon
    )

    Column(modifier = Modifier.fillMaxSize().background(GhostBlack)) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Debrief.route
            ) {
                composable(Screen.Debrief.route) {
                    ChatScreen(
                        messages = listOf(),
                        isListening = false,
                        isProcessing = false,
                        isTtsEnabled = false,
                        activeProtocol = null,
                        ghostStatus = "STANDBY",
                        onSendMessage = {},
                        onToggleTts = {},
                        onMicClick = {}
                    )
                }
                composable(Screen.Comms.route) { CommsScreen() }
                composable(Screen.Protocols.route) { ProtocolsScreen() }
                composable(Screen.Records.route) { RecordsScreen() }
                composable(Screen.Recon.route) { ReconScreen() }
            }
        }

        // Bottom navigation
        GhostNavBar(navController = navController, screens = screens)
    }
}

@Composable
fun GhostNavBar(navController: NavHostController, screens: List<Screen>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostBlack)
            .border(
                width = 0.5.dp,
                color = GhostBorder
            )
    ) {
        screens.forEach { screen ->
            val isActive = currentRoute == screen.route
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate(screen.route) }
                    .border(
                        width = if (isActive) 1.dp else 0.dp,
                        color = if (isActive) GhostWhite else Color.Transparent,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Nav icon box
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(
                                1.dp,
                                if (isActive) GhostWhite else GhostWhiteFaint
                            )
                    )
                    Text(
                        text = screen.label,
                        style = GhostTypography.micro.copy(
                            color = if (isActive) GhostWhite else GhostWhiteFaint
                        )
                    )
                }
            }
        }
    }
}

// Placeholder screens
@Composable fun CommsScreen() {
    Box(Modifier.fillMaxSize().background(GhostBlack), Alignment.Center) {
        Text("COMMS", style = GhostTypography.title)
    }
}
@Composable fun ProtocolsScreen() {
    Box(Modifier.fillMaxSize().background(GhostBlack), Alignment.Center) {
        Text("PROTOCOLS", style = GhostTypography.title)
    }
}
@Composable fun RecordsScreen() {
    Box(Modifier.fillMaxSize().background(GhostBlack), Alignment.Center) {
        Text("RECORDS", style = GhostTypography.title)
    }
}
@Composable fun ReconScreen() {
    Box(Modifier.fillMaxSize().background(GhostBlack), Alignment.Center) {
        Text("RECON", style = GhostTypography.title)
    }
}
