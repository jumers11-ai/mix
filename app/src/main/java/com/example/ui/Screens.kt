package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CompatibilityEngine
import com.example.data.FlowEntity
import com.example.data.TrackEntity
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ========================================================
// CORE SCREENS ENTRY POINTS
// ========================================================

@Composable
fun MixgraphAppContent(viewModel: MixgraphViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            MixgraphBottomNavigation(
                selectedTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    MixgraphTab.DASHBOARD -> DashboardScreen(viewModel)
                    MixgraphTab.TRACKS -> TracksScreen(viewModel)
                    MixgraphTab.FLOW_BUILDER -> FlowBuilderScreen(viewModel)
                    MixgraphTab.SET_GENERATOR -> SetGeneratorScreen(viewModel)
                    MixgraphTab.LIVE_MODE -> LiveModeScreen(viewModel)
                    MixgraphTab.AI_ASSISTANT -> AIAssistantScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MixgraphBottomNavigation(
    selectedTab: MixgraphTab,
    onTabSelected: (MixgraphTab) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val tabs = listOf(
            Triple(MixgraphTab.DASHBOARD, "Kokpit", Icons.Default.Home),
            Triple(MixgraphTab.TRACKS, "Utwory", Icons.Default.List),
            Triple(MixgraphTab.FLOW_BUILDER, "Kreator", Icons.Default.Build),
            Triple(MixgraphTab.SET_GENERATOR, "Generator", Icons.Default.Star),
            Triple(MixgraphTab.LIVE_MODE, "Live", Icons.Default.PlayArrow),
            Triple(MixgraphTab.AI_ASSISTANT, "Czat AI", Icons.Default.Face)
        )

        tabs.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selectedTab == tab) NeonCyan else TextSecondary
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == tab) NeonCyan else TextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = DarkSurfaceAlt
                ),
                modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
            )
        }
    }
}

// ========================================================
// 1. DASHBOARD SCREEN
// ========================================================

