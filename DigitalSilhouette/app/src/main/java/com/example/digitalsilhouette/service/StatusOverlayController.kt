package com.example.digitalsilhouette.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object StatusOverlayController {
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var isAdded = false
    private var currentMessage by mutableStateOf("")
    private var isVisible by mutableStateOf(false)

    fun show(context: Context, message: String) {
        if (!Settings.canDrawOverlays(context)) return

        if (windowManager == null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        currentMessage = message
        isVisible = true

        if (!isAdded) {
            composeView = ComposeView(context).apply {
                // Setup Lifecycle and SavedStateRegistry required for Compose in WindowManager
                val lifecycleOwner = object : androidx.lifecycle.LifecycleOwner, SavedStateRegistryOwner {
                    private val lifecycleRegistry = LifecycleRegistry(this)
                    private val savedStateRegistryController = SavedStateRegistryController.create(this)
                    override val lifecycle: Lifecycle get() = lifecycleRegistry
                    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
                    init {
                        savedStateRegistryController.performRestore(null)
                        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                    }
                }
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                setContent {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { -100 }),
                        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { -100 })
                    ) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF070A0F)) // AetherNeon background
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentMessage,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Auto-hide after 3 seconds
                    LaunchedEffect(currentMessage) {
                        if (isVisible) {
                            delay(3000)
                            isVisible = false
                            delay(400) // wait for exit animation
                            removeView()
                        }
                    }
                }
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }

            try {
                windowManager?.addView(composeView, params)
                isAdded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeView() {
        if (isAdded) {
            try {
                windowManager?.removeView(composeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            composeView = null
            isAdded = false
        }
    }
}
