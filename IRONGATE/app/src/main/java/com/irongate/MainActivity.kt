package com.irongate

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irongate.model.AssistantState
import com.irongate.protocol.ProtocolStep
import com.irongate.ui.MainViewModel
import com.irongate.ui.components.*
import com.irongate.ui.screens.*
import com.irongate.ui.theme.IronColors
import com.irongate.ui.theme.IrongateTheme
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Chat : Screen("chat")
    object Logs : Screen("logs")
    object Settings : Screen("settings")
    object Nuke : Screen("nuke")
}

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request overlay permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1001)
        }

        setContent {
            IrongateTheme {
                IrongateApp(
                    onPlayNukeAudio = { playNukeAudio() }
                )
            }
        }
    }

    private fun playNukeAudio() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val afd = assets.openFd("destroyer-of-world.mp3")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

@Composable
fun IrongateApp(onPlayNukeAudio: () -> Unit) {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var interfaceExpanded by remember { mutableStateOf(false) }
    var showProtocolOverlay by remember { mutableStateOf(false) }

    val connStatus by vm.connectionStatus.collectAsStateWithLifecycle(
        initialValue = com.irongate.model.ConnectionStatus()
    )
    val protocolState by vm.protocolState.collectAsStateWithLifecycle()
    val feed by vm.feed.collectAsStateWithLifecycle()
    val chatMessages by vm.chatMessages.collectAsStateWithLifecycle()
    val callsign by vm.callsign.collectAsStateWithLifecycle()

    // Bind to service
    LaunchedEffect(Unit) {
        vm.bindService(context)
    }

    // Show overlay when protocol starts
    LaunchedEffect(protocolState.running) {
        if (protocolState.running) showProtocolOverlay = true
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            IrongateDrawer(
                currentScreen = currentScreen,
                interfaceExpanded = interfaceExpanded,
                connStatus = connStatus,
                onToggleInterface = { interfaceExpanded = !interfaceExpanded },
                onNavigate = { screen ->
                    currentScreen = screen
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IronColors.Black)
        ) {
            Column(Modifier.fillMaxSize()) {

                // ─── TOP BAR ──────────────────────────────────────────────
                TopBar(
                    ghostState = connStatus.ghost,
                    gipsyState = connStatus.gipsy,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )

                // ─── CONTENT ──────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        Screen.Dashboard -> DashboardScreen(
                            connectionStatus = connStatus,
                            protocolState = protocolState,
                            onCommenceProtocol = {
                                showProtocolOverlay = true
                                vm.commenceProtocol()
                            }
                        )
                        Screen.Chat -> ChatScreen(
                            messages = chatMessages,
                            onSend = { target, msg -> vm.sendChat(target, msg) }
                        )
                        Screen.Logs -> LogsScreen(feed = feed)
                        Screen.Settings -> SettingsScreen(
                            callsign = callsign,
                            onSaveApiKey = { vm.saveApiKey(context, it) },
                            onSaveCallsign = { t, n, w -> vm.saveCallsign(context, t, n, w) },
                            getApiKey = { vm.getApiKey(context) }
                        )
                        Screen.Nuke -> NukeScreen(
                            onExecuteNuke = { vm.executeNuke(it) },
                            onPlayAudio = onPlayNukeAudio
                        )
                    }
                }
            }

            // ─── FLOATING STATUS BAR ──────────────────────────────────────
            FloatingStatusBar(
                connStatus = connStatus,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // ─── PROTOCOL OVERLAY ─────────────────────────────────────────
            if (showProtocolOverlay) {
                ProtocolOverlay(
                    state = protocolState,
                    onDismiss = {
                        showProtocolOverlay = false
                        vm.resetProtocol()
                    }
                )
            }
        }
    }
}

@Composable
fun TopBar(
    ghostState: AssistantState,
    gipsyState: AssistantState,
    onMenuClick: () -> Unit
) {
    val allOnline = ghostState == AssistantState.ONLINE && gipsyState == AssistantState.ONLINE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IronColors.Black)
            .border(0.dp, IronColors.Dark2)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Hamburger
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.clickable { onMenuClick() }.padding(4.dp)
            ) {
                repeat(3) {
                    Box(Modifier.width(22.dp).height(1.5.dp).background(IronColors.White))
                }
            }

            // Logo placeholder (square)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(IronColors.Dark2)
                    .border(1.dp, IronColors.Dark3),
                contentAlignment = Alignment.Center
            ) {
                PixelLabel("IG", fontSize = 5.sp, color = IronColors.White)
            }

            Column {
                PixelLabel("IRONGATE", fontSize = 9.sp, color = IronColors.White)
                MonoText("BRIDGE v1.0", fontSize = 9.sp, color = IronColors.Gray1)
            }
        }

        StatusDot(if (allOnline) AssistantState.ONLINE else AssistantState.OFFLINE, 8.dp)
    }

    // Top border line
    Box(Modifier.fillMaxWidth().height(1.dp).background(IronColors.Dark2))
}

