package com.ghost.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.ui.theme.*

@Composable
fun SettingsScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("STATUS", "PROTOCOLS", "IDENTITY", "CONFIG", "CALLSIGN")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GhostBlack)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GHOST", style = GhostTypography.display.copy(fontSize = 16.sp))
            Text("BUILD 1.0 // OFFLINE", style = GhostTypography.micro)
        }

        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GhostSurface)
                .border(BorderStroke(0.5.dp, GhostBorder))
        ) {
            tabs.forEachIndexed { index, tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (selectedTab == index) 0.dp else 0.dp,
                            color = GhostWhite
                        )
                        .background(
                            if (selectedTab == index) GhostSurface2 else GhostBlack
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (selectedTab == index) GhostWhite else GhostBorder
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = GhostTypography.micro.copy(
                            color = if (selectedTab == index) GhostWhite else GhostWhiteFaint,
                            fontSize = 5.sp
                        )
                    )
                }
            }
        }

        // Content
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(200))
            },
            label = "settings_tab",
            modifier = Modifier.weight(1f)
        ) { tab ->
            when (tab) {
                0 -> StatusTab()
                1 -> ProtocolsTab()
                2 -> IdentityTab()
                3 -> ConfigTab()
                4 -> CallsignTab()
            }
        }
    }
}

@Composable
private fun StatusTab() {
    val pulse = rememberInfiniteTransition(label = "status_pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(dotAlpha)
                        .background(GhostWhite, RoundedCornerShape(50))
                )
                Text("SYSTEM ACTIVE", style = GhostTypography.small.copy(color = GhostWhite))
                Spacer(Modifier.weight(1f))
                Text("UPTIME 00:00:00", style = GhostTypography.micro)
            }
        }

        item { GhostDivider() }

        item {
            InfoGrid(
                listOf(
                    "WAKE WORD" to "GHOST",
                    "STT ENGINE" to "VOSK",
                    "LLM" to "GEMMA 3 1B",
                    "PERSON ID" to "FACE+VOICE",
                    "BRIDGE" to "CONNECTING...",
                    "IRONGATE" to "EVERY 1HR"
                )
            )
        }

        item {
            StatusCard(label = "ACTIVE PROTOCOL", value = "-- NONE --", dim = true)
        }

        item {
            StatusCard(label = "ACTIVE MODE", value = "NORMAL", dim = false)
        }
    }
}

@Composable
private fun ProtocolsTab() {
    val protocols = listOf(
        "DOOMSDAY" to "ONE-SHOT",
        "BLACKOUT" to "TOGGLE",
        "GHOST" to "TOGGLE",
        "LOCKDOWN" to "TOGGLE",
        "MORNING" to "AUTO",
        "NIGHT" to "ONE-SHOT",
        "DRIVE" to "TOGGLE",
        "FOCUS" to "TOGGLE",
        "RECON" to "ONE-SHOT",
        "PURGE" to "ONE-SHOT",
        "SHADOW" to "TOGGLE",
        "INCOGNITO" to "TOGGLE",
        "BROADCAST" to "ONE-SHOT",
        "BRIEFING" to "ONE-SHOT",
        "SHUTDOWN" to "ONE-SHOT",
        "SOS LITE" to "ONE-SHOT",
        "CAMOUFLAGE" to "TOGGLE",
        "PHANTOM" to "AUTO",
        "FORTRESS" to "TOGGLE",
        "HUNT" to "TOGGLE",
        "DECOY" to "TOGGLE",
        "PANIC" to "ONE-SHOT",
        "ANTI-THEFT" to "TOGGLE",
        "DEBRIEF" to "ONE-SHOT",
        "GUARDIAN" to "TOGGLE",
        "CLASSIFIED" to "TOGGLE",
        "INTERCEPTOR" to "TOGGLE",
        "PHANTOM CALL" to "TOGGLE",
        "SENTINEL" to "TOGGLE",
        "PREDATOR" to "TOGGLE",
        "IRONGATE" to "AUTO+MANUAL"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(protocols) { index, (name, type) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GhostSurface, RoundedCornerShape(2.dp))
                    .border(0.5.dp, GhostBorder, RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}. $name",
                    style = GhostTypography.small.copy(color = GhostWhite)
                )
                Text(
                    text = type,
                    style = GhostTypography.micro
                )
            }
        }
    }
}

@Composable
private fun IdentityTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("SAVED PROFILES", style = GhostTypography.label)
            Spacer(Modifier.height(6.dp))
        }

        item {
            // Owner profile
            PersonCard(name = "HARI", tier = "OWNER", relationship = "Owner")
        }

        item {
            Text("No other profiles saved.", style = GhostTypography.micro)
        }

        item { GhostDivider() }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, GhostWhiteDim, RoundedCornerShape(2.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SAY \"GHOST, SAVE THEM\" TO ADD A PERSON",
                    style = GhostTypography.micro,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PersonCard(name: String, tier: String, relationship: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostSurface, RoundedCornerShape(2.dp))
            .border(0.5.dp, GhostBorder, RoundedCornerShape(2.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, style = GhostTypography.small.copy(color = GhostWhite))
            Text(relationship, style = GhostTypography.micro)
        }
        Text(tier, style = GhostTypography.micro.copy(color = GhostWhite))
    }
}

