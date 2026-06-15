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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF070A0F)) // Very dark premium night-sky background
  ) {
    when (state) {
      MainScreenUiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = Color(0xFF00E5FF))
        }
      }
      is MainScreenUiState.Success -> {
        MainScreenContent(
          successState = state as MainScreenUiState.Success,
          viewModel = viewModel,
          modifier = modifier.fillMaxSize()
        )
      }
      is MainScreenUiState.Error -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreenContent(
  successState: MainScreenUiState.Success,
  viewModel: MainScreenViewModel,
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
    modifier = modifier.padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "DIGITAL SILHOUETTE",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      color = Color.White,
      letterSpacing = 3.sp
    )
    Text(
      text = "Context-Aware Focus Space",
      fontSize = 12.sp,
      color = Color(0xFF788E9E),
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(24.dp))

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
      Spacer(modifier = Modifier.height(16.dp))
    }

    // Main Silhouette Shield Card
    SilhouetteShield(
      isServiceRunning = successState.isServiceRunning,
      isFocusActive = successState.isFocusActive,
      elapsedSeconds = elapsedSeconds,
      onToggleService = { viewModel.toggleService(context) }
    )
    Spacer(modifier = Modifier.height(20.dp))

    // Stats Section
    StatsRow(sessions = successState.completedSessions)
    Spacer(modifier = Modifier.height(20.dp))

    // History Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Session History",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
      )
      if (successState.completedSessions.isNotEmpty()) {
        IconButton(onClick = { viewModel.clearHistory() }) {
          Text(
            text = "🗑️",
            fontSize = 18.sp
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // History List
    if (successState.completedSessions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0x0DFFFFFF))
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No focus sessions recorded yet.\nActivate the service and place your device face down to start.",
          color = Color(0xFF627A8A),
          fontSize = 13.sp,
          textAlign = TextAlign.Center,
          lineHeight = 20.sp
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(successState.completedSessions, key = { it.id }) { session ->
          SessionHistoryItem(session = session)
        }
      }
    }
  }
}

@Composable
fun SilhouetteShield(
  isServiceRunning: Boolean,
  isFocusActive: Boolean,
  elapsedSeconds: Long,
  onToggleService: () -> Unit
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

  val cardBackground = if (isFocusActive) {
    Brush.verticalGradient(listOf(Color(0xFF0F1E2E), Color(0xFF0A0F17)))
  } else {
    Brush.verticalGradient(listOf(Color(0xFF141923), Color(0xFF0A0C10)))
  }

  val shieldColor = when {
    isFocusActive -> Color(0xFF00E5FF)
    isServiceRunning -> Color(0xFF00F5D4)
    else -> Color(0xFF37474F)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(cardBackground)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Shield Pulse Circle
        Box(
          modifier = Modifier
            .size(160.dp),
          contentAlignment = Alignment.Center
        ) {
          if (isFocusActive) {
            Box(
              modifier = Modifier
                .fillMaxSize(pulseScale)
                .clip(CircleShape)
                .background(Color(0x1200E5FF))
                .border(BorderStroke(2.dp, Color(0x3300E5FF)), CircleShape)
            )
            Box(
              modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color(0x0A00E5FF))
                .border(BorderStroke(1.5.dp, Color(0x4D00E5FF).copy(alpha = pulseAlpha)), CircleShape)
            )
          } else if (isServiceRunning) {
            Box(
              modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .border(BorderStroke(1.dp, Color(0x1F00F5D4)), CircleShape)
            )
          }

          // Inner Circle Display
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
              .size(110.dp)
              .clip(CircleShape)
              .background(Color(0xFF0D121A))
              .border(BorderStroke(2.dp, shieldColor), CircleShape)
          ) {
            if (isFocusActive) {
              Text(
                text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = "FOCUSED",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
              )
            } else {
              Text(
                text = if (isServiceRunning) "IDLE" else "OFF",
                color = if (isServiceRunning) Color(0xFF00F5D4) else Color(0xFF788E9E),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // State Text
        Text(
          text = when {
            isFocusActive -> "Deep Work Active"
            isServiceRunning -> "Listening. Flip face-down on a desk to focus."
            else -> "Digital Silhouette is Disabled"
          },
          color = Color.White,
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
          color = Color(0xFF788E9E),
          fontSize = 11.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Toggle Button
        Button(
          onClick = onToggleService,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isServiceRunning) Color(0xFFE53935) else Color(0xFF00E5FF),
            contentColor = if (isServiceRunning) Color.White else Color.Black
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
    colors = CardDefaults.cardColors(containerColor = Color(0x33FFA726)), // Warm translucent orange
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
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Permissions are required for the app to function properly. Please grant:",
        color = Color(0xFFECEFF1),
        fontSize = 12.sp
      )
      Spacer(modifier = Modifier.height(12.dp))

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
fun StatsRow(sessions: List<FocusSession>) {
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
      colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)),
      border = BorderStroke(1.dp, Color(0x0AFFFFFF))
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "TOTAL FOCUS TIME", fontSize = 10.sp, color = Color(0xFF788E9E), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = durationText, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
      }
    }

    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF)),
      border = BorderStroke(1.dp, Color(0x0AFFFFFF))
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "COMPLETED SESSIONS", fontSize = 10.sp, color = Color(0xFF788E9E), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${sessions.size}", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun SessionHistoryItem(session: FocusSession) {
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
    colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
    border = BorderStroke(1.dp, Color(0x05FFFFFF))
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
          color = Color(0xFFECEFF1),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = "Gravity Trigger Focus",
          color = Color(0xFF788E9E),
          fontSize = 10.sp
        )
      }
      Text(
        text = durationText,
        color = Color(0xFF00E5FF),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
