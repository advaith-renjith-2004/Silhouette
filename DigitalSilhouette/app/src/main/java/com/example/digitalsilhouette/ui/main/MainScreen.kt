package com.example.digitalsilhouette.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import com.example.digitalsilhouette.theme.Domine

// Motivational quotes that rotate — gives the app a personal, human touch
private val motivationalQuotes = listOf(
  "Deep work is the superpower of the 21st century.",
  "Focus is not about saying yes. It's about saying no.",
  "Small steps every day lead to big changes.",
  "Your phone can wait. Your dreams can't.",
  "Be where your feet are.",
  "Silence is the sleep that nourishes wisdom.",
  "Every minute of focus compounds over time.",
  "You don't need more time — you need more focus."
)

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
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF00E5FF))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Getting things ready...",
              color = Color(0xFF788E9E),
              fontSize = 13.sp
            )
          }
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
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("😔", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Something went wrong",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = (state as MainScreenUiState.Error).throwable.localizedMessage ?: "Unknown error",
              color = Color(0xFFEF5350),
              textAlign = TextAlign.Center,
              fontSize = 12.sp
            )
          }
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

  // Staggered entrance animation
  var showContent by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { showContent = true }

  // Random motivational quote — changes each composition
  val dailyQuote = remember {
    motivationalQuotes[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % motivationalQuotes.size]
  }

  // Time-of-day greeting
  val greeting = remember {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val firstName = successState.userName.split(" ").firstOrNull() ?: successState.userName
    when {
      hour < 5 -> "Late night, $firstName 🌙"
      hour < 12 -> "Good morning, $firstName ☀️"
      hour < 17 -> "Good afternoon, $firstName 🌤️"
      hour < 21 -> "Good evening, $firstName 🌅"
      else -> "Hey there, $firstName 🦉"
    }
  }

  Column(
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header — personalized and warm
    Spacer(modifier = Modifier.height(12.dp))
    AnimatedVisibility(
      visible = showContent,
      enter = fadeIn(tween(500)) + slideInVertically(
        initialOffsetY = { -it / 4 },
        animationSpec = tween(500)
      )
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = greeting,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Domine,
            color = theme.textPrimary,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "\"$dailyQuote\"",
            fontSize = 11.sp,
            color = theme.textSecondary.copy(alpha = 0.7f),
            letterSpacing = 0.3.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Normal
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = { viewModel.logoutUser() },
          modifier = Modifier.size(36.dp)
        ) {
          Text("👋", fontSize = 20.sp)
        }
      }
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Permission Alerts Block
    if (!successState.hasNotificationPermission || !successState.hasDndPermission) {
      AnimatedVisibility(
        visible = showContent,
        enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(
          initialOffsetY = { it / 4 },
          animationSpec = tween(400, delayMillis = 100)
        )
      ) {
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
      }
      Spacer(modifier = Modifier.height(12.dp))
    }

    // Main Silhouette Shield Card with animations
    AnimatedVisibility(
      visible = showContent,
      enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(
        initialOffsetY = { it / 3 },
        animationSpec = tween(500, delayMillis = 200, easing = FastOutSlowInEasing)
      )
    ) {
      SilhouetteShield(
        isServiceRunning = successState.isServiceRunning,
        isFocusActive = successState.isFocusActive,
        elapsedSeconds = elapsedSeconds,
        onToggleService = { viewModel.toggleService(context) },
        theme = theme
      )
    }
    Spacer(modifier = Modifier.height(12.dp))

    // Stats Section
    AnimatedVisibility(
      visible = showContent,
      enter = fadeIn(tween(400, delayMillis = 350)) + slideInVertically(
        initialOffsetY = { it / 3 },
        animationSpec = tween(400, delayMillis = 350)
      )
    ) {
      StatsRow(sessions = successState.completedSessions, theme = theme)
    }
    Spacer(modifier = Modifier.height(12.dp))

    // Live Logs Terminal & History Tab Split
    var activeTab by remember { mutableStateOf("logs") }
    AnimatedVisibility(
      visible = showContent,
      enter = fadeIn(tween(400, delayMillis = 450))
    ) {
      TabSelector(activeTab = activeTab, onTabSelected = { activeTab = it }, theme = theme)
    }
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
fun TabSelector(
  activeTab: String,
  onTabSelected: (String) -> Unit,
  theme: FocusTheme
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(theme.textSecondary.copy(alpha = 0.08f))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(10.dp))
        .background(if (activeTab == "logs") theme.accent.copy(alpha = 0.1f) else Color.Transparent)
        .border(
          width = if (activeTab == "logs") 1.dp else 0.dp,
          color = if (activeTab == "logs") theme.accent.copy(alpha = 0.3f) else Color.Transparent,
          shape = RoundedCornerShape(10.dp)
        )
        .clickable { onTabSelected("logs") }
        .padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "📡 Live Feed",
        fontSize = 12.sp,
        fontWeight = if (activeTab == "logs") FontWeight.Bold else FontWeight.Medium,
        color = if (activeTab == "logs") theme.accent else theme.textSecondary,
        letterSpacing = 0.5.sp
      )
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(10.dp))
        .background(if (activeTab == "history") theme.accent.copy(alpha = 0.1f) else Color.Transparent)
        .border(
          width = if (activeTab == "history") 1.dp else 0.dp,
          color = if (activeTab == "history") theme.accent.copy(alpha = 0.3f) else Color.Transparent,
          shape = RoundedCornerShape(10.dp)
        )
        .clickable { onTabSelected("history") }
        .padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "📋 History",
        fontSize = 12.sp,
        fontWeight = if (activeTab == "history") FontWeight.Bold else FontWeight.Medium,
        color = if (activeTab == "history") theme.accent else theme.textSecondary,
        letterSpacing = 0.5.sp
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
    Brush.verticalGradient(listOf(theme.accent.copy(alpha = 0.06f), Color.Transparent))
  } else {
    Brush.verticalGradient(listOf(Color(0x05FFFFFF), Color.Transparent))
  }

  // Human-readable elapsed time
  val elapsedText = when {
    elapsedSeconds < 60 -> "${elapsedSeconds}s"
    elapsedSeconds < 3600 -> "${elapsedSeconds / 60}m ${elapsedSeconds % 60}s"
    else -> "${elapsedSeconds / 3600}h ${(elapsedSeconds % 3600) / 60}m"
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
                text = elapsedText,
                color = theme.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = "focusing",
                color = theme.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
              )
            } else {
              Text(
                text = if (isServiceRunning) "👂" else "💤",
                fontSize = 28.sp
              )
              Text(
                text = if (isServiceRunning) "listening" else "off",
                color = if (isServiceRunning) theme.accent else theme.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // State Text — conversational
        Text(
          text = when {
            isFocusActive -> "You're in the zone 🎯"
            isServiceRunning -> "Place your phone face-down to begin"
            else -> "Ready when you are"
          },
          color = theme.textPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center,
          fontFamily = Domine
        )
        Text(
          text = when {
            isFocusActive -> "Calls and notifications are silenced for you."
            isServiceRunning -> "Sensors are listening for the flip gesture."
            else -> "Tap below to activate focus tracking."
          },
          color = theme.textSecondary,
          fontSize = 11.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
          lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Toggle Button — friendlier labels
        Button(
          onClick = onToggleService,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isServiceRunning) Color(0xFFE53935) else theme.accent,
            contentColor = if (isServiceRunning || theme.name == "Snow Drift") Color.White else Color.Black
          ),
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
        ) {
          Text(
            text = if (isServiceRunning) "End Focus" else "Begin Focus",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
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
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0x22FFA726)),
    border = BorderStroke(1.dp, Color(0xFFFFA726).copy(alpha = 0.25f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "🔔",
          fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Quick setup needed",
          color = Color(0xFFFFA726),
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "We need a couple of permissions so focus mode can work its magic:",
        color = Color(0xFFECEFF1),
        fontSize = 12.sp,
        lineHeight = 17.sp
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
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Text("Allow Notifications", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }
        if (!hasDnd) {
          Button(
            onClick = onRequestDnd,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFFFA726),
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Text("DND Access", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@Composable
fun StatsRow(sessions: List<FocusSession>, theme: FocusTheme) {
  val totalDuration = sessions.sumOf { it.durationSeconds }

  // Human-readable total time
  val durationText = when {
    totalDuration >= 3600 -> {
      val h = totalDuration / 3600
      val m = (totalDuration % 3600) / 60
      if (m > 0) "${h}h ${m}m" else "${h} hour${if (h > 1) "s" else ""}"
    }
    totalDuration >= 60 -> {
      val m = totalDuration / 60
      "${m} minute${if (m > 1) "s" else ""}"
    }
    totalDuration > 0 -> "${totalDuration} seconds"
    else -> "—"
  }

  // Sessions count text
  val sessionsText = when (sessions.size) {
    0 -> "—"
    1 -> "1 session"
    else -> "${sessions.size} sessions"
  }

  // Calculate streak (consecutive days with at least one session)
  val streakDays = remember(sessions) {
    if (sessions.isEmpty()) return@remember 0
    val cal = Calendar.getInstance()
    val sessionDays = sessions.map { session ->
      cal.timeInMillis = session.startTime
      val year = cal.get(Calendar.YEAR)
      val day = cal.get(Calendar.DAY_OF_YEAR)
      year * 1000 + day
    }.toSet().sorted().reversed()

    if (sessionDays.isEmpty()) return@remember 0

    val today = Calendar.getInstance()
    val todayKey = today.get(Calendar.YEAR) * 1000 + today.get(Calendar.DAY_OF_YEAR)

    // Check if today or yesterday is in the set
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yesterdayKey = yesterdayCal.get(Calendar.YEAR) * 1000 + yesterdayCal.get(Calendar.DAY_OF_YEAR)

    if (!sessionDays.contains(todayKey) && !sessionDays.contains(yesterdayKey)) {
      return@remember 0
    }

    var streak = 0
    var checkCal = Calendar.getInstance()
    // Start from today if today has a session, otherwise from yesterday
    if (!sessionDays.contains(todayKey)) {
      checkCal.add(Calendar.DAY_OF_YEAR, -1)
    }
    while (true) {
      val key = checkCal.get(Calendar.YEAR) * 1000 + checkCal.get(Calendar.DAY_OF_YEAR)
      if (sessionDays.contains(key)) {
        streak++
        checkCal.add(Calendar.DAY_OF_YEAR, -1)
      } else {
        break
      }
    }
    streak
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Total Focus Time
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = theme.cardBg),
      border = BorderStroke(1.dp, theme.border.copy(alpha = 0.2f))
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "⏱️", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = durationText, fontSize = 14.sp, color = theme.textPrimary, fontWeight = FontWeight.Bold)
        Text(text = "total focus", fontSize = 9.sp, color = theme.textSecondary)
      }
    }

    // Sessions Count
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = theme.cardBg),
      border = BorderStroke(1.dp, theme.border.copy(alpha = 0.2f))
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "📊", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = sessionsText, fontSize = 14.sp, color = theme.textPrimary, fontWeight = FontWeight.Bold)
        Text(text = "completed", fontSize = 9.sp, color = theme.textSecondary)
      }
    }

    // Streak
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = theme.cardBg),
      border = BorderStroke(1.dp, theme.border.copy(alpha = 0.2f))
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = if (streakDays > 0) "🔥" else "💫", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (streakDays > 0) "$streakDays day${if (streakDays > 1) "s" else ""}" else "—",
          fontSize = 14.sp,
          color = theme.textPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(text = "streak", fontSize = 9.sp, color = theme.textSecondary)
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
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = if (theme.name == "Snow Drift") Color(0xFFE2E8F0) else Color(0xFF040609)),
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "📡 live_sensor_feed.log",
          color = theme.accent,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Clear",
          color = Color(0xFFE57373),
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier
            .clickable { onClearLogs() }
            .padding(4.dp)
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = theme.border.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(8.dp))

      if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "No activity yet",
              color = theme.textSecondary.copy(alpha = 0.5f),
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            )
          }
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
        text = "Your focus sessions",
        color = theme.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
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
    Spacer(modifier = Modifier.height(6.dp))

    if (sessions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .clip(RoundedCornerShape(18.dp))
          .background(theme.textSecondary.copy(alpha = 0.04f))
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("🧘", fontSize = 32.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No sessions yet",
            color = theme.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Start the tracker and place your phone face-down.\nYour focus sessions will appear here.",
            color = theme.textSecondary.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(sessions, key = { it.id }) { session ->
          val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
          val dateText = formatter.format(Date(session.startTime))

          // Human-readable duration
          val durationText = when {
            session.durationSeconds >= 3600 -> {
              val h = session.durationSeconds / 3600
              val m = (session.durationSeconds % 3600) / 60
              if (m > 0) "${h}h ${m}m" else "$h hour${if (h > 1) "s" else ""}"
            }
            session.durationSeconds >= 60 -> {
              val m = session.durationSeconds / 60
              val s = session.durationSeconds % 60
              if (s > 0) "${m}m ${s}s" else "$m min${if (m > 1) "s" else ""}"
            }
            else -> "${session.durationSeconds} sec${if (session.durationSeconds > 1) "s" else ""}"
          }

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            border = BorderStroke(1.dp, theme.border.copy(alpha = 0.2f))
          ) {
            Row(
              modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                  text = "Focus session",
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
