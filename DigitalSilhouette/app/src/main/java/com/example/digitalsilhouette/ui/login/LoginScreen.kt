package com.example.digitalsilhouette.ui.login

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.digitalsilhouette.R
import com.example.digitalsilhouette.theme.FocusTheme
import com.example.digitalsilhouette.theme.Domine
import java.util.Calendar

@Composable
fun LoginScreen(
  onLoginSuccess: (email: String, name: String, password: String) -> Unit,
  theme: FocusTheme,
  selectedTheme: String,
  onThemeSelected: (String) -> Unit,
  initialName: String = "",
  initialEmail: String = "",
  initialPassword: String = "",
  modifier: Modifier = Modifier
) {
  var name by remember { mutableStateOf(initialName) }
  var email by remember { mutableStateOf(initialEmail) }
  var password by remember { mutableStateOf(initialPassword) }
  var isPasswordVisible by remember { mutableStateOf(false) }

  var nameError by remember { mutableStateOf<String?>(null) }
  var emailError by remember { mutableStateOf<String?>(null) }
  var passwordError by remember { mutableStateOf<String?>(null) }

  // Time-of-day greeting
  val greeting = remember {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    when {
      hour < 5 -> "Burning the midnight oil? 🌙"
      hour < 12 -> "Good morning ☀️"
      hour < 17 -> "Good afternoon 🌤️"
      hour < 21 -> "Good evening 🌅"
      else -> "Night owl mode 🦉"
    }
  }

  // Staggered reveal animations
  var showContent by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { showContent = true }

  // Logo bounce animation
  val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
  val logoScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "logoScale"
  )

  // Soft background glow animation
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.03f,
    targetValue = 0.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glowAlpha"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    // Soft ambient background glow
    Box(
      modifier = Modifier
        .size(340.dp)
        .offset(y = (-60).dp)
        .clip(RoundedCornerShape(170.dp))
        .background(
          Brush.radialGradient(
            colors = listOf(theme.accent.copy(alpha = glowAlpha), Color.Transparent)
          )
        )
    )

    AnimatedVisibility(
      visible = showContent,
      enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
        initialOffsetY = { it / 6 },
        animationSpec = tween(600, easing = FastOutSlowInEasing)
      )
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
          .border(BorderStroke(1.dp, theme.border.copy(alpha = 0.2f)), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardBg)
      ) {
        Column(
          modifier = Modifier
            .padding(horizontal = 28.dp, vertical = 32.dp)
            .fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // App logo with gentle bounce
          Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
              .size(68.dp)
              .scale(logoScale)
              .padding(bottom = 8.dp)
          )

          Text(
            text = "Kinetix",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Domine,
            color = theme.textPrimary,
            letterSpacing = 2.sp
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Time-of-day greeting — feels personal
          Text(
            text = greeting,
            fontSize = 14.sp,
            color = theme.textSecondary,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
          )
          Text(
            text = "Your quiet corner of focus",
            fontSize = 11.sp,
            color = theme.textSecondary.copy(alpha = 0.7f),
            letterSpacing = 0.5.sp
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Theme picker
          ThemeSelectorRow(
            selectedTheme = selectedTheme,
            onThemeSelected = onThemeSelected,
            theme = theme
          )
          Spacer(modifier = Modifier.height(24.dp))

          // --- Fields with staggered animation ---

          // Name field
          AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(400, delayMillis = 150)) + slideInVertically(
              initialOffsetY = { it / 4 },
              animationSpec = tween(400, delayMillis = 150)
            )
          ) {
            Column {
              OutlinedTextField(
                value = name,
                onValueChange = {
                  name = it
                  nameError = null
                },
                label = { Text("What should we call you?", color = theme.textSecondary) },
                placeholder = { Text("e.g. Advaith", color = theme.textSecondary.copy(alpha = 0.4f)) },
                isError = nameError != null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = theme.accent,
                  unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.2f),
                  focusedTextColor = theme.textPrimary,
                  unfocusedTextColor = theme.textPrimary,
                  cursorColor = theme.accent,
                  focusedLabelColor = theme.accent,
                  unfocusedLabelColor = theme.textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
              )
              nameError?.let {
                Text(
                  text = it,
                  color = Color(0xFFEF5350),
                  fontSize = 11.sp,
                  modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))

          // Email field
          AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(
              initialOffsetY = { it / 4 },
              animationSpec = tween(400, delayMillis = 300)
            )
          ) {
            Column {
              OutlinedTextField(
                value = email,
                onValueChange = {
                  email = it
                  emailError = null
                },
                label = { Text("Your email", color = theme.textSecondary) },
                placeholder = { Text("name@example.com", color = theme.textSecondary.copy(alpha = 0.4f)) },
                isError = emailError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = theme.accent,
                  unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.2f),
                  focusedTextColor = theme.textPrimary,
                  unfocusedTextColor = theme.textPrimary,
                  cursorColor = theme.accent,
                  focusedLabelColor = theme.accent,
                  unfocusedLabelColor = theme.textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
              )
              emailError?.let {
                Text(
                  text = it,
                  color = Color(0xFFEF5350),
                  fontSize = 11.sp,
                  modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))

          // Password field
          AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(400, delayMillis = 450)) + slideInVertically(
              initialOffsetY = { it / 4 },
              animationSpec = tween(400, delayMillis = 450)
            )
          ) {
            Column {
              OutlinedTextField(
                value = password,
                onValueChange = {
                  password = it
                  passwordError = null
                },
                label = { Text("Password", color = theme.textSecondary) },
                placeholder = { Text("At least 6 characters", color = theme.textSecondary.copy(alpha = 0.4f)) },
                isError = passwordError != null,
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                  Text(
                    text = if (isPasswordVisible) "👁️" else "🙈",
                    modifier = Modifier
                      .clickable { isPasswordVisible = !isPasswordVisible }
                      .padding(8.dp)
                  )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = theme.accent,
                  unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.2f),
                  focusedTextColor = theme.textPrimary,
                  unfocusedTextColor = theme.textPrimary,
                  cursorColor = theme.accent,
                  focusedLabelColor = theme.accent,
                  unfocusedLabelColor = theme.textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
              )
              passwordError?.let {
                Text(
                  text = it,
                  color = Color(0xFFEF5350),
                  fontSize = 11.sp,
                  modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(28.dp))

          // Submit Button
          AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(400, delayMillis = 600)) + scaleIn(
              initialScale = 0.9f,
              animationSpec = tween(400, delayMillis = 600)
            )
          ) {
            Button(
              onClick = {
                var isValid = true
                if (name.isBlank()) {
                  nameError = "Oops! We need your name to get started 😊"
                  isValid = false
                }
                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                  emailError = "Hmm, that doesn't look like a valid email"
                  isValid = false
                }
                if (password.length < 6) {
                  passwordError = "A bit short — try at least 6 characters"
                  isValid = false
                }

                if (isValid) {
                  onLoginSuccess(email, name, password)
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = theme.accent,
                contentColor = if (theme.name == "Snow Drift") Color.White else Color.Black
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
            ) {
              Text(
                text = "Let's go ✨",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ThemeSelectorRow(
  selectedTheme: String,
  onThemeSelected: (String) -> Unit,
  theme: FocusTheme
) {
  // Friendly display names for themes
  val themes = listOf(
    "Aether Neon" to "Neon ⚡",
    "Cyberpunk" to "Cyber 🌃",
    "Forest Oasis" to "Forest 🌿",
    "Obsidian" to "Dark 🖤",
    "Snow Drift" to "Light ☁️"
  )
  LazyRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    items(themes) { (key, label) ->
      val isSelected = selectedTheme == key
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(if (isSelected) theme.accent.copy(alpha = 0.12f) else Color.Transparent)
          .border(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) theme.accent else theme.textSecondary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(10.dp)
          )
          .clickable { onThemeSelected(key) }
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Text(
          text = label,
          fontSize = 11.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) theme.accent else theme.textSecondary
        )
      }
    }
  }
}
