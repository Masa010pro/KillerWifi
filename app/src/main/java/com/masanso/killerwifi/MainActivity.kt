package com.masanso.killerwifi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masanso.killerwifi.ui.theme.KillerWifiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.URL
import java.util.UUID
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }
        setContent { KillerWifiApp() }
    }
}

private enum class ThemeMode(val title: String, val icon: String) {
    System("System", "⚙"),
    Light("Light", "☼"),
    Dark("Dark", "☾")
}

private data class WifiSnapshot(
    val ssid: String = "Not connected",
    val bssid: String = "Unavailable",
    val rssi: Int = -100,
    val signal: Int = 0,
    val frequency: Int = 0,
    val ipAddress: String = "Unavailable",
    val network: String = "Unavailable",
    val usedIps: Int = 0,
    val freeIps: Int = 0,
    val history: List<Int> = List(24) { 0 }
)

private data class SpeedResult(
    val latency: Long? = null,
    val download: Double? = null,
    val upload: Double? = null
)

private data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String
)

@Composable
private fun KillerWifiApp() {
    var themeMode by remember { mutableStateOf(ThemeMode.System) }
    val dark = when (themeMode) {
        ThemeMode.System -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    KillerWifiTheme(darkTheme = dark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            WifiDashboard(onThemeChange = { themeMode = it }, themeMode = themeMode)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiDashboard(onThemeChange: (ThemeMode) -> Unit, themeMode: ThemeMode) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(WifiSnapshot()) }
    var speed by remember { mutableStateOf(SpeedResult()) }
    var testing by remember { mutableStateOf(false) }
    var themeMenu by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            snapshot = readWifiSnapshot(context, snapshot)
            delay(3.seconds)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Killer WiFi", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                LiveBadge()
                            }
                            Text("Network Intelligence", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { themeMenu = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Text(themeMode.icon, fontSize = 18.sp)
                        }
                        DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                            ThemeMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(mode.icon, fontSize = 16.sp)
                                            Spacer(Modifier.width(10.dp))
                                            Text(mode.title, fontWeight = if (mode == themeMode) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    onClick = {
                                        onThemeChange(mode)
                                        themeMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAiSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Ask AI", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectionCard(snapshot)
            NetworkDetailsCard(snapshot)
            IpCapacityCard(snapshot)
            SpeedTestCard(
                result = speed,
                testing = testing,
                onTest = {
                    testing = true
                    speed = SpeedResult()
                    scope.launch {
                        speed = measureSpeed()
                        testing = false
                    }
                }
            )
            Spacer(Modifier.height(80.dp))
        }

        if (showAiSheet) {
            AiAssistantBottomSheet(
                snapshot = snapshot,
                speed = speed,
                chatMessages = chatMessages,
                onMessagesUpdated = { chatMessages = it },
                onDismiss = { showAiSheet = false }
            )
        }
    }
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseLive")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .alpha(alpha)
                .background(MaterialTheme.colorScheme.secondary)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "LIVE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            letterSpacing = 0.8.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiAssistantBottomSheet(
    snapshot: WifiSnapshot,
    speed: SpeedResult,
    chatMessages: List<ChatMessage>,
    onMessagesUpdated: (List<ChatMessage>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var queryText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    fun sendPrompt(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank()) return

        // Immediately clear the input bar as requested by user
        queryText = ""
        keyboardController?.hide()

        val updatedWithUser = chatMessages + ChatMessage(isUser = true, text = trimmed)
        onMessagesUpdated(updatedWithUser)
        isThinking = true

        scope.launch {
            delay(400.milliseconds)
            val responseText = generateAiResponse(trimmed, updatedWithUser, snapshot, speed)
            onMessagesUpdated(updatedWithUser + ChatMessage(isUser = false, text = responseText))
            isThinking = false
            listState.animateScrollToItem((updatedWithUser.size).coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with clear button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "AI NETWORK ASSISTANT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Conversational Telemetry AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (chatMessages.isNotEmpty()) {
                    IconButton(
                        onClick = { onMessagesUpdated(emptyList()) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text("🗑️", fontSize = 14.sp)
                    }
                }
            }

            // Quick suggestion chips
            FlowRowSuggestionChips(onChipClick = { prompt -> sendPrompt(prompt) })

            // Chat Messages Thread
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(10.dp)
            ) {
                if (chatMessages.isEmpty() && !isThinking) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("👋 Ask Killer AI anything about your Wi-Fi network!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap a quick suggestion above or type below.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatMessages, key = { it.id }) { msg ->
                            ChatBubble(msg = msg, onCopy = { copyToClipboard(context, "AI Response", msg.text) })
                        }
                        if (isThinking) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("AI is reasoning...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("Ask follow-up or any question...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendPrompt(queryText) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                Button(
                    onClick = { sendPrompt(queryText) },
                    enabled = queryText.isNotBlank() && !isThinking,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Ask", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowSuggestionChips(onChipClick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            "✨ Summarize status",
            "🎮 Gaming check",
            "📺 4K stream info",
            "📡 Wi-Fi band details"
        ).forEach { chipText ->
            SuggestionChip(
                onClick = {
                    val prompt = chipText.removePrefix("✨ ").removePrefix("🎮 ").removePrefix("📺 ").removePrefix("📡 ")
                    onChipClick(prompt)
                },
                label = { Text(chipText, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage, onCopy: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (msg.isUser) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp)
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("Killer AI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onCopy, modifier = Modifier.size(22.dp)) {
                            Text("📋", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

private fun generateAiResponse(
    query: String,
    history: List<ChatMessage>,
    snapshot: WifiSnapshot,
    speed: SpeedResult
): String {
    val q = query.lowercase().trim()
    val band = getBandLabel(snapshot.frequency)
    val quality = getSignalQuality(snapshot.signal)
    val latencyText = speed.latency?.let { "$it ms" } ?: "not benchmarked yet"
    val downloadText = speed.download?.let { "%.1f Mbps".format(it) } ?: "not benchmarked yet"
    val uploadText = speed.upload?.let { "%.1f Mbps".format(it) } ?: "not benchmarked yet"

    val isConnected = snapshot.ssid != "Not connected"

    if (!isConnected) {
        return "You are currently not connected to any Wi-Fi network. Please turn on Wi-Fi on your device and connect to a wireless network to receive active telemetry analysis and intelligence."
    }

    // Inspect conversation context from history
    val previousUserPrompts = history.filter { it.isUser }.map { it.text.lowercase() }
    val lastPrompt = previousUserPrompts.dropLast(1).lastOrNull() ?: ""

    return when {
        q.contains("summarize") || q.contains("summary") || q.contains("status") || q.contains("health") || q.contains("check") -> {
            val signalAssessment = when {
                snapshot.signal >= 80 -> "You have an excellent Wi-Fi connection with a high signal strength of ${snapshot.signal}% (${snapshot.rssi} dBm)."
                snapshot.signal >= 50 -> "Your Wi-Fi signal is fair at ${snapshot.signal}% (${snapshot.rssi} dBm), which is sufficient for standard browsing."
                else -> "Your Wi-Fi signal is weak at ${snapshot.signal}% (${snapshot.rssi} dBm). You may experience latency spikes or reduced bandwidth."
            }
            val speedAssessment = if (speed.download != null) {
                " Your speed benchmark shows a download throughput of $downloadText, an upload speed of $uploadText, and a latency of $latencyText."
            } else {
                " Internet performance has not been measured yet; tap 'Run Test' on the benchmark card to measure real-time throughput."
            }
            val bandAssessment = if (snapshot.frequency > 0) " You are operating on the $band frequency band." else ""

            "Network Summary for \"${snapshot.ssid}\":\n\n$signalAssessment$bandAssessment$speedAssessment Your device is assigned IP address ${snapshot.ipAddress} on subnet ${snapshot.network}."
        }

        q.contains("game") || q.contains("gaming") || q.contains("ping") || q.contains("latency") ||
        (q.contains("fast") && lastPrompt.contains("game")) -> {
            val ping = speed.latency
            when {
                ping == null -> "To accurately evaluate online gaming performance, please run the speed benchmark test first so latency can be measured."
                ping < 30 -> "Your connection latency is exceptional at $ping ms. This provides an ideal competitive online gaming experience with minimal response delay."
                ping < 70 -> "Your latency is moderate at $ping ms. Online gaming will generally perform smoothly, though minor delays may occasionally happen in fast-paced games."
                else -> "Your latency is high at $ping ms. You may experience noticeable lag or delay during online multiplayer sessions."
            }
        }

        q.contains("4k") || q.contains("stream") || q.contains("video") || q.contains("youtube") || q.contains("netflix") ||
        (q.contains("movie") || q.contains("buffering")) -> {
            val dl = speed.download
            when {
                dl == null -> "Please run the speed benchmark test first to evaluate your download capacity for video streaming."
                dl >= 25.0 -> "Your current download speed of $downloadText easily supports seamless 4K Ultra HD video streaming across multiple devices simultaneously without buffering."
                dl >= 10.0 -> "Your download speed of $downloadText is sufficient for high-definition 1080p video streaming. However, 4K streaming may require occasional buffering."
                else -> "Your download speed of $downloadText is relatively low and may cause buffering during high-definition video playback."
            }
        }

        q.contains("ip") || q.contains("mac") || q.contains("bssid") || q.contains("subnet") || q.contains("capacity") || q.contains("address") -> {
            "Network Addressing Breakdown:\n\n" +
            "• Connected SSID: ${snapshot.ssid}\n" +
            "• Device IP Address: ${snapshot.ipAddress}\n" +
            "• Subnet Network: ${snapshot.network}\n" +
            "• Router MAC (BSSID): ${snapshot.bssid}\n" +
            "• Estimated IP Capacity: ${snapshot.usedIps + snapshot.freeIps} total addresses (${snapshot.freeIps} available)."
        }

        q.contains("band") || q.contains("5g") || q.contains("2.4") || q.contains("6g") || q.contains("frequency") ||
        (q.contains("why") && lastPrompt.contains("band")) -> {
            when {
                snapshot.frequency in 4900..5900 -> "You are connected on the 5 GHz band ($band). This frequency offers high data rates and less wireless interference, though it has slightly shorter physical range compared to 2.4 GHz."
                snapshot.frequency in 5925..7125 -> "You are connected on the 6 GHz band ($band). This band provides maximum wireless bandwidth with virtually zero channel congestion."
                snapshot.frequency in 2400..2500 -> "You are connected on the 2.4 GHz band ($band). This frequency provides greater range through walls, but can experience interference from nearby household devices."
                else -> "Your Wi-Fi frequency band is currently $band."
            }
        }

        // Context-aware follow-ups
        q.contains("why") || q.contains("how come") || q.contains("explain") || q.contains("is that good") -> {
            val speedInfo = if (speed.download != null) "Download is $downloadText with $latencyText latency." else "Speed benchmark is currently pending."
            "Regarding your conversation about \"${snapshot.ssid}\":\n\n" +
            "Your Wi-Fi signal quality is $quality at ${snapshot.signal}% (${snapshot.rssi} dBm) on the $band band. $speedInfo\n\n" +
            "If you'd like tips to improve your connection, moving closer to your router or switching to a less congested band (like 5 GHz) is highly recommended."
        }

        else -> {
            val speedInfo = if (speed.download != null) "Download speed is $downloadText with a latency of $latencyText." else "Run the benchmark test to measure throughput."
            "Here is the active network intelligence for \"${snapshot.ssid}\":\n\n" +
            "• Signal Quality: $quality (${snapshot.signal}%, ${snapshot.rssi} dBm)\n" +
            "• Frequency Band: $band\n" +
            "• Device IP: ${snapshot.ipAddress}\n" +
            "• Performance: $speedInfo\n\n" +
            "Feel free to ask any follow-up question!"
        }
    }
}

@Composable
private fun ConnectionCard(snapshot: WifiSnapshot) {
    val quality = getSignalQuality(snapshot.signal)
    val band = getBandLabel(snapshot.frequency)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(28.dp)
            )
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "CONNECTED NETWORK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        snapshot.ssid,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (snapshot.frequency > 0) "$band  •  ${snapshot.network}" else "Waiting for Wi-Fi connection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.width(12.dp))
                SignalGauge(snapshot.signal, quality)
            }

            SignalChart(snapshot.history)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Signal history",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    "${snapshot.rssi} dBm",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SignalGauge(percent: Int, quality: String) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)

    Box(
        modifier = Modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 7.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topParam = strokeWidth / 2

            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(topParam, topParam),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = primary,
                startAngle = 135f,
                sweepAngle = 270f * (percent.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = Offset(topParam, topParam),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = quality,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SignalChart(history: List<Int>) {
    val line = MaterialTheme.colorScheme.primary
    val fillGradient = Brush.verticalGradient(
        colors = listOf(line.copy(alpha = 0.35f), Color.Transparent)
    )
    val grid = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)

    val pulseAnim = rememberInfiniteTransition(label = "pulseChart")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(Modifier.fillMaxWidth().height(85.dp)) {
        repeat(3) { index ->
            val y = size.height * (index + 1) / 4
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        if (history.any { it > 0 }) {
            val strokePath = Path()
            val fillPath = Path()

            var lastX = 0f
            var lastY = size.height

            history.forEachIndexed { index, value ->
                val x = size.width * index / (history.lastIndex.coerceAtLeast(1))
                val y = size.height - (value.coerceIn(0, 100) / 100f * (size.height - 12.dp.toPx())) - 6.dp.toPx()
                if (index == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    val controlX1 = (lastX + x) / 2
                    strokePath.cubicTo(controlX1, lastY, controlX1, y, x, y)
                    fillPath.cubicTo(controlX1, lastY, controlX1, y, x, y)
                }
                lastX = x
                lastY = y
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(fillPath, brush = fillGradient)
            drawPath(strokePath, color = line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            drawCircle(
                color = line.copy(alpha = 0.4f * pulseAlpha),
                radius = 8.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = line,
                radius = 4.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}

@Composable
private fun NetworkDetailsCard(snapshot: WifiSnapshot) {
    val context = LocalContext.current
    val band = getBandLabel(snapshot.frequency)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ACCESS POINT DETAILS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Tap detail to copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            DetailRow("Router MAC (BSSID)", snapshot.bssid) { copyToClipboard(context, "BSSID", snapshot.bssid) }
            DetailRow("Device IP", snapshot.ipAddress) { copyToClipboard(context, "IP Address", snapshot.ipAddress) }
            DetailRow("Subnet / Network", snapshot.network) { copyToClipboard(context, "Network", snapshot.network) }
            DetailRow("Frequency Band", band)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    if (value.isBlank() || value == "Unavailable") return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, value)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

@Composable
private fun IpCapacityCard(snapshot: WifiSnapshot) {
    val totalIps = (snapshot.usedIps + snapshot.freeIps).coerceAtLeast(1)
    val usedRatio = (snapshot.usedIps.toFloat() / totalIps).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SUBNET IP CAPACITY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("Subnet estimate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { usedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Allocated: ${snapshot.usedIps}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Total Available: ${snapshot.freeIps + snapshot.usedIps}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill(Modifier.weight(1f), "Used IPs", snapshot.usedIps.toString(), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                StatPill(Modifier.weight(1f), "Free IPs", snapshot.freeIps.toString(), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(
                "Estimated using current interface prefix. Full network scan requires router admin permissions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun StatPill(modifier: Modifier, label: String, value: String, containerColor: Color, contentColor: Color) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(14.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = contentColor)
        Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.8f))
    }
}

@Composable
private fun SpeedTestCard(result: SpeedResult, testing: Boolean, onTest: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "INTERNET SPEED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("Connection Benchmark", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onTest,
                    enabled = !testing,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Run Test", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Metric("Latency", result.latency?.let { "$it ms" } ?: "--", "⚡")
                Metric("Download", result.download?.let { "%.1f Mbps".format(it) } ?: "--", "↓")
                Metric("Upload", result.upload?.let { "%.1f Mbps".format(it) } ?: "--", "↑")
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, symbol: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun getBandLabel(frequency: Int): String {
    return when (frequency) {
        in 2400..2500 -> "2.4 GHz"
        in 4900..5900 -> "5 GHz"
        in 5925..7125 -> "6 GHz"
        else -> if (frequency > 0) "$frequency MHz" else "Unknown"
    }
}

private fun getSignalQuality(signal: Int): String {
    return when {
        signal >= 80 -> "Excellent"
        signal >= 60 -> "Good"
        signal >= 40 -> "Fair"
        signal > 0 -> "Weak"
        else -> "None"
    }
}

@Suppress("DEPRECATION")
private fun readWifiSnapshot(context: Context, previous: WifiSnapshot): WifiSnapshot {
    val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val info = wifi?.connectionInfo
    val link = connectivity?.getLinkProperties(connectivity.activeNetwork)
    val address = link?.linkAddresses?.firstOrNull { it.address is Inet4Address }
    val prefix = address?.prefixLength ?: 24
    val capacity = 2.0.pow((32 - prefix).toDouble()).roundToInt().coerceAtLeast(2) - 2
    val ip = address?.address?.hostAddress ?: "Unavailable"
    val percent = if (info != null && info.rssi > -100) ((info.rssi + 100) * 100 / 60).coerceIn(0, 100) else 0
    val history = (previous.history.drop(1) + percent).takeLast(24)
    return WifiSnapshot(
        ssid = info?.ssid?.removePrefix("\"")?.removeSuffix("\"")?.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: "Not connected",
        bssid = info?.bssid?.takeIf { it != "02:00:00:00:00:00" } ?: "Unavailable",
        rssi = info?.rssi?.takeIf { it > -100 } ?: -100,
        signal = percent,
        frequency = info?.frequency ?: 0,
        ipAddress = ip,
        network = if (address != null) "$ip/$prefix" else "Unavailable",
        usedIps = if (address != null) 1 else 0,
        freeIps = if (address != null) capacity - 1 else 0,
        history = history
    )
}

private suspend fun measureSpeed(): SpeedResult = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val pingStart = System.currentTimeMillis()
        connection = URL("https://www.google.com/generate_204").openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.requestMethod = "GET"
        connection.connect()
        val latency = System.currentTimeMillis() - pingStart
        connection.disconnect()

        val bytes = 1_000_000L
        val downloadStart = System.nanoTime()
        connection = URL("https://speed.cloudflare.com/__down?bytes=$bytes").openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 12_000
        val downloaded = connection.inputStream.use { input ->
            val buffer = ByteArray(16_384)
            var total = 0L
            var read = input.read(buffer)
            while (read >= 0 && total < bytes) {
                total += read
                read = input.read(buffer)
            }
            total
        }
        val downloadMbps = downloaded * 8.0 / ((System.nanoTime() - downloadStart) / 1_000_000_000.0) / 1_000_000.0
        connection.disconnect()
        val uploadBytes = ByteArray(250_000)
        val uploadStart = System.nanoTime()
        connection = URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 12_000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/octet-stream")
        connection.outputStream.use { it.write(uploadBytes) }
        connection.inputStream.use { it.readBytes() }
        val uploadMbps = uploadBytes.size * 8.0 / ((System.nanoTime() - uploadStart) / 1_000_000_000.0) / 1_000_000.0
        SpeedResult(latency, downloadMbps, uploadMbps)
    } catch (_: IOException) {
        connection?.disconnect()
        SpeedResult()
    }
}
