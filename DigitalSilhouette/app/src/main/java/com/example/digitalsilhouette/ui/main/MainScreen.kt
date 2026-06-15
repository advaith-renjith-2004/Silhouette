package com.example.digitalsilhouette.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.digitalsilhouette.data.DefaultDataRepository
import com.example.digitalsilhouette.data.FocusSession
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

import com.example.digitalsilhouette.theme.FocusTheme
import com.example.digitalsilhouette.theme.ThemePresets

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = run {
    val context = LocalContext.current.applicationContext
    viewModel { MainScreenViewModel(DefaultDataRepository.getInstance(context)) }
  },
) {
  val context = LocalContext.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  // Track lifecycle to recheck permissions on resume
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.checkPermissions(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Check initial permissions
  LaunchedEffect(Unit) {
    viewModel.checkPermissions(context)
  }

  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    when (state) {
      MainScreenUiState.Loading -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F)),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = Color(0xFF00E5FF))
        }
      }
      is MainScreenUiState.Success -> {
        val successState = state as MainScreenUiState.Success
        val currentTheme = when (successState.selectedTheme) {
          "Cyberpunk" -> ThemePresets.Cyberpunk
          "Forest Oasis" -> ThemePresets.ForestOasis
          "Obsidian" -> ThemePresets.Obsidian
          "Snow Drift" -> ThemePresets.SnowDrift
          else -> ThemePresets.AetherNeon
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background)
        ) {
          MainScreenContent(
            successState = successState,
            viewModel = viewModel,
            theme = currentTheme,
            modifier = modifier.fillMaxSize()
          )
        }
      }
      is MainScreenUiState.Error -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Error: ${(state as MainScreenUiState.Error).throwable.localizedMessage}",
            color = Color.Red,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
internal fun MainScreenContent(
  successState: MainScreenUiState.Success,
  viewModel: MainScreenViewModel,
  theme: FocusTheme,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Permission Launchers
  val requestPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    viewModel.checkPermissions(context)
  }

  // Format Elapsed Time
  var elapsedSeconds by remember { mutableStateOf(0L) }
  LaunchedEffect(successState.isFocusActive, successState.currentSessionStartTime) {
    if (successState.isFocusActive && successState.currentSessionStartTime != null) {
      while (true) {
        elapsedSeconds = (System.currentTimeMillis() - successState.currentSessionStartTime) / 1000L
        delay(1000L)
      }
    } else {
      elapsedSeconds = 0L
    }
  }

  Column(
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "DIGITAL SILHOUETTE",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
          color = theme.textPrimary,
          letterSpacing = 2.sp
        )
        Text(
          text = "Welcome back, ${successState.userName}! (${successState.userEmail})",
          fontSize = 11.sp,
          color = theme.textSecondary,
          letterSpacing = 0.5.sp
        )
      }
      IconButton(
        onClick = { viewModel.logoutUser() },
        modifier = Modifier.size(36.dp)
      ) {
        Text("🚪", fontSize = 20.sp)
      }
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Theme Selector Row
    ThemeSelectorRow(
      selectedTheme = successState.selectedTheme,
      onThemeSelected = { viewModel.changeTheme(it) },
      theme = theme
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Permission Alerts Block
    if (!successState.hasNotificationPermission || !successState.hasDndPermission) {
      PermissionAlertCard(
        hasNotification = successState.hasNotificationPermission,
        hasDnd = successState.hasDndPermission,
        onRequestNotification = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
          }
        },
        onRequestDnd = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            context.startActivity(intent)
          }
        }
      )
      Spacer(modifier = Modifier.height(12.dp))
    }

    // Main Silhouette Shield Card with custom animations
    SilhouetteShield(
      isServiceRunning = successState.isServiceRunning,
      isFocusActive = successState.isFocusActive,
      elapsedSeconds = elapsedSeconds,
      onToggleService = { viewModel.toggleService(context) },
      theme = theme
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Stats Section
    StatsRow(sessions = successState.completedSessions, theme = theme)
    Spacer(modifier = Modifier.height(12.dp))

    // Live Logs Terminal & History Tab Split
    var activeTab by remember { mutableStateOf("logs") }
    TabSelector(activeTab = activeTab, onTabSelected = { activeTab = it }, theme = theme)
    Spacer(modifier = Modifier.height(8.dp))

    if (activeTab == "logs") {
      LogTerminal(
        logs = successState.sensorLogs,
        onClearLogs = { viewModel.clearLogs() },
        theme = theme,
        modifier = Modifier.weight(1f)
      )
    } else {
      HistoryList(
        sessions = successState.completedSessions,
        onClearHistory = { viewModel.clearHistory() },
        theme = theme,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
fun ThemeSelectorRow(
  selectedTheme: String,
  onThemeSelected: (String) -> Unit,
  theme: FocusTheme
) {
  val themes = listOf("Aether Neon", "Cyberpunk", "Forest Oasis", "Obsidian", "Snow Drift")
  LazyRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    items(themes) { name ->
      val isSelected = selectedTheme == name
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(if (isSelected) theme.accent.copy(alpha = 0.15f) else Color.Transparent)
          .border(
            width = 1.dp,
            color = if (isSelected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
          )
          .clickable { onThemeSelected(name) }
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Text(
          text = name,
          fontSize = 11.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) theme.accent else theme.textSecondary
        )
      }
    }
  }
}

@Composable
fun TabSelector(
  activeTab: String,
  onTabSelected: (String) -> Unit,
  theme: FocusTheme
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(theme.textSecondary.copy(alpha = 0.1f))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(8.dp))
        .background(if (activeTab == "logs") theme.accent.copy(alpha = 0.1f) else Color.Transparent)
        .border(
          width = if (activeTab == "logs") 1.dp else 0.dp,
          color = if (activeTab == "logs") theme.accent.copy(alpha = 0.4f) else Color.Transparent,
          shape = RoundedCornerShape(8.dp)
        )
        .clickable { onTabSelected("logs") }
        .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "LIVE SENSOR LOGS",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (activeTab == "logs") theme.accent else theme.textSecondary,
        letterSpacing = 1.sp
      )
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(8.dp))
        .background(if (activeTab == "history") theme.accent.copy(alpha = 0.1f) else Color.Transparent)
        .border(
          width = if (activeTab == "history") 1.dp else 0.dp,
          color = if (activeTab == "history") theme.accent.copy(alpha = 0.4f) else Color.Transparent,
          shape = RoundedCornerShape(8.dp)
        )
        .clickable { onTabSelected("history") }
        .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "SESSION HISTORY",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (activeTab == "history") theme.accent else theme.textSecondary,
        letterSpacing = 1.sp
      )
    }
  }
}

