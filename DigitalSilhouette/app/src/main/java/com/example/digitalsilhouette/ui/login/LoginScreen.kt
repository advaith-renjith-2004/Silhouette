package com.example.digitalsilhouette.ui.login

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalsilhouette.theme.FocusTheme

@Composable
fun LoginScreen(
  onLoginSuccess: (email: String, name: String) -> Unit,
  theme: FocusTheme,
  modifier: Modifier = Modifier
) {
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }

  var nameError by remember { mutableStateOf<String?>(null) }
  var emailError by remember { mutableStateOf<String?>(null) }
  var passwordError by remember { mutableStateOf<String?>(null) }

  // Floating background circle animation to make the login feel dynamic & premium
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(2500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    // Glowing ambient background effect
    Box(
      modifier = Modifier
        .size(300.dp)
        .offset(y = (-80).dp)
        .clip(RoundedCornerShape(150.dp))
        .background(
          Brush.radialGradient(
            colors = listOf(theme.accent.copy(alpha = 0.05f * pulseScale), Color.Transparent)
          )
        )
    )

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .border(BorderStroke(1.dp, theme.border.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = theme.cardBg)
    ) {
      Column(
        modifier = Modifier
          .padding(24.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // App logo & Name
        Text(
          text = "SILHOUETTE",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
          color = theme.textPrimary,
          letterSpacing = 4.sp
        )
        Text(
          text = "Secure Space Gateway",
          fontSize = 11.sp,
          color = theme.textSecondary,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Name field
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            nameError = null
          },
          label = { Text("Full Name", color = theme.textSecondary) },
          isError = nameError != null,
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.accent,
            unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.3f),
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
            color = Color(0xFFE53935),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp)
          )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Email field
        OutlinedTextField(
          value = email,
          onValueChange = {
            email = it
            emailError = null
          },
          label = { Text("Email Address", color = theme.textSecondary) },
          isError = emailError != null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.accent,
            unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.3f),
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
            color = Color(0xFFE53935),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp)
          )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Password field
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            passwordError = null
          },
          label = { Text("Password", color = theme.textSecondary) },
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
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.accent,
            unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.3f),
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
            color = Color(0xFFE53935),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp)
          )
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
          onClick = {
            var isValid = true
            if (name.isBlank()) {
              nameError = "Name cannot be empty"
              isValid = false
            }
            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
              emailError = "Please enter a valid email address"
              isValid = false
            }
            if (password.length < 6) {
              passwordError = "Password must be at least 6 characters"
              isValid = false
            }

            if (isValid) {
              onLoginSuccess(email, name)
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = theme.accent,
            contentColor = if (theme.name == "Snow Drift") Color.White else Color.Black
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
        ) {
          Text(
            text = "ENTER SECURE SPACE",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        }
      }
    }
  }
}
