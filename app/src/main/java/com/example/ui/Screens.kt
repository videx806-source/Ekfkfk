package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

sealed interface AppScreen {
    object Home : AppScreen
    object Recordings : AppScreen
    object Config : AppScreen
    data class Player(val streamUrl: String, val title: String, val isLive: Boolean) : AppScreen
}

@Composable
fun MainContent(viewModel: VidexViewModel) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    val isRecording by viewModel.isCurrentlyRecording.collectAsStateWithLifecycle()
    val recordedBytes by viewModel.recordedBytes.collectAsStateWithLifecycle()

    BackHandler(enabled = currentScreen is AppScreen.Player) {
        currentScreen = AppScreen.Home
    }

    Scaffold(
        bottomBar = {
            if (currentScreen !is AppScreen.Player) {
                VidexBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen }
                )
            }
        },
        topBar = {
            if (currentScreen !is AppScreen.Player) {
                VidexTopAppHeader(isRecording, recordedBytes, onStopRecord = { viewModel.stopRecordingStream() })
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onPlayChannel = { channel ->
                        val playUrl = ChannelRepository.getStreamUrl(channel.path)
                        currentScreen = AppScreen.Player(
                            streamUrl = playUrl,
                            title = channel.name,
                            isLive = true
                        )
                    },
                    onPlayEvent = { event ->
                        currentScreen = AppScreen.Player(
                            streamUrl = event.streamUrl,
                            title = event.titulo,
                            isLive = event.estado == "en_vivo"
                        )
                    }
                )
                is AppScreen.Recordings -> RecordingsScreen(
                    viewModel = viewModel,
                    onPlayFile = { recording ->
                        currentScreen = AppScreen.Player(
                            streamUrl = "file://${recording.absolutePath}",
                            title = recording.name,
                            isLive = false
                        )
                    }
                )
                is AppScreen.Config -> ConfigScreen(viewModel = viewModel)
                is AppScreen.Player -> PlayerScreen(
                    streamUrl = screen.streamUrl,
                    title = screen.title,
                    isLive = screen.isLive,
                    viewModel = viewModel,
                    onBack = { currentScreen = AppScreen.Home }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VidexTopAppHeader(isRecording: Boolean, recordedBytes: Long, onStopRecord: () -> Unit) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "VIDEX",
                    color = AccentCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RecordingRed.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { onStopRecord() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(RecordingRed, shape = CircleShape)
                            )
                            Text(
                                text = "REC: ${formatBytes(recordedBytes)}",
                                color = RecordingRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary
        ),
        actions = {
            // Real physical Chromecast / Google Cast button from package androidx.mediarouter
            RealCastButton(
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 4.dp)
            )
            IconButton(
                onClick = {},
                modifier = Modifier.testTag("action_search_mock")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search placeholder",
                    tint = TextPrimary
                )
            }
        }
    )
}