@Composable
fun DashboardScreen(viewModel: MixgraphViewModel) {
    val trackCount by viewModel.trackCount.collectAsStateWithLifecycle()
    val flowCount by viewModel.flowCount.collectAsStateWithLifecycle()
    val sessionCount by viewModel.sessionCount.collectAsStateWithLifecycle()
    val avgComp by viewModel.averageCompatibility.collectAsStateWithLifecycle()
    val recentSessions by viewModel.sessions.collectAsStateWithLifecycle()
    val tracksList by viewModel.tracks.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Mixgraph Solo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Darmowa, inteligentna asysta DJ-ska offline",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Baza utworów",
                    value = trackCount.toString(),
                    subtitle = "aktywnych tracków",
                    icon = Icons.Default.Star,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Projekty Flow",
                    value = flowCount.toString(),
                    subtitle = "wizualnych setów",
                    icon = Icons.Default.Build,
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Zapisane sesje",
                    value = sessionCount.toString(),
                    subtitle = "historii miksów",
                    icon = Icons.Default.Refresh,
                    accentColor = OrangeBoost,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Średnia zgodność",
                    value = "$avgComp%",
                    subtitle = "kompatybilności",
                    icon = Icons.Default.ThumbUp,
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Energy vs BPM Overview Header
        item {
            Text(
                text = "Rozkład Energii i BPM biblioteki",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Custom Graphics Rozkład
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Wizualizacja energii i tempa",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .drawBehind {
                                // Draw grid
                                val gridLines = 4
                                for (i in 0..gridLines) {
                                    val y = size.height * (i.toFloat() / gridLines)
                                    drawLine(
                                        color = TextMuted.copy(alpha = 0.2f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                }

                                if (tracksList.isEmpty()) return@drawBehind

                                // Draw scatter plot points of energy (Y) vs BPM (X)
                                tracksList.forEach { track ->
                                    // Scale BPM: 110 to 140
                                    val bpmMin = 110f
                                    val bpmMax = 140f
                                    val xPct = ((track.bpm.toFloat() - bpmMin) / (bpmMax - bpmMin)).coerceIn(0f, 1f)
                                    // Scale Energy: 0 to 100 (invert Y)
                                    val yPct = 1f - (track.energy.toFloat() / 100f)

                                    val xPos = xPct * size.width
                                    val yPos = yPct * size.height

                                    // Color based on energy
                                    val color = when {
                                        track.energy > 80 -> NeonPurple
                                        track.energy > 50 -> NeonCyan
                                        else -> NeonGreen
                                    }

                                    drawCircle(
                                        color = color,
                                        radius = 8f,
                                        center = Offset(xPos, yPos)
                                    )
                                    drawCircle(
                                        color = color.copy(alpha = 0.3f),
                                        radius = 16f,
                                        center = Offset(xPos, yPos)
                                    )
                                }
                            }
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Low Energy (Głębokie)", fontSize = 11.sp, color = TextSecondary)
                        Text("High Energy (Klubowe)", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Recent Activity History Log
        item {
            Text(
                text = "Ostatnie sesje i aktywność",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (recentSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Text(
                        text = "Brak zapisanych sesji. Użyj generatora lub trybu na żywo!",
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(recentSessions.take(5)) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (session.type == "LIVE") NeonGreen.copy(alpha = 0.15f) else NeonPurple.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (session.type == "LIVE") Icons.Default.PlayArrow else Icons.Default.Build,
                                contentDescription = session.type,
                                tint = if (session.type == "LIVE") NeonGreen else NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.name,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (session.type == "LIVE") "Tryb Występu Live" else "Set Automatyczny",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        
                        Text(
                            text = "Ukończona",
                            fontSize = 11.sp,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkSurfaceAlt)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

// ========================================================
// 2. TRACKS SCREEN (DATABASE & FILTERS)
// ========================================================

@Composable
fun TracksScreen(viewModel: MixgraphViewModel) {
    val filteredList by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val genres by viewModel.availableGenres.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.filterGenre.collectAsStateWithLifecycle()
    val bpmMin by viewModel.filterBpmMin.collectAsStateWithLifecycle()
    val bpmMax by viewModel.filterBpmMax.collectAsStateWithLifecycle()
    val energyMin by viewModel.filterEnergyMin.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header with search & add
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Baza Utworów",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.background(NeonCyan, CircleShape).testTag("add_track_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj utwór", tint = DarkBg)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Filter Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Szukaj artysty lub tytułu...", color = TextMuted) },
                    modifier = Modifier.weight(1f).testTag("track_search_input"),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceAlt,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = { filterExpanded = !filterExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterExpanded) NeonCyan else DarkSurface,
                        contentColor = if (filterExpanded) DarkBg else TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Filtruj",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Advanced Filters Area
            AnimatedVisibility(visible = filterExpanded) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkSurfaceAlt)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Filtry Zaawansowane",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Genre Dropdown selector (Simple row buttons)
                        Text("Gatunek:", fontSize = 12.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genres.forEach { genre ->
                                val isSelected = genre == selectedGenre
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) NeonCyan else DarkSurfaceAlt,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { viewModel.filterGenre.value = genre }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        color = if (isSelected) DarkBg else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // BPM Slider range simulator
                        Text("Zakres BPM: $bpmMin - $bpmMax BPM", fontSize = 12.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (bpmMin > 80) viewModel.filterBpmMin.value = bpmMin - 5 },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceAlt),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp)
                            ) { Text("-", color = TextPrimary) }
                            
                            Text(
                                "BPM Min: $bpmMin",
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                fontSize = 12.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = { if (bpmMin < bpmMax) viewModel.filterBpmMin.value = bpmMin + 5 },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceAlt),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp)
                            ) { Text("+", color = TextPrimary) }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (bpmMax > bpmMin) viewModel.filterBpmMax.value = bpmMax - 5 },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceAlt),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp)
                            ) { Text("-", color = TextPrimary) }
                            
                            Text(
                                "BPM Max: $bpmMax",
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                fontSize = 12.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = { if (bpmMax < 160) viewModel.filterBpmMax.value = bpmMax + 5 },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceAlt),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp)
                            ) { Text("+", color = TextPrimary) }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Energy filter
                        Text("Minimalna Energia: $energyMin%", fontSize = 12.sp, color = TextSecondary)
                        Slider(
                            value = energyMin.toFloat(),
                            onValueChange = { viewModel.filterEnergyMin.value = it.toInt() },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = DarkSurfaceAlt
                            )
                        )

                        // Clear filters button
                        TextButton(
                            onClick = {
                                viewModel.searchQuery.value = ""
                                viewModel.filterGenre.value = "All"
                                viewModel.filterBpmMin.value = 110
                                viewModel.filterBpmMax.value = 140
                                viewModel.filterEnergyMin.value = 0
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Wyczyść filtry", color = NeonCyan)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tracks list
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Brak utworów spełniających kryteria.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { track ->
                        TrackItemCard(
                            track = track,
                            onDelete = { viewModel.deleteTrack(track) },
                            onSelectLive = {
                                viewModel.selectLiveTrack(track)
                                viewModel.selectTab(MixgraphTab.LIVE_MODE)
                            },
                            onAddToCanvas = {
                                viewModel.addTrackToCanvas(track.id)
                            }
                        )
                    }
                }
            }
        }

        // Add track dialog
        if (showAddDialog) {
            AddTrackDialog(
                onDismiss = { showAddDialog = false },
                onAddTrack = { title, artist, bpm, camelot, key, energy, genre ->
                    viewModel.addTrack(title, artist, bpm, camelot, key, energy, genre)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun TrackItemCard(
    track: TrackEntity,
    onDelete: () -> Unit,
    onSelectLive: () -> Unit,
    onAddToCanvas: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DarkSurfaceAlt)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork Placeholder
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(NeonPurple.copy(alpha = 0.4f), DarkSurfaceAlt)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.genre,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${track.bpm.toInt()} BPM",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Key Badge and Energy Indicator
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Camelot badge
                Box(
                    modifier = Modifier
                        .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = track.camelotKey,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Energy mini bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Energia",
                        tint = NeonPurple,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "${track.energy}%",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Column {
                IconButton(onClick = onSelectLive) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Wybierz na żywo", tint = NeonGreen)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun AddTrackDialog(
    onDismiss: () -> Unit,
    onAddTrack: (String, String, Double, String, String, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var bpm by remember { mutableStateOf("124") }
    var camelot by remember { mutableStateOf("8A") }
    var key by remember { mutableStateOf("Am") }
    var energy by remember { mutableStateOf(75f) }
    var genre by remember { mutableStateOf("Tech House") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DarkSurfaceAlt)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Dodaj Nowy Utwór",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł utworu") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                    modifier = Modifier.fillMaxWidth().testTag("add_title_input")
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Wykonawca") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bpm,
                        onValueChange = { bpm = it },
                        label = { Text("BPM") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = camelot,
                        onValueChange = { camelot = it },
                        label = { Text("Camelot Key") },
                        placeholder = { Text("e.g. 8A") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Musical Key") },
                        placeholder = { Text("e.g. Am") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Gatunek") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                        modifier = Modifier.weight(1f)
                    )
                }

                Column {
                    Text(
                        text = "Energia utworu: ${energy.toInt()}%",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Slider(
                        value = energy,
                        onValueChange = { energy = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && artist.isNotBlank()) {
                                onAddTrack(
                                    title,
                                    artist,
                                    bpm.toDoubleOrNull() ?: 120.0,
                                    camelot,
                                    key,
                                    energy.toInt(),
                                    genre
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg),
                        modifier = Modifier.testTag("submit_track_button")
                    ) {
                        Text("Zapisz", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ========================================================
// 3. FLOW BUILDER (VISUAL CANVAS)
// ========================================================

@Composable
fun FlowBuilderScreen(viewModel: MixgraphViewModel) {
    val flowsList by viewModel.flows.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedFlowId.collectAsStateWithLifecycle()
    val nodes by viewModel.canvasNodes.collectAsStateWithLifecycle()
    val edges by viewModel.canvasEdges.collectAsStateWithLifecycle()
    val allTracks by viewModel.tracks.collectAsStateWithLifecycle()

    var showNewFlowDialog by remember { mutableStateOf(false) }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var connectModeEnabled by remember { mutableStateOf(false) }
    var selectedSourceNodeId by remember { mutableStateOf<Long?>(null) }

    val activeFlow = flowsList.find { it.id == selectedId }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Flow Select Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                var expandedDropdown by remember { mutableStateOf(false) }
                Button(
                    onClick = { expandedDropdown = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeFlow?.name ?: "Wybierz projekt Flow...",
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NeonCyan)
                    }
                }

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.background(DarkSurface).border(1.dp, DarkSurfaceAlt)
                ) {
                    flowsList.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f.name, color = TextPrimary) },
                            onClick = {
                                viewModel.selectFlow(f.id)
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            IconButton(
                onClick = { showNewFlowDialog = true },
                modifier = Modifier.background(NeonCyan, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Create, contentDescription = "Nowy Flow", tint = DarkBg)
            }

            IconButton(
                onClick = { viewModel.deleteCurrentFlow() },
                modifier = Modifier.background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń Flow", tint = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool bar for Flow controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showAddNodeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceAlt),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dodaj Utwór", fontSize = 12.sp)
            }

            Button(
                onClick = { 
                    connectModeEnabled = !connectModeEnabled 
                    selectedSourceNodeId = null
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (connectModeEnabled) NeonCyan else DarkSurfaceAlt,
                    contentColor = if (connectModeEnabled) DarkBg else TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (connectModeEnabled) "Wybierz węzły..." else "Połącz utwory", fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.clearCanvas() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Wyczyść", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // INTERACTIVE CANVAS GRID DRAWING
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, DarkSurfaceAlt, RoundedCornerShape(12.dp))
        ) {
            // Draw visual grid background & connections
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Grid pattern
                val dotSpacing = 30f
                val dotColor = TextMuted.copy(alpha = 0.15f)
                for (x in 0..size.width.toInt() step dotSpacing.toInt()) {
                    for (y in 0..size.height.toInt() step dotSpacing.toInt()) {
                        drawCircle(color = dotColor, radius = 2f, center = Offset(x.toFloat(), y.toFloat()))
                    }
                }

                // 2. Connecting Lines (Edges)
                edges.forEach { edge ->
                    // Find node positions
                    val sourceNode = nodes.find { it.trackId == edge.sourceTrackId }
                    val targetNode = nodes.find { it.trackId == edge.targetTrackId }
                    if (sourceNode != null && targetNode != null) {
                        val sourceTrack = allTracks.find { it.id == edge.sourceTrackId }
                        val targetTrack = allTracks.find { it.id == edge.targetTrackId }
                        if (sourceTrack != null && targetTrack != null) {
                            val score = CompatibilityEngine.calculateCompatibility(sourceTrack, targetTrack)
                            val strokeColor = when {
                                score >= 85 -> NeonGreen
                                score >= 70 -> NeonCyan
                                else -> OrangeBoost
                            }

                            // Center coordinates for drawing line (compensating for 90dp width and 70dp height of node cards roughly)
                            val startX = sourceNode.x + 120f
                            val startY = sourceNode.y + 40f
                            val endX = targetNode.x + 120f
                            val endY = targetNode.y + 40f

                            // Draw elegant curve or line
                            drawLine(
                                color = strokeColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 5f
                            )

                            // Draw a circle in the middle with compatibility text
                            val midX = (startX + endX) / 2
                            val midY = (startY + endY) / 2
                            drawCircle(
                                color = DarkBg,
                                radius = 25f,
                                center = Offset(midX, midY)
                            )
                            drawCircle(
                                color = strokeColor,
                                radius = 25f,
                                style = Stroke(width = 3f),
                                center = Offset(midX, midY)
                            )
                        }
                    }
                }
            }

            // Text on connection mode helper
            if (connectModeEnabled) {
                Text(
                    text = if (selectedSourceNodeId == null) "Krok 1: Dotknij pierwszy utwór (źródło)" else "Krok 2: Dotknij utwór docelowy",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                        .background(DarkBg.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Draw Draggable Nodes
            nodes.forEach { node ->
                val track = allTracks.find { it.id == node.trackId }
                if (track != null) {
                    Box(
                        modifier = Modifier
                            .offset(x = node.x.dp, y = node.y.dp)
                            .pointerInput(node.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    viewModel.updateNodePosition(
                                        node.id,
                                        (node.x + dragAmount.x / 2f).coerceAtLeast(0f),
                                        (node.y + dragAmount.y / 2f).coerceAtLeast(0f)
                                    )
                                }
                            }
                            .clickable {
                                if (connectModeEnabled) {
                                    if (selectedSourceNodeId == null) {
                                        selectedSourceNodeId = node.trackId
                                    } else {
                                        if (selectedSourceNodeId != node.trackId) {
                                            viewModel.connectNodesInCanvas(
                                                selectedSourceNodeId!!,
                                                node.trackId
                                            )
                                        }
                                        connectModeEnabled = false
                                        selectedSourceNodeId = null
                                    }
                                }
                            }
                            .width(135.dp)
                            .background(
                                if (selectedSourceNodeId == node.trackId) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceAlt,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.5.dp,
                                if (selectedSourceNodeId == node.trackId) NeonCyan else DarkSurfaceAlt,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.camelotKey,
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                IconButton(
                                    onClick = { viewModel.removeNodeFromCanvas(node.id) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Usuń", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = track.title,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${track.bpm.toInt()} BPM", fontSize = 9.sp, color = TextMuted)
                                Text("E: ${track.energy}%", fontSize = 9.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Synced Timeline chart (waveform) of energy & BPM progression
        Text("Prognoza fali energii seta (BPM i Energia)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkSurfaceAlt)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (nodes.isEmpty()) return@drawBehind

                        // We sort nodes by their X position on canvas to represent a timeline sequence!
                        val sortedNodeTracks = nodes
                            .sortedBy { it.x }
                            .mapNotNull { node -> allTracks.find { it.id == node.trackId } }

                        if (sortedNodeTracks.isEmpty()) return@drawBehind

                        val points = sortedNodeTracks.size
                        val stepX = size.width / (points + 1)

                        val energyPath = Path()
                        val bpmPath = Path()

                        sortedNodeTracks.forEachIndexed { idx, track ->
                            val x = stepX * (idx + 1)
                            // Scale energy (0-100) to Y
                            val energyY = size.height - (track.energy / 100f * size.height * 0.8f) - 5f
                            // Scale BPM (110-140) to Y
                            val bpmMin = 110f
                            val bpmMax = 140f
                            val bpmNorm = ((track.bpm.toFloat() - bpmMin) / (bpmMax - bpmMin)).coerceIn(0f, 1f)
                            val bpmY = size.height - (bpmNorm * size.height * 0.8f) - 5f

                            if (idx == 0) {
                                energyPath.moveTo(x, energyY)
                                bpmPath.moveTo(x, bpmY)
                            } else {
                                energyPath.lineTo(x, energyY)
                                bpmPath.lineTo(x, bpmY)
                            }

                            // Draw node dot
                            drawCircle(color = NeonPurple, radius = 4f, center = Offset(x, energyY))
                            drawCircle(color = NeonCyan, radius = 4f, center = Offset(x, bpmY))
                        }

                        // Draw lines
                        drawPath(path = energyPath, color = NeonPurple, style = Stroke(width = 4f))
                        drawPath(path = bpmPath, color = NeonCyan, style = Stroke(width = 4f))
                    }
            ) {
                if (nodes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Dodaj i połącz utwory, aby zobaczyć oś czasu", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // New Flow dialog
    if (showNewFlowDialog) {
        var newFlowName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showNewFlowDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkSurfaceAlt)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Utwórz Nowy Set (Flow)", fontWeight = FontWeight.Bold, color = NeonCyan)
                    OutlinedTextField(
                        value = newFlowName,
                        onValueChange = { newFlowName = it },
                        label = { Text("Nazwa projektu") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showNewFlowDialog = false }) { Text("Anuluj", color = TextSecondary) }
                        Button(
                            onClick = {
                                if (newFlowName.isNotBlank()) {
                                    viewModel.createNewFlow(newFlowName)
                                    showNewFlowDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg)
                        ) {
                            Text("Utwórz")
                        }
                    }
                }
            }
        }
    }

    // Add node dialog (list of tracks to select)
    if (showAddNodeDialog) {
        Dialog(onDismissRequest = { showAddNodeDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkSurfaceAlt)
            ) {
                Column(modifier = Modifier.padding(16.dp).height(400.dp)) {
                    Text("Wybierz utwór do wstawienia na canvas", fontWeight = FontWeight.Bold, color = NeonCyan, modifier = Modifier.padding(bottom = 10.dp))
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allTracks) { track ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTrackToCanvas(track.id, x = 120f + (nodes.size * 20f), y = 150f + (nodes.size * 10f))
                                        showAddNodeDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceAlt)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(track.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text(track.artist, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Text(track.camelotKey, color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// 4. SET GENERATOR SCREEN (AUTO BUILD SET)
// ========================================================

@Composable
fun SetGeneratorScreen(viewModel: MixgraphViewModel) {
    val genres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val generatedSet by viewModel.generatedSet.collectAsStateWithLifecycle()

    val setGenre by viewModel.setGenre.collectAsStateWithLifecycle()
    val startBpm by viewModel.setBpmStart.collectAsStateWithLifecycle()
    val endBpm by viewModel.setBpmEnd.collectAsStateWithLifecycle()
    val targetEnergy by viewModel.setEnergyTarget.collectAsStateWithLifecycle()
    val length by viewModel.setLength.collectAsStateWithLifecycle()

    var savedSuccessfullyFlow by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Generator Setów AI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "System automatycznie dopasuje utwory z Twojej bazy, używając grafu kompatybilności Camelota, tempa i energii",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        // Settings Panel Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceAlt),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Parametry Setu", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonCyan)

                    // Genre Scroll
                    Column {
                        Text("Gatunek przewodni:", fontSize = 12.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            genres.forEach { genre ->
                                val isSelected = genre == setGenre
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) NeonCyan else DarkSurfaceAlt,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { viewModel.setGenre.value = genre }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        color = if (isSelected) DarkBg else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // BPM Start & End
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BPM Początkowe: $startBpm", fontSize = 12.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (startBpm > 80) viewModel.setBpmStart.value = startBpm - 1 }) { Text("-", color = NeonCyan) }
                                Text("$startBpm", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = TextPrimary)
                                IconButton(onClick = { if (startBpm < 160) viewModel.setBpmStart.value = startBpm + 1 }) { Text("+", color = NeonCyan) }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BPM Końcowe: $endBpm", fontSize = 12.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (endBpm > 80) viewModel.setBpmEnd.value = endBpm - 1 }) { Text("-", color = NeonCyan) }
                                Text("$endBpm", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = TextPrimary)
                                IconButton(onClick = { if (endBpm < 160) viewModel.setBpmEnd.value = endBpm + 1 }) { Text("+", color = NeonCyan) }
                            }
                        }
                    }

                    // Set length
                    Column {
                        Text("Liczba utworów w secie: $length", fontSize = 12.sp, color = TextSecondary)
                        Slider(
                            value = length.toFloat(),
                            onValueChange = { viewModel.setLength.value = it.toInt() },
                            valueRange = 3f..15f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // Energy level
                    Column {
                        Text("Docelowy poziom energii: $targetEnergy%", fontSize = 12.sp, color = TextSecondary)
                        Slider(
                            value = targetEnergy.toFloat(),
                            onValueChange = { viewModel.setEnergyTarget.value = it.toInt() },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                        )
                    }

                    Button(
                        onClick = { viewModel.generateSet() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("generate_set_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generuj Zgodny Set", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Results Section
        if (generatedSet.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Wygenerowana kolejność setu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Button(
                        onClick = {
                            val flowName = "Auto-Set ${setGenre} (${startBpm}-${endBpm} BPM)"
                            viewModel.saveGeneratedSetAsFlow(flowName)
                            savedSuccessfullyFlow = flowName
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Eksportuj do Flow", fontSize = 12.sp)
                    }
                }
            }

            items(generatedSet.size) { index ->
                val track = generatedSet[index]
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Track card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceAlt),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 14.sp,
                                modifier = Modifier.width(30.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                Text(track.artist, color = TextSecondary, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(track.camelotKey, color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("${track.bpm.toInt()} BPM", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Transition analysis draw (except last track)
                    if (index < generatedSet.size - 1) {
                        val nextTrack = generatedSet[index + 1]
                        val score = CompatibilityEngine.calculateCompatibility(track, nextTrack)
                        val explanation = CompatibilityEngine.getTransitionExplanation(track, nextTrack, score)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (score >= 80) NeonGreen else OrangeBoost,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Zgodność przejścia: $score%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (score >= 80) NeonGreen else OrangeBoost
                                )
                                Text(
                                    text = explanation,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// 5. LIVE MODE SCREEN
// ========================================================

@Composable
fun LiveModeScreen(viewModel: MixgraphViewModel) {
    val allTracks by viewModel.tracks.collectAsStateWithLifecycle()
    val activeTrack by viewModel.selectedLiveTrack.collectAsStateWithLifecycle()
    val recommendations by viewModel.liveRecommendations.collectAsStateWithLifecycle()

    var rotationAngle by remember { mutableStateOf(0f) }

    // Vinyl slow rotation animation
    LaunchedEffect(activeTrack) {
        if (activeTrack != null) {
            while (true) {
                withFrameNanos {
                    rotationAngle = (rotationAngle + 0.5f) % 360f
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Tryb Występu Live",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Błyskawiczne rekomendacje następnego tracku w czasie rzeczywistym (< 2ms)",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        // Active Deck (Now Playing)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(2.dp, NeonCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotating Vinyl representation
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .rotate(if (activeTrack != null) rotationAngle else 0f)
                            .background(Color.Black, CircleShape)
                            .border(3.dp, DarkSurfaceAlt, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Vinyl grooving layers
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color.DarkGray, radius = size.minDimension / 2.3f, style = Stroke(1f))
                            drawCircle(color = Color.DarkGray, radius = size.minDimension / 3f, style = Stroke(1f))
                        }
                        // Center Sticker
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(NeonCyan, CircleShape)
                                .border(1.dp, Color.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color.Black, CircleShape))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AKTYWNY UTWÓR (DECK A)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeTrack?.title ?: "Wybierz utwór z bazy poniżej...",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeTrack?.artist ?: "Nie gra żaden track",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (activeTrack != null) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "${activeTrack!!.bpm.toInt()} BPM",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeTrack!!.camelotKey,
                                    color = NeonPurple,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "E: ${activeTrack!!.energy}%",
                                    color = OrangeBoost,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeTrack == null) {
            item {
                Text(
                    text = "Kliknij utwór, aby włożyć na deck:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(allTracks.take(8)) { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectLiveTrack(track) },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(track.artist, color = TextSecondary, fontSize = 12.sp)
                        }
                        Text(track.camelotKey, color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        } else {
            // Live recommendations list based on active track
            item {
                Text(
                    text = "Najlepsze przejścia i dopasowania",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (recommendations.isEmpty()) {
                item {
                    Text("Brak odpowiednich rekomendacji w bazie.", color = TextSecondary, fontSize = 12.sp)
                }
            } else {
                items(recommendations) { rec ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectLiveTrack(rec.track) }, // Transition deck instantly!
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(
                            1.dp,
                            if (rec.score >= 85) NeonGreen.copy(alpha = 0.4f) else DarkSurfaceAlt
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Score indicator
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .background(
                                        if (rec.score >= 85) NeonGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rec.score}%",
                                    fontWeight = FontWeight.Bold,
                                    color = if (rec.score >= 85) NeonGreen else NeonCyan,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rec.track.title,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = rec.track.artist,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = rec.explanation,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = rec.track.camelotKey,
                                    color = NeonPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${rec.track.bpm.toInt()} BPM",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// 6. SMART DJ ASSISTANT SCREEN (AI CHAT)
// ========================================================

@Composable
fun AIAssistantScreen(viewModel: MixgraphViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    val chatListState = rememberLazyListState()

    // Auto scroll chat to bottom when message arrives
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            chatListState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Asystent DJ AI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Inteligentne sugestie na bazie Twojej biblioteki",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Wyczyść czat", tint = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset Prompt Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                "Zbuduj set Tech House",
                "Znajdź przejście harmoniczne z 8A",
                "Jak podbić energię o +2 kroki?"
            )
            presets.forEach { preset ->
                Box(
                    modifier = Modifier
                        .background(DarkSurfaceAlt, RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.chatInput.value = preset
                            viewModel.sendChatMessage()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(preset, color = TextPrimary, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat messages logs scroll
        LazyColumn(
            state = chatListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, DarkSurfaceAlt, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatHistory) { message ->
                val isAi = message.sender == "assistant"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAi) DarkSurfaceAlt else NeonCyan
                            ),
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isAi) 0.dp else 12.dp,
                                bottomEnd = if (isAi) 12.dp else 0.dp
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isAi) "SMART DJ ASSISTANT" else "TY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAi) NeonCyan else DarkBg,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = message.content,
                                    fontSize = 14.sp,
                                    color = if (isAi) TextPrimary else DarkBg
                                )
                            }
                        }
                    }
                }
            }

            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Asystent analizuje bibliotekę i generuje set...", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Text input field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { viewModel.chatInput.value = it },
                placeholder = { Text("Zadaj pytanie asystentowi...", color = TextMuted) },
                modifier = Modifier.weight(1f).testTag("chat_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurfaceAlt,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            IconButton(
                onClick = { viewModel.sendChatMessage() },
                modifier = Modifier
                    .background(NeonCyan, CircleShape)
                    .size(48.dp)
                    .testTag("send_chat_button"),
                enabled = !isChatLoading
            ) {
                Icon(Icons.Default.Send, contentDescription = "Wyślij", tint = DarkBg)
            }
        }
    }
}
