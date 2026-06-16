package com.example.digitalsilhouette.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.example.digitalsilhouette.theme.FocusTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GeometricBackground(
  theme: FocusTheme,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "geomBackground")

  // Ambient radial glow coordinates
  val glowXOffset by infiniteTransition.animateFloat(
    initialValue = -60f,
    targetValue = 60f,
    animationSpec = infiniteRepeatable(
      animation = tween(6000, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glowX"
  )
  val glowYOffset by infiniteTransition.animateFloat(
    initialValue = -40f,
    targetValue = 40f,
    animationSpec = infiniteRepeatable(
      animation = tween(5000, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glowY"
  )
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.05f,
    targetValue = 0.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glowAlpha"
  )

  // Floating offset for circles
  val circleFloatX by infiniteTransition.animateFloat(
    initialValue = -12f,
    targetValue = 12f,
    animationSpec = infiniteRepeatable(
      animation = tween(7000, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "circleX"
  )
  val circleFloatY by infiniteTransition.animateFloat(
    initialValue = -8f,
    targetValue = 8f,
    animationSpec = infiniteRepeatable(
      animation = tween(6000, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "circleY"
  )

  // Rotating animation for squares
  val squareRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(15000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "squareRot"
  )

  // Floating offset for squares
  val squareFloatY by infiniteTransition.animateFloat(
    initialValue = -10f,
    targetValue = 10f,
    animationSpec = infiniteRepeatable(
      animation = tween(5500, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "squareY"
  )

  // Wave phase animation (flows the wavy lines)
  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(9000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wavePhase"
  )

  // Dot matrix shimmer alpha
  val dotShimmerAlpha by infiniteTransition.animateFloat(
    initialValue = 0.06f,
    targetValue = 0.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(3500, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dotAlpha"
  )

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .background(theme.background)
  ) {
    val width = size.width
    val height = size.height

    // 1. Draw Moving Radial Ambient Glow
    val glowCenter = Offset(
      x = width * 0.5f + glowXOffset.dp.toPx(),
      y = height * 0.35f + glowYOffset.dp.toPx()
    )
    drawRect(
      brush = Brush.radialGradient(
        colors = listOf(theme.accent.copy(alpha = glowAlpha), Color.Transparent),
        center = glowCenter,
        radius = width * 0.85f
      )
    )

    // 2. Draw Geometric Circles (Watermarks)
    val circlePaintColor = theme.border.copy(alpha = 0.08f)
    val circleShadowColor = theme.textSecondary.copy(alpha = 0.03f)

    // Top-Left Circle (partial)
    drawFloatingCircle(
      baseCenter = Offset(0f, height * 0.1f),
      radius = 70.dp.toPx(),
      floatX = circleFloatX.dp.toPx(),
      floatY = circleFloatY.dp.toPx(),
      strokeColor = circlePaintColor,
      shadowColor = circleShadowColor
    )

    // Bottom-Left Circle
    drawFloatingCircle(
      baseCenter = Offset(width * 0.28f, height * 0.65f),
      radius = 110.dp.toPx(),
      floatX = circleFloatX.dp.toPx(),
      floatY = circleFloatY.dp.toPx(),
      strokeColor = circlePaintColor,
      shadowColor = circleShadowColor
    )

    // Top-Right Circle
    drawFloatingCircle(
      baseCenter = Offset(width * 0.88f, height * 0.28f),
      radius = 160.dp.toPx(),
      floatX = circleFloatX.dp.toPx(),
      floatY = circleFloatY.dp.toPx(),
      strokeColor = circlePaintColor,
      shadowColor = circleShadowColor
    )

    // 3. Draw Wavy Sinuous Lines (Teal/Accent styled)
    val waveColor = theme.accent.copy(alpha = 0.12f)
    val strokeWidth = 1.5.dp.toPx()

    // Top-Left to Center Wave Band
    drawWaveBand(
      startX = -50f,
      startY = height * 0.38f,
      endX = width * 0.6f,
      endY = height * 0.05f,
      amplitude = 20.dp.toPx(),
      frequency = 1.8f,
      phase = wavePhase,
      lineCount = 12,
      spacing = 6.dp.toPx(),
      color = waveColor,
      strokeWidth = strokeWidth
    )

    // Bottom-Center to Right Wave Band
    drawWaveBand(
      startX = width * 0.45f,
      startY = height * 0.92f,
      endX = width * 1.05f,
      endY = height * 0.62f,
      amplitude = 24.dp.toPx(),
      frequency = 1.4f,
      phase = -wavePhase, // moves in opposite direction
      lineCount = 14,
      spacing = 6.dp.toPx(),
      color = waveColor,
      strokeWidth = strokeWidth
    )

    // 4. Draw Dot Matrices / Grid Patterns (Neutral style)
    val dotColor = theme.textSecondary.copy(alpha = dotShimmerAlpha)
    
    // Top-Right Dot Grid (horizontal array)
    drawDotGrid(
      startX = width * 0.58f,
      startY = height * 0.11f,
      rows = 2,
      cols = 16,
      spacing = 10.dp.toPx(),
      dotRadius = 2.dp.toPx(),
      color = dotColor
    )

    // Mid-Left Dot Grid (vertical array)
    drawDotGrid(
      startX = width * 0.06f,
      startY = height * 0.38f,
      rows = 9,
      cols = 2,
      spacing = 10.dp.toPx(),
      dotRadius = 2.dp.toPx(),
      color = dotColor
    )

    // 5. Draw Floating Squares (Warm Gold/Accent styled)
    val solidSquareColor = theme.accent.copy(alpha = 0.16f)
    val outlineSquareColor = theme.accent.copy(alpha = 0.12f)

    // Center-Top Solid Square
    drawFloatingSquare(
      baseCenter = Offset(width * 0.55f, height * 0.28f + squareFloatY.dp.toPx()),
      sizePx = 54.dp.toPx(),
      rotationAngle = squareRotation,
      color = solidSquareColor,
      isSolid = true
    )

    // Bottom-Left Solid Square (overlapping bottom-left circle)
    drawFloatingSquare(
      baseCenter = Offset(width * 0.35f, height * 0.78f + squareFloatY.dp.toPx() * 0.6f),
      sizePx = 32.dp.toPx(),
      rotationAngle = -squareRotation * 1.2f,
      color = solidSquareColor,
      isSolid = true
    )

    // Bottom-Right Outlined Square
    drawFloatingSquare(
      baseCenter = Offset(width * 0.82f, height * 0.74f + squareFloatY.dp.toPx() * 0.8f),
      sizePx = 80.dp.toPx(),
      rotationAngle = squareRotation * 0.7f,
      color = outlineSquareColor,
      isSolid = false,
      strokeWidth = 3.dp.toPx()
    )

    // Bottom-Left horizontal line marker
    val markerColor = theme.textSecondary.copy(alpha = 0.18f)
    val markerY = height * 0.91f
    drawLine(
      color = markerColor,
      start = Offset(width * 0.05f, markerY),
      end = Offset(width * 0.16f, markerY),
      strokeWidth = 4.dp.toPx()
    )
    drawLine(
      color = markerColor,
      start = Offset(width * 0.08f, markerY + 6.dp.toPx()),
      end = Offset(width * 0.16f, markerY + 6.dp.toPx()),
      strokeWidth = 4.dp.toPx()
    )
  }
}

private fun DrawScope.drawFloatingCircle(
  baseCenter: Offset,
  radius: Float,
  floatX: Float,
  floatY: Float,
  strokeColor: Color,
  shadowColor: Color
) {
  val center = Offset(baseCenter.x + floatX, baseCenter.y + floatY)
  // Draw soft double shadow circles for 3D depth
  drawCircle(
    color = shadowColor,
    radius = radius + 4.dp.toPx(),
    center = Offset(center.x + 4.dp.toPx(), center.y + 4.dp.toPx()),
    style = Stroke(width = 8.dp.toPx())
  )
  drawCircle(
    color = strokeColor,
    radius = radius,
    center = center,
    style = Stroke(width = 2.dp.toPx())
  )
}

private fun DrawScope.drawFloatingSquare(
  baseCenter: Offset,
  sizePx: Float,
  rotationAngle: Float,
  color: Color,
  isSolid: Boolean,
  strokeWidth: Float = 2f
) {
  rotate(degrees = rotationAngle, pivot = baseCenter) {
    val left = baseCenter.x - sizePx / 2f
    val top = baseCenter.y - sizePx / 2f
    if (isSolid) {
      drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(sizePx, sizePx)
      )
    } else {
      drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(sizePx, sizePx),
        style = Stroke(width = strokeWidth)
      )
    }
  }
}

private fun DrawScope.drawDotGrid(
  startX: Float,
  startY: Float,
  rows: Int,
  cols: Int,
  spacing: Float,
  dotRadius: Float,
  color: Color
) {
  for (r in 0 until rows) {
    for (c in 0 until cols) {
      drawCircle(
        color = color,
        radius = dotRadius,
        center = Offset(startX + c * spacing, startY + r * spacing)
      )
    }
  }
}

private fun DrawScope.drawWaveBand(
  startX: Float,
  startY: Float,
  endX: Float,
  endY: Float,
  amplitude: Float,
  frequency: Float,
  phase: Float,
  lineCount: Int,
  spacing: Float,
  color: Color,
  strokeWidth: Float
) {
  val path = Path()
  val dx = endX - startX
  val dy = endY - startY
  val length = sqrt(dx * dx + dy * dy)
  val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()

  for (i in 0 until lineCount) {
    path.reset()
    val lineOffset = i * spacing
    val steps = 40
    for (step in 0..steps) {
      val t = step.toFloat() / steps
      // Interpolate base line coordinates
      val lx = startX + t * dx
      val ly = startY + t * dy

      // Sinuous displacement perpendicular to the wave flow direction
      val perpAngle = angle + (Math.PI / 2f).toFloat()
      val displacement = amplitude * sin(t * frequency * 2 * Math.PI + phase).toFloat()

      val px = lx + cos(perpAngle) * (displacement + lineOffset)
      val py = ly + sin(perpAngle) * (displacement + lineOffset)

      if (step == 0) {
        path.moveTo(px, py)
      } else {
        path.lineTo(px, py)
      }
    }
    drawPath(
      path = path,
      color = color,
      style = Stroke(width = strokeWidth)
    )
  }
}