@Composable
fun IrongateDrawer(
    currentScreen: Screen,
    interfaceExpanded: Boolean,
    connStatus: com.irongate.model.ConnectionStatus,
    onToggleInterface: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(IronColors.NearBlack)
            .border(0.dp, IronColors.Dark2)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IronColors.Black)
                .border(0.dp, IronColors.Dark2)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(IronColors.Dark2).border(1.dp, IronColors.Dark3),
                contentAlignment = Alignment.Center
            ) {
                PixelLabel("IG", fontSize = 6.sp, color = IronColors.White)
            }
            Column {
                PixelLabel("IRONGATE", fontSize = 8.sp, color = IronColors.White)
                MonoText("BRIDGE v1.0", fontSize = 9.sp, color = IronColors.Gray1)
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(IronColors.Dark2))

        // Nav items
        DrawerItem("DASHBOARD", currentScreen == Screen.Dashboard) { onNavigate(Screen.Dashboard) }

        // INTERFACE with sub-menu
        DrawerItem("INTERFACE", false, hasChevron = true) { onToggleInterface() }
        AnimatedVisibility(visible = interfaceExpanded) {
            Column {
                listOf("GHOST", "GIPSY", "BOTH").forEach { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Screen.Chat) }
                            .padding(start = 44.dp, top = 10.dp, bottom = 10.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(4.dp).background(IronColors.Gray1))
                        MonoText(target, fontSize = 10.sp, color = IronColors.Gray2)
                        // Status dot for GHOST/GIPSY
                        if (target == "GHOST") {
                            Spacer(Modifier.weight(1f))
                            StatusDot(connStatus.ghost, 5.dp)
                        } else if (target == "GIPSY") {
                            Spacer(Modifier.weight(1f))
                            StatusDot(connStatus.gipsy, 5.dp)
                        }
                    }
                }
            }
        }

        DrawerItem("LOGS", currentScreen == Screen.Logs) { onNavigate(Screen.Logs) }
        DrawerItem("SETTINGS", currentScreen == Screen.Settings) { onNavigate(Screen.Settings) }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(1.dp).background(IronColors.DangerDark))
        DrawerItem("NUKE", currentScreen == Screen.Nuke, textColor = IronColors.Danger) {
            onNavigate(Screen.Nuke)
        }

        // Footer
        Box(Modifier.fillMaxWidth().height(1.dp).background(IronColors.Dark2))
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(connStatus.ghost, 5.dp)
            MonoText("GHOST", fontSize = 9.sp, color = IronColors.Gray1)
            Spacer(Modifier.width(8.dp))
            StatusDot(connStatus.gipsy, 5.dp)
            MonoText("GIPSY", fontSize = 9.sp, color = IronColors.Gray1)
        }
        MonoText(
            "LOCALHOST ONLY · NO INTERNET",
            fontSize = 8.sp,
            color = IronColors.Gray1,
            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun DrawerItem(
    label: String,
    active: Boolean,
    textColor: androidx.compose.ui.graphics.Color = IronColors.Gray2,
    hasChevron: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) IronColors.Dark1 else IronColors.NearBlack)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MonoText(
            label,
            fontSize = 11.sp,
            color = if (active) IronColors.White else textColor
        )
        if (hasChevron) {
            MonoText("›", fontSize = 14.sp, color = IronColors.Gray1)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(IronColors.Dark2))
}

@Composable
fun FloatingStatusBar(
    connStatus: com.irongate.model.ConnectionStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(IronColors.Black)
            .border(0.dp, IronColors.Dark2)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusDot(connStatus.ghost, 5.dp)
            MonoText("GHOST", fontSize = 9.sp, color = IronColors.Gray1)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusDot(connStatus.gipsy, 5.dp)
            MonoText("GIPSY", fontSize = 9.sp, color = IronColors.Gray1)
        }
        val h = connStatus.uptimeSeconds / 3600
        val m = (connStatus.uptimeSeconds % 3600) / 60
        val s = connStatus.uptimeSeconds % 60
        MonoText("UP: %02d:%02d:%02d".format(h, m, s), fontSize = 9.sp, color = IronColors.Gray1)
        MonoText("8765·8766", fontSize = 8.sp, color = IronColors.Dark4)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(IronColors.Dark2))
}
