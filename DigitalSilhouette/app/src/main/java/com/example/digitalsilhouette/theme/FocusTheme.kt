package com.example.digitalsilhouette.theme

import androidx.compose.ui.graphics.Color

// Theme definition data class
data class FocusTheme(
  val background: Color,
  val cardBg: Color,
  val border: Color,
  val accent: Color,
  val pulseColor: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val name: String
)

// Predefined premium themes
object ThemePresets {
  val AetherNeon = FocusTheme(
    background = Color(0xFF070A0F),
    cardBg = Color(0x1F142030),
    border = Color(0x2B00E5FF),
    accent = Color(0xFF00E5FF),
    pulseColor = Color(0x0E00E5FF),
    textPrimary = Color.White,
    textSecondary = Color(0xFF788E9E),
    name = "Aether Neon"
  )

  val Cyberpunk = FocusTheme(
    background = Color(0xFF0C020B),
    cardBg = Color(0x1F2B0424),
    border = Color(0x2BFF007F),
    accent = Color(0xFFFF007F),
    pulseColor = Color(0x0EFF007F),
    textPrimary = Color.White,
    textSecondary = Color(0xFFAC8B9B),
    name = "Cyberpunk"
  )

  val ForestOasis = FocusTheme(
    background = Color(0xFF020D07),
    cardBg = Color(0x1F0A2715),
    border = Color(0x2B00F5D4),
    accent = Color(0xFF00F5D4),
    pulseColor = Color(0x0E00F5D4),
    textPrimary = Color.White,
    textSecondary = Color(0xFF7CA088),
    name = "Forest Oasis"
  )

  val Obsidian = FocusTheme(
    background = Color(0xFF070707),
    cardBg = Color(0x1F1E1E1E),
    border = Color(0x2BB0BEC5),
    accent = Color(0xFFCFD8DC),
    pulseColor = Color(0x0ECFD8DC),
    textPrimary = Color.White,
    textSecondary = Color(0xFF90A4AE),
    name = "Obsidian"
  )

  val SnowDrift = FocusTheme(
    background = Color(0xFFF0F3F6),
    cardBg = Color(0xFFFFFFFF),
    border = Color(0xFFD0D7DE),
    accent = Color(0xFF007AFF),
    pulseColor = Color(0x0E007AFF),
    textPrimary = Color(0xFF1F2328),
    textSecondary = Color(0xFF656D76),
    name = "Snow Drift"
  )
}
