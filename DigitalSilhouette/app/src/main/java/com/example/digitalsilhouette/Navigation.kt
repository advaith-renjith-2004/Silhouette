package com.example.digitalsilhouette

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.digitalsilhouette.data.DefaultDataRepository
import com.example.digitalsilhouette.theme.ThemePresets
import com.example.digitalsilhouette.ui.login.LoginScreen
import com.example.digitalsilhouette.ui.main.MainScreen

@Composable
fun MainNavigation(detectedSsid: String? = null) {
  val context = LocalContext.current.applicationContext
  val repository = remember { DefaultDataRepository.getInstance(context) }
  val isLoggedIn by repository.isLoggedIn.collectAsStateWithLifecycle()
  val selectedTheme by repository.selectedTheme.collectAsStateWithLifecycle()
  val userName by repository.userName.collectAsStateWithLifecycle()
  val userEmail by repository.userEmail.collectAsStateWithLifecycle()
  val userPassword by repository.userPassword.collectAsStateWithLifecycle()

  val currentTheme = remember(selectedTheme) {
    when (selectedTheme) {
      "Cyberpunk" -> ThemePresets.Cyberpunk
      "Forest Oasis" -> ThemePresets.ForestOasis
      "Obsidian" -> ThemePresets.Obsidian
      "Snow Drift" -> ThemePresets.SnowDrift
      else -> ThemePresets.AetherNeon
    }
  }

  key(isLoggedIn) {
    val startDestination = if (isLoggedIn) Main else Login
    val backStack = rememberNavBackStack(startDestination)

    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider = entryProvider {
        entry<Login> {
          LoginScreen(
            onLoginSuccess = { email, name, password ->
              repository.loginUser(email, name, password)
            },
            theme = currentTheme,
            selectedTheme = selectedTheme,
            onThemeSelected = { themeName ->
              repository.setTheme(themeName)
            },
            initialName = userName,
            initialEmail = userEmail,
            initialPassword = userPassword,
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Main> {
          MainScreen(
            detectedSsid = detectedSsid,
            onItemClick = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
      }
    )
  }
}