@Composable
private fun ConfigTab() {
    var showResetScreen by remember { mutableStateOf(false) }

    if (showResetScreen) {
        FactoryResetScreen(
            onReset = { target ->
                // Trigger reset
                showResetScreen = false
            },
            onCancel = { showResetScreen = false }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { ConfigSection(label = "EMERGENCY CONTACT", placeholder = "ENTER NUMBER") }
        item { ConfigSection(label = "DOOMSDAY MESSAGE", placeholder = "ENTER MESSAGE") }
        item { ConfigSection(label = "SOS CONTACT", placeholder = "ENTER NUMBER") }
        item { ConfigSection(label = "MORNING WINDOW", placeholder = "05:00 - 09:00") }
        item { ConfigSection(label = "DRIVE PLAYLIST", placeholder = "PLAYLIST NAME") }
        item { ConfigSection(label = "MORNING PLAYLIST", placeholder = "PLAYLIST NAME") }
        item { GhostDivider() }
        item {
            Text("MEMORY", style = GhostTypography.label)
            Spacer(Modifier.height(6.dp))
            Text("Total entries: 0", style = GhostTypography.micro)
            Spacer(Modifier.height(8.dp))
            GhostActionButton(label = "VIEW MEMORIES", onClick = {})
            Spacer(Modifier.height(6.dp))
            GhostActionButton(label = "DELETE MEMORY", onClick = {})
        }
        item { GhostDivider() }
        item {
            Text("DANGER ZONE", style = GhostTypography.label.copy(color = GhostRed))
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, GhostRed, RoundedCornerShape(2.dp))
                    .background(GhostSurface, RoundedCornerShape(2.dp))
                    .clickable { showResetScreen = true }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "FACTORY RESET",
                    style = GhostTypography.small.copy(color = GhostRed)
                )
            }
        }
    }
}

@Composable
private fun CallsignTab() {
    var selectedMember by remember { mutableStateOf("GHOST") }
    val members = listOf("GHOST", "GIPSY", "BRIDGE")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "RENAME ECOSYSTEM MEMBERS.\nCHANGES WAKE WORD AND DISPLAY NAME.",
                style = GhostTypography.micro,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { GhostDivider() }

        item {
            Text("SELECT MEMBER", style = GhostTypography.label)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEach { member ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                0.5.dp,
                                if (selectedMember == member) GhostWhite else GhostBorder,
                                RoundedCornerShape(2.dp)
                            )
                            .background(
                                if (selectedMember == member) GhostSurface2 else GhostBlack,
                                RoundedCornerShape(2.dp)
                            )
                            .clickable { selectedMember = member }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            member,
                            style = GhostTypography.micro.copy(
                                color = if (selectedMember == member) GhostWhite else GhostWhiteDim
                            )
                        )
                    }
                }
            }
        }

        item { GhostDivider() }

        item {
            Text("CURRENT NAME: $selectedMember", style = GhostTypography.small)
            Spacer(Modifier.height(8.dp))
            ConfigSection(label = "NEW CALLSIGN", placeholder = "ENTER NEW NAME (MAX 12 CHARS)")
            Spacer(Modifier.height(8.dp))
            ConfigSection(label = "NEW WAKE WORD", placeholder = "ENTER WAKE WORD (LEAVE BLANK = MATCH CALLSIGN)")
            Spacer(Modifier.height(12.dp))
            GhostActionButton(label = "SAVE CALLSIGN", onClick = {})
        }

        item { GhostDivider() }

        item {
            Text("ACTIVE CALLSIGNS", style = GhostTypography.label)
            Spacer(Modifier.height(8.dp))
            listOf(
                "GHOST" to "WAKE: GHOST",
                "GIPSY" to "WAKE: GIPSY",
                "BRIDGE" to "WAKE: BRIDGE"
            ).forEach { (name, wake) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, style = GhostTypography.small.copy(color = GhostWhite))
                    Text(wake, style = GhostTypography.micro)
                }
            }
        }
    }
}

@Composable
private fun ConfigSection(label: String, placeholder: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = GhostTypography.micro)
        var value by remember { mutableStateOf("") }
        TextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(placeholder, style = GhostTypography.micro.copy(color = GhostWhiteFaint))
            },
            textStyle = GhostTypography.small.copy(color = GhostWhite),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GhostSurface,
                unfocusedContainerColor = GhostSurface,
                focusedIndicatorColor = GhostWhite,
                unfocusedIndicatorColor = GhostBorder,
                cursorColor = GhostWhite
            ),
            singleLine = true
        )
    }
}

@Composable
private fun GhostActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GhostWhiteDim, RoundedCornerShape(2.dp))
            .background(GhostSurface, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = GhostTypography.small.copy(color = GhostWhite))
    }
}

@Composable
private fun InfoGrid(items: List<Pair<String, String>>) {
    val chunked = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(GhostSurface, RoundedCornerShape(2.dp))
                            .border(0.5.dp, GhostBorder, RoundedCornerShape(2.dp))
                            .padding(8.dp)
                    ) {
                        Text(label, style = GhostTypography.micro)
                        Spacer(Modifier.height(2.dp))
                        Text(value, style = GhostTypography.small.copy(color = GhostWhite))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusCard(label: String, value: String, dim: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostSurface, RoundedCornerShape(2.dp))
            .border(0.5.dp, GhostBorder, RoundedCornerShape(2.dp))
            .padding(10.dp)
    ) {
        Text(label, style = GhostTypography.micro)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = GhostTypography.small.copy(
                color = if (dim) GhostWhiteFaint else GhostWhite
            )
        )
    }
}

@Composable
private fun GhostDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(GhostBorder)
    )
}
