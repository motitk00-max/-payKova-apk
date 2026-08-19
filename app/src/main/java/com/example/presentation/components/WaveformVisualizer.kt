package com.example.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PulseBlue
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    barCount: Int = 28
) {
    val animatedAmp = remember { Animatable(0f) }

    LaunchedEffect(amplitude, isActive) {
        val target = if (isActive) amplitude.coerceIn(0.05f, 1f) else 0.02f
        animatedAmp.animateTo(target, tween(60))
    }

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val width = size.width
        val barWidth = width / (barCount * 1.8f)
        val spacing = (width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)
        val midY = size.height / 2f

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            // Symmetric wave distribution
            val distFromCenter = 1f - (Math.abs(progress - 0.5f) * 2f)
            val sineFactor = (sin(progress * Math.PI * 3 + System.currentTimeMillis() * 0.005) + 1f) / 2f
            val dynamicHeight = (size.height * 0.15f) + (size.height * 0.8f * animatedAmp.value * distFromCenter * (0.4f + 0.6f * sineFactor.toFloat()))

            val x = i * (barWidth + spacing)
            val y = midY - (dynamicHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(CyberCyan, NeonViolet, PulseBlue),
                    startY = y,
                    endY = y + dynamicHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, dynamicHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
