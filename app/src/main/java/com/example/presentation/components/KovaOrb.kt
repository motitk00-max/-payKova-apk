package com.example.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.KovaEmotion
import com.example.domain.model.KovaState
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PulseBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun KovaOrb(
    state: KovaState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "KovaOrbAnimations")

    // Slow breathing scale for Idle
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // Fast rotation for gyro rings
    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuousRotation"
    )

    // Fast pulse for Thinking state
    val thinkingPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinkingPulse"
    )

    // Secondary reverse rotation
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverseRotation"
    )

    // Derive colors based on state & emotion
    val (primaryColor, secondaryColor, accentGlow) = when (state) {
        is KovaState.Idle -> Triple(CyberCyan, NeonViolet, Color(0x3300F0FF))
        is KovaState.Listening -> Triple(CyberCyan, PulseBlue, Color(0x7700F0FF))
        is KovaState.Thinking -> Triple(NeonViolet, PulseBlue, Color(0x77A855F7))
        is KovaState.Speaking -> {
            when (state.emotion) {
                KovaEmotion.HAPPY, KovaEmotion.EXCITED -> Triple(MatrixGreen, CyberCyan, Color(0x7710B981))
                KovaEmotion.FUNNY, KovaEmotion.PLAYFUL -> Triple(NeonViolet, EnergyAmber, Color(0x77A855F7))
                KovaEmotion.CALM, KovaEmotion.EMPATHETIC -> Triple(PulseBlue, CyberCyan, Color(0x5538BDF8))
                KovaEmotion.CONCERNED, KovaEmotion.SERIOUS -> Triple(EnergyAmber, NeonViolet, Color(0x66F59E0B))
                else -> Triple(CyberCyan, NeonViolet, Color(0x6600F0FF))
            }
        }
        is KovaState.ExecutingTool -> Triple(MatrixGreen, PulseBlue, Color(0x7710B981))
        is KovaState.ConfirmationRequired -> Triple(EnergyAmber, NeonViolet, Color(0x77F59E0B))
        is KovaState.DisambiguationRequired -> Triple(PulseBlue, EnergyAmber, Color(0x6638BDF8))
        is KovaState.PermissionRequired -> Triple(EnergyAmber, CriticalRed, Color(0x77F59E0B))
        is KovaState.Error -> Triple(CriticalRed, EnergyAmber, Color(0x66EF4444))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            )
            .testTag("kova_orb_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 3.2f

            // Dynamic scale influenced by audio amplitude
            val dynamicScale = when (state) {
                is KovaState.Idle -> breathingScale
                is KovaState.Listening -> 1.0f + (amplitude * 0.45f)
                is KovaState.Thinking -> thinkingPulse
                is KovaState.Speaking -> 1.0f + (amplitude * 0.55f)
                is KovaState.ExecutingTool -> 1.05f
                else -> 1.0f
            }

            val currentRadius = baseRadius * dynamicScale

            // 1. Outermost Diffused Neon Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentGlow, accentGlow.copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = currentRadius * 1.8f
                ),
                radius = currentRadius * 1.8f,
                center = center
            )

            // 2. Holographic Outer Gyro Ring 1
            rotate(degrees = if (state is KovaState.Thinking || state is KovaState.ExecutingTool) continuousRotation * 3f else continuousRotation, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.45f),
                    radius = currentRadius * 1.35f,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f, 10f, 15f), 0f)
                    )
                )
                // Orbiting satellite dots
                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90).toDouble())
                    val dotX = center.x + (currentRadius * 1.35f * cos(angle)).toFloat()
                    val dotY = center.y + (currentRadius * 1.35f * sin(angle)).toFloat()
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }
            }

            // 3. Holographic Reverse Ring 2
            rotate(degrees = if (state is KovaState.Thinking) reverseRotation * 3f else reverseRotation, pivot = center) {
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.35f),
                    radius = currentRadius * 1.18f,
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), 0f)
                    )
                )
            }

            // 4. Reactive Audio Waveform Ribs (Listening & Speaking states)
            if (state is KovaState.Listening || state is KovaState.Speaking) {
                val waveCount = 18
                for (i in 0 until waveCount) {
                    val angleDeg = (i * (360f / waveCount)) + continuousRotation
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val waveAmp = ((sin(angleDeg * 3) + 1f) / 2f) * amplitude * 25.dp.toPx()
                    val startR = currentRadius * 0.95f
                    val endR = currentRadius * 1.05f + waveAmp

                    val startOffset = Offset(
                        (center.x + startR * cos(angleRad)).toFloat(),
                        (center.y + startR * sin(angleRad)).toFloat()
                    )
                    val endOffset = Offset(
                        (center.x + endR * cos(angleRad)).toFloat(),
                        (center.y + endR * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.8f),
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }

            // 5. Luminescent Plasma Core Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor,
                        Color(0xFF070B14)
                    ),
                    center = center,
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            // 6. Inner Cyber Shimmer Rim
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.9f),
                        secondaryColor.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.9f),
                        primaryColor.copy(alpha = 0.9f)
                    ),
                    center = center
                ),
                radius = currentRadius * 0.98f,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