@Composable
fun VidexBottomNavigation(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentScreen is AppScreen.Home,
            onClick = { onNavigate(AppScreen.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Canales") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SurfaceDark,
                selectedTextColor = AccentCyan,
                indicatorColor = AccentCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = currentScreen is AppScreen.Recordings,
            onClick = { onNavigate(AppScreen.Recordings) },
            icon = { Icon(Icons.Default.Videocam, contentDescription = "Grabadas") },
            label = { Text("Grabado") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SurfaceDark,
                selectedTextColor = AccentCyan,
                indicatorColor = AccentCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = currentScreen is AppScreen.Config,
            onClick = { onNavigate(AppScreen.Config) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
            label = { Text("Ajustes") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SurfaceDark,
                selectedTextColor = AccentCyan,
                indicatorColor = AccentCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}

@Composable
fun HomeScreen(
    viewModel: VidexViewModel,
    onPlayChannel: (Channel) -> Unit,
    onPlayEvent: (Evento) -> Unit
) {
    val events by viewModel.eventsState.collectAsStateWithLifecycle()
    val isLoadingEvents by viewModel.isLoadingEvents.collectAsStateWithLifecycle()
    val favorites by viewModel.favoritesState.collectAsStateWithLifecycle()

    var selectedGroup by remember { mutableStateOf("TODOS") }
    val filteredChannels = remember(selectedGroup, favorites) {
        if (selectedGroup == "FAVORITOS") {
            ChannelRepository.CHANNELS.filter { favorites.contains(it.name) }
        } else if (selectedGroup == "TODOS") {
            ChannelRepository.CHANNELS
        } else {
            ChannelRepository.CHANNELS.filter { it.group.equals(selectedGroup, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // --- 1. EVENTOS DEPORTIVOS ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Eventos Deportivos",
                    color = AccentCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                val activeEventsCount = events.count { it.estado == "en_vivo" }
                if (activeEventsCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RedLive)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$activeEventsCount EN VIVO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        item {
            if (isLoadingEvents) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay eventos activos",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(events, key = { it.id }) { event ->
                        EventCard(event = event, onPlay = onPlayEvent)
                    }
                }
            }
        }

        // --- 2. FILTRADO DE CANALES ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Canales en Vivo",
                color = AccentCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category Selector Chips including FAVORITOS
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                val groups = listOf("TODOS", "FAVORITOS", "GENERAL", "EVENTOS", "MÚSICA", "PLUTO")
                items(groups) { group ->
                    val isSelected = selectedGroup == group
                    val chipStrokeColor = when (group) {
                        "FAVORITOS" -> Color(0xFFFFD700)
                        "GENERAL" -> GroupGeneral
                        "EVENTOS" -> GroupEventos
                        "MÚSICA" -> GroupMusica
                        "PLUTO" -> GroupPluto
                        else -> AccentCyan
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGroup = group },
                        label = { Text(group) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = chipStrokeColor.copy(alpha = 0.15f),
                            labelColor = TextSecondary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = TextSecondary.copy(alpha = 0.4f),
                            selectedBorderColor = chipStrokeColor,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Grid listing filtered channels
        item {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredChannels.forEach { channel ->
                    ChannelChip(
                        channel = channel,
                        isFavorite = favorites.contains(channel.name),
                        onToggleFavorite = { viewModel.toggleFavorite(channel.name) },
                        onPlay = onPlayChannel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        maxItemsInEachRow = Int.MAX_VALUE,
        content = { content() }
    )
}

@Composable
fun EventCard(event: Evento, onPlay: (Evento) -> Unit) {
    val isLive = event.estado == "en_vivo"
    val isEnded = event.estado == "finalizado"

    Card(
        modifier = Modifier
            .width(170.dp)
            .height(240.dp)
            .testTag("event_card_${event.id}")
            .clickable(enabled = !isEnded) { onPlay(event) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = event.imagenUrl,
                    contentDescription = event.titulo,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Liga Badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = event.liga,
                        color = AccentCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Live Indicator badge
                if (isLive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(RedLive)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "● VIVO",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = event.titulo,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.equipos,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        minLines = 2,
                        lineHeight = 13.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Fecha",
                        tint = AccentCyan,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (isLive) "EN VIVO" else "${event.hora} · ${event.fecha.substringAfter("-")}",
                        color = if (isLive) RedLive else AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelChip(
    channel: Channel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPlay: (Channel) -> Unit
) {
    val strokeColor = when (channel.group.uppercase()) {
        "GENERAL" -> GroupGeneral
        "EVENTOS" -> GroupEventos
        "MÚSICA" -> GroupMusica
        "PLUTO" -> GroupPluto
        else -> AccentCyan
    }

    Surface(
        modifier = Modifier
            .testTag("channel_chip_${channel.name}"),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, strokeColor.copy(alpha = 0.7f)),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Toggle Favorite",
                tint = if (isFavorite) Color(0xFFFFD700) else TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onToggleFavorite() }
            )
            Row(
                modifier = Modifier.clickable { onPlay(channel) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(strokeColor, shape = CircleShape)
                )
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RecordingsScreen(viewModel: VidexViewModel, onPlayFile: (RecordingFile) -> Unit) {
    val recordings by viewModel.recordingsState.collectAsStateWithLifecycle()
    var deletingFile by remember { mutableStateOf<RecordingFile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshRecordings()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Mis Grabaciones locales",
            color = AccentCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "No recordings",
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Aún no tienes grabaciones",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Inicia reproducción y pulsa grabar",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordings, key = { it.absolutePath }) { file ->
                    RecordingItemRow(
                        file = file,
                        onPlay = { onPlayFile(file) },
                        onDelete = { deletingFile = file }
                    )
                }
            }
        }
    }

    deletingFile?.let { file ->
        AlertDialog(
            onDismissRequest = { deletingFile = null },
            title = { Text("Eliminar Grabación", color = Color.White) },
            text = { Text("¿Deseas borrar permanentemente el archivo ${file.name} de tu dispositivo?", color = TextPrimary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecording(file.absolutePath)
                        deletingFile = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RecordingRed)
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingFile = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceDarkVariant
        )
    }
}

@Composable
fun RecordingItemRow(
    file: RecordingFile,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(file.lastModified) {
        val sdf = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
        sdf.format(Date(file.lastModified))
    }

    val sizeString = remember(file.length) {
        formatBytes(file.length)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recording_${file.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant),
        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentCyan.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "Video",
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$dateString  ·  $sizeString",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RecordingRed),
                    border = BorderStroke(1.dp, RecordingRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Borrar", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reproducir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConfigScreen(viewModel: VidexViewModel) {
    val quality by viewModel.defaultQuality.collectAsStateWithLifecycle()
    val buffer by viewModel.networkBuffer.collectAsStateWithLifecycle()
    val autoPlay by viewModel.autoPlay.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ajustes de Reproducción",
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            SettingsSelectorCard(
                title = "Calidad de video por defecto",
                subtitle = "Resolución inicial de streams",
                currentValue = quality,
                options = listOf("Auto", "1080p", "720p", "480p", "360p"),
                onSelect = { viewModel.setQuality(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSelectorCard(
                title = "Búfer de red",
                subtitle = "Pre-carga de segmentos de video",
                currentValue = buffer,
                options = listOf("10s", "30s", "60s"),
                onSelect = { viewModel.setNetworkBuffer(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reproducción automática", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Iniciar streaming al entrar", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoPlay,
                        onCheckedChange = { viewModel.setAutoPlay(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan)
                    )
                }
            }
        }

        item {
            Text(
                text = "Almacenamiento",
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Carpeta de grabaciones", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("/VIDEX/Grabaciones", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val dir = File(context.getExternalFilesDir(null), "VIDEX/Grabaciones")
                            val files = dir.listFiles()
                            var deletedCount = 0
                            files?.forEach {
                                if (it.delete()) deletedCount++
                            }
                            viewModel.refreshRecordings()
                            Toast.makeText(context, "Grabaciones eliminadas: $deletedCount", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RecordingRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Borrar todas las grabaciones", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Acerca de VIDEX",
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Plataforma", color = TextSecondary, fontSize = 12.sp)
                        Text("VIDEX Streaming", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Versión", color = TextSecondary, fontSize = 12.sp)
                        Text("1.0.0", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sitio web", color = TextSecondary, fontSize = 12.sp)
                        Text("videx.lol", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSelectorCard(
    title: String,
    subtitle: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(subtitle, color = TextSecondary, fontSize = 11.sp)
                }
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(currentValue, color = AccentCyan, fontSize = 12.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentCyan)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = if (option == currentValue) AccentCyan else Color.White) },
                                onClick = {
                                    onSelect(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealCastButton(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            androidx.mediarouter.app.MediaRouteButton(ctx).apply {
                val selector = androidx.mediarouter.media.MediaRouteSelector.Builder()
                    .addControlCategory(androidx.mediarouter.media.MediaControlIntent.CATEGORY_LIVE_VIDEO)
                    .addControlCategory(androidx.mediarouter.media.MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .build()
                setRouteSelector(selector)
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    isLive: Boolean,
    viewModel: VidexViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isRecordingState by viewModel.isCurrentlyRecording.collectAsStateWithLifecycle()
    val recordedBytes by viewModel.recordedBytes.collectAsStateWithLifecycle()
    val autoPlay by viewModel.autoPlay.collectAsStateWithLifecycle()

    val favorites by viewModel.favoritesState.collectAsStateWithLifecycle()
    val isFavorite = favorites.contains(title)

    var isFullscreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    val formattedRecordedBytes = remember(recordedBytes) { formatBytes(recordedBytes) }

    val view = androidx.compose.ui.platform.LocalView.current
    val window = remember(view) { (context as? android.app.Activity)?.window }
    val activity = context as? android.app.Activity

    LaunchedEffect(isFullscreen) {
        window?.let { win ->
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(win, view)
            if (isFullscreen) {
                windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    // Initialize LibVLC and VLC MediaPlayer
    val libVlc = remember {
        val options = ArrayList<String>()
        options.add("-vvv")
        options.add("--http-reconnect")
        options.add("--network-caching=2500")
        org.videolan.libvlc.LibVLC(context, options)
    }

    val vlcMediaPlayer = remember(libVlc) {
        org.videolan.libvlc.MediaPlayer(libVlc)
    }

    val videoLayout = remember {
        org.videolan.libvlc.util.VLCVideoLayout(context)
    }

    var isBufferLoading by remember { mutableStateOf(true) }

    // Set speed on vlcMediaPlayer when slider state shifts
    LaunchedEffect(playbackSpeed) {
        try {
            vlcMediaPlayer.rate = playbackSpeed
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(vlcMediaPlayer) {
        vlcMediaPlayer.setEventListener { event ->
            when (event.type) {
                org.videolan.libvlc.MediaPlayer.Event.Buffering -> {
                    isBufferLoading = event.buffering < 100f
                }
                org.videolan.libvlc.MediaPlayer.Event.Playing -> {
                    isBufferLoading = false
                }
                org.videolan.libvlc.MediaPlayer.Event.EncounteredError -> {
                    isBufferLoading = false
                }
            }
        }

        onDispose {
            try {
                vlcMediaPlayer.stop()
                vlcMediaPlayer.detachViews()
                vlcMediaPlayer.release()
                libVlc.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(streamUrl) {
        try {
            isBufferLoading = true
            vlcMediaPlayer.stop()
            vlcMediaPlayer.detachViews()

            vlcMediaPlayer.attachViews(videoLayout, null, true, false)

            val uri = Uri.parse(streamUrl)
            val media = org.videolan.libvlc.Media(libVlc, uri)
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=2500")
            media.addOption(":clock-jitter=0")
            media.addOption(":clock-synchro=0")

            vlcMediaPlayer.media = media
            media.release()

            vlcMediaPlayer.rate = playbackSpeed

            if (autoPlay) {
                vlcMediaPlayer.play()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            isBufferLoading = false
            Toast.makeText(context, "No se pudo reproducir este stream", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isFullscreen) {
            // --- 1. Custom Player Toolbar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("player_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Regular back navigation",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isLive) "Streaming en Vivo" else "Grabación local",
                        color = if (isLive) RedLive else AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Favorites button directly inside player screen toolbar
                IconButton(onClick = { viewModel.toggleFavorite(title) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) Color(0xFFFFD700) else Color.White
                    )
                }
                // Native Cast connection in the toolbar
                RealCastButton(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 4.dp)
                )
            }
        }

        // --- 2. 16:9 Video Canvas (NATIVE VLC HOST) ---
        Box(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            }.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    videoLayout
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isBufferLoading) {
                CircularProgressIndicator(color = AccentCyan)
            }

            // Floating Toggle Screen Overlay - Cinematic view switcher
            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Pantalla completa",
                    tint = Color.White
                )
            }
        }

        if (!isFullscreen) {
            // --- 3. Stream Info & Live Status ---
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isLive) RedLive else AccentCyan, shape = CircleShape)
                        )
                        Text(
                            text = if (isLive) "VIVO" else "OFFLINE",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (isLive) {
                        LiveStreamTimeIndicator()
                    }
                }
            }

            Divider(color = TextSecondary.copy(alpha = 0.2f), thickness = 1.dp)

            // --- 4. Controls Action Area ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDark)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Acciones de Reproducción",
                    color = AccentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Full Cast device option opening system selection directly
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .background(SurfaceDarkVariant, shape = RoundedCornerShape(10.dp))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            RealCastButton(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Transmitir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Record local button
                    Button(
                        onClick = {
                            if (isRecordingState) {
                                viewModel.stopRecordingStream()
                                Toast.makeText(context, "Grabación guardada en Mis Grabaciones", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.startRecordingStream(streamUrl, title)
                                Toast.makeText(context, "Grabación iniciada...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecordingState) RecordingRed.copy(alpha = 0.2f) else SurfaceDarkVariant,
                            contentColor = if (isRecordingState) RecordingRed else Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonChecked,
                                contentDescription = "Record",
                                tint = if (isRecordingState) RecordingRed else TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRecordingState) "REC..." else "Grabar Stream",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Ajustar velocidad del player: ${playbackSpeed}x",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = playbackSpeed,
                            onValueChange = { playbackSpeed = it },
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentCyan,
                                activeTrackColor = AccentCyan,
                                inactiveTrackColor = TextSecondary.copy(alpha = 0.3f)
                              )
                          )
                      }
                  }
              }
          }
      }
  }

@Composable
fun LiveStreamTimeIndicator() {
    var currentTimeString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            currentTimeString = sdf.format(Date())
            delay(1000)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Hora actual",
            tint = TextSecondary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = currentTimeString,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes.toFloat() / 1024
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024
    return String.format(Locale.US, "%.1f MB", mb)
}