@Composable
fun SilhouetteShield(
  isServiceRunning: Boolean,
  isFocusActive: Boolean,
  elapsedSeconds: Long,
  onToggleService: () -> Unit,
  theme: FocusTheme
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  // Spinning radar angle
  val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  val cardBackground = if (isFocusActive) {
    Brush.verticalGradient(listOf(theme.accent.copy(alpha = 0.08f), Color.Transparent))
  } else {
    Brush.verticalGradient(listOf(Color(0x05FFFFFF), Color.Transparent))
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, theme.border),
    colors = CardDefaults.cardColors(containerColor = theme.cardBg)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(cardBackground)
        .padding(20.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Shield Pulse Circle
        Box(
          modifier = Modifier.size(160.dp),
          contentAlignment = Alignment.Center
        ) {
          if (isFocusActive) {
            // Pulse Ring
            Box(
              modifier = Modifier
                .fillMaxSize(pulseScale)
                .clip(CircleShape)
                .background(theme.accent.copy(alpha = 0.06f))
                .border(BorderStroke(2.dp, theme.accent.copy(alpha = 0.15f)), CircleShape)
            )
            // Animated Canvas Radar Sweep
            Canvas(modifier = Modifier.size(140.dp)) {
              val center = this.center
              val radius = size.minDimension / 2f

              // Draw radar boundary circle
              drawCircle(
                color = theme.accent.copy(alpha = 0.1f),
                radius = radius,
                style = Stroke(width = 1.dp.toPx())
              )

              // Draw sweeping line
              val angleRad = Math.toRadians(rotationAngle.toDouble())
              val lineEnd = Offset(
                x = center.x + radius * Math.cos(angleRad).toFloat(),
                y = center.y + radius * Math.sin(angleRad).toFloat()
              )
              drawLine(
                color = theme.accent.copy(alpha = 0.8f),
                start = center,
                end = lineEnd,
                strokeWidth = 2.dp.toPx()
              )

              // Draw radar sweep tail
              drawArc(
                color = theme.accent.copy(alpha = 0.08f),
                startAngle = rotationAngle - 60f,
                sweepAngle = 60f,
                useCenter = true,
                size = Size(radius * 2f, radius * 2f),
                topLeft = Offset(center.x - radius, center.y - radius)
              )
            }
          } else if (isServiceRunning) {
            Box(
              modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .border(BorderStroke(1.dp, theme.accent.copy(alpha = 0.15f)), CircleShape)
            )
          }

          // Inner Circle Display
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
              .size(110.dp)
              .clip(CircleShape)
              .background(theme.background)
              .border(BorderStroke(2.dp, theme.accent), CircleShape)
          ) {
            if (isFocusActive) {
              Text(
                text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
                color = theme.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = "FOCUSED",
                color = theme.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
              )
            } else {
              Text(
                text = if (isServiceRunning) "IDLE" else "OFF",
                color = if (isServiceRunning) theme.accent else theme.textSecondary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // State Text
        Text(
          text = when {
            isFocusActive -> "Deep Work Active"
            isServiceRunning -> "Listening. Flip face-down on a desk to focus."
            else -> "Digital Silhouette is Disabled"
          },
          color = theme.textPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        )
        Text(
          text = when {
            isFocusActive -> "Muting incoming calls and notifications."
            isServiceRunning -> "Service is actively monitoring device position."
            else -> "Activate the tracker below to start."
          },
          color = theme.textSecondary,
          fontSize = 11.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Button
        Button(
          onClick = onToggleService,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isServiceRunning) Color(0xFFE53935) else theme.accent,
            contentColor = if (isServiceRunning || theme.name == "Snow Drift") Color.White else Color.Black
          ),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
        ) {
          Text(
            text = if (isServiceRunning) "STOP TRACKER" else "START TRACKER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        }
      }
    }
  }
}

@Composable
fun PermissionAlertCard(
  hasNotification: Boolean,
  hasDnd: Boolean,
  onRequestNotification: () -> Unit,
  onRequestDnd: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0x33FFA726)), // Warm orange
    border = BorderStroke(1.dp, Color(0xFFFFA726).copy(alpha = 0.3f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "⚠️",
          fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Action Required",
          color = Color(0xFFFFA726),
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Permissions are required for the app to function properly. Please grant:",
        color = Color(0xFFECEFF1),
        fontSize = 12.sp
      )
      Spacer(modifier = Modifier.height(10.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!hasNotification) {
          Button(
            onClick = onRequestNotification,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFFFA726),
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("Notifications", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
        if (!hasDnd) {
          Button(
            onClick = onRequestDnd,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFFFA726),
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("DND Policy Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun StatsRow(sessions: List<FocusSession>, theme: FocusTheme) {
  val totalDuration = sessions.sumOf { it.durationSeconds }
  val totalMinutes = totalDuration / 60
  val totalHours = totalMinutes / 60
  val displayedMinutes = totalMinutes % 60

  val durationText = when {
    totalHours > 0 -> "${totalHours}h ${displayedMinutes}m"
    totalMinutes > 0 -> "${totalMinutes}m ${totalDuration % 60}s"
    else -> "${totalDuration}s"
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = theme.cardBg),
      border = BorderStroke(1.dp, theme.border.copy(alpha = 0.3f))
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "TOTAL FOCUS TIME", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = durationText, fontSize = 16.sp, color = theme.textPrimary, fontWeight = FontWeight.Bold)
      }
    }

    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = theme.cardBg),
      border = BorderStroke(1.dp, theme.border.copy(alpha = 0.3f))
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "COMPLETED SESSIONS", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "${sessions.size}", fontSize = 16.sp, color = theme.textPrimary, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun LogTerminal(
  logs: List<String>,
  onClearLogs: () -> Unit,
  theme: FocusTheme,
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()

  // Scroll to bottom when logs size changes
  LaunchedEffect(logs.size) {
    if (logs.isNotEmpty()) {
      listState.animateScrollToItem(logs.size - 1)
    }
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = if (theme.name == "Snow Drift") Color(0xFFE2E8F0) else Color(0xFF040609)),
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "live_sensor_feed.log",
          color = theme.accent,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "CLEAR LOGS",
          color = Color(0xFFE57373),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier
            .clickable { onClearLogs() }
            .padding(4.dp)
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Divider(color = theme.border.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(8.dp))

      if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = "No logs recorded.",
            color = theme.textSecondary.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
          )
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          items(logs) { log ->
            Text(
              text = log,
              color = theme.accent.copy(alpha = 0.9f),
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              lineHeight = 14.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun HistoryList(
  sessions: List<FocusSession>,
  onClearHistory: () -> Unit,
  theme: FocusTheme,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Recorded Sessions",
        color = theme.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )
      if (sessions.isNotEmpty()) {
        IconButton(
          onClick = onClearHistory,
          modifier = Modifier.size(28.dp)
        ) {
          Text("🗑️", fontSize = 16.sp)
        }
      }
    }
    Spacer(modifier = Modifier.height(4.dp))

    if (sessions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .clip(RoundedCornerShape(16.dp))
          .background(theme.textSecondary.copy(alpha = 0.05f))
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No focus sessions recorded yet.\nActivate the service and place your device face down to start.",
          color = theme.textSecondary.copy(alpha = 0.6f),
          fontSize = 12.sp,
          textAlign = TextAlign.Center,
          lineHeight = 18.sp
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(sessions, key = { it.id }) { session ->
          val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
          val dateText = formatter.format(Date(session.startTime))

          val durationText = if (session.durationSeconds >= 60) {
            "${session.durationSeconds / 60}m ${session.durationSeconds % 60}s"
          } else {
            "${session.durationSeconds}s"
          }

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            border = BorderStroke(1.dp, theme.border.copy(alpha = 0.3f))
          ) {
            Row(
              modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = dateText,
                  color = theme.textPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = "Gravity Trigger Focus",
                  color = theme.textSecondary,
                  fontSize = 10.sp
                )
              }
              Text(
                text = durationText,
                color = theme.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}
