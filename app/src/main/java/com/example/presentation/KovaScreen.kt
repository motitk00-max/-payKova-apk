package com.example.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.KovaEmotion
import com.example.domain.model.KovaState
import com.example.presentation.components.ConfirmationDialog
import com.example.presentation.components.DisambiguationSheet
import com.example.presentation.components.KovaOrb
import com.example.presentation.components.PermissionOnboardingSheet
import com.example.presentation.components.SettingsSheet
import com.example.presentation.components.WaveformVisualizer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PulseBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KovaScreen(
    viewModel: KovaViewModel,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.kovaState.collectAsState()
    val amplitude by viewModel.audioAmplitude.collectAsState()
    val activeTranscript by viewModel.activeTranscript.collectAsState()
    val permissionStatus by viewModel.permissionStatus.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()

    val selectedModel by viewModel.selectedModel.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val wakeWordSensitivity by viewModel.wakeWordSensitivity.collectAsState()
    val voicePitch by viewModel.voicePitch.collectAsState()
    val voiceSpeed by viewModel.voiceSpeed.collectAsState()
    val languageMode by viewModel.languageMode.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarScan")
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VoidBlack, DarkCanvas, Color(0xFF04060A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Futuristic Header HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isServiceRunning) MatrixGreen else CyberCyan,
                                CircleShape
                            )
                            .alpha(radarAlpha)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KOVA // QUANTUM CORE",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Background Wake Word Standby Service Toggle
                    Card(
                        modifier = Modifier
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                            .clickable {
                                if (isServiceRunning) viewModel.stopBackgroundService()
                                else viewModel.startBackgroundService()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isServiceRunning) MatrixGreen.copy(alpha = 0.15f) else SurfaceElevated
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Standby Service",
                                tint = if (isServiceRunning) MatrixGreen else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isServiceRunning) "STANDBY ON" else "STANDBY OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isServiceRunning) MatrixGreen else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 2. Centerpiece Glowing Orb & Status Chip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Interactive Reactive Orb
                KovaOrb(
                    state = state,
                    amplitude = amplitude,
                    onClick = { viewModel.onOrbClicked() },
                    size = 260.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Waveform visualizer
                WaveformVisualizer(
                    amplitude = amplitude,
                    isActive = state is KovaState.Listening || state is KovaState.Speaking,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    height = 36.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // State Status Tag
                StateStatusChip(state = state)

                Spacer(modifier = Modifier.height(12.dp))

                // Live Spoken Transcript Bubble
                AnimatedVisibility(
                    visible = activeTranscript.isNotBlank() || state is KovaState.Speaking || state is KovaState.Thinking,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (state) {
                                        is KovaState.Listening -> "LIVE TRANSCRIPT"
                                        is KovaState.Thinking -> "PROCESSING QUERY..."
                                        is KovaState.Speaking -> "KOVA SPEAKING"
                                        else -> "ASSISTANT"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (state) {
                                        is KovaState.Speaking -> CyberCyan
                                        is KovaState.Thinking -> NeonViolet
                                        else -> TextMuted
                                    }
                                )

                                if (state is KovaState.Speaking) {
                                    val emotion = (state as KovaState.Speaking).emotion
                                    Text(
                                        text = "${getEmotionEmoji(emotion)} ${emotion.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberCyan
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = activeTranscript.ifBlank {
                                    if (state is KovaState.Thinking) "Analyzing speech and device commands..."
                                    else "Listening..."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }

            // 3. Quick Action Chips & Zero-Touch Mic Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preset Command Suggestions
                Text(
                    text = "TRY ASKING",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "YouTube kholo" to "Open YouTube",
                        "Battery kitni hai?" to "Check Battery",
                        "Torch ON karo" to "Turn on flashlight",
                        "Main bore ho raha hoon" to "Entertain me",
                        "Calculator kholo" to "Open Calculator"
                    ).forEach { (query, label) ->
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.processSpokenQuery(query) },
                            colors = CardDefaults.cardColors(containerColor = SurfaceElevated.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "“$query”",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = TextCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Push-to-Talk / Wake Orb Trigger
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(52.dp)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                        .clickable { viewModel.onOrbClicked() }
                        .testTag("push_to_talk_button"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state is KovaState.Speaking) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (state) {
                                is KovaState.Idle -> "SAY \"KOVA\" OR TAP TO SPEAK"
                                is KovaState.Listening -> "LISTENING... TAP TO FINISH"
                                is KovaState.Thinking -> "KOVA IS THINKING..."
                                is KovaState.Speaking -> "TAP TO INTERRUPT (BARGE-IN)"
                                else -> "ACTIVE"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                }
            }
        }

        // 4. Permission Onboarding Sheet (Overlay)
        if (!permissionStatus.hasCorePermissions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                PermissionOnboardingSheet(
                    status = permissionStatus,
                    onRequestPermissions = onRequestPermissions,
                    onOpenSettings = onOpenAppSettings
                )
            }
        }

        // 5. Action Confirmation Dialog (Overlay)
        if (state is KovaState.ConfirmationRequired) {
            val conf = state as KovaState.ConfirmationRequired
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                ConfirmationDialog(
                    toolName = conf.toolName,
                    promptText = conf.promptText,
                    targetEntity = conf.targetEntity,
                    onConfirm = { viewModel.confirmPendingAction() },
                    onCancel = { viewModel.cancelPendingAction() }
                )
            }
        }

        // 6. Disambiguation Sheet (Overlay)
        if (state is KovaState.DisambiguationRequired) {
            val dis = state as KovaState.DisambiguationRequired
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                DisambiguationSheet(
                    title = dis.title,
                    options = dis.options,
                    onSelectOption = { viewModel.selectDisambiguationOption(it) },
                    onDismiss = { viewModel.cancelPendingAction() }
                )
            }
        }

        // 7. Settings Sheet (Overlay)
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                SettingsSheet(
                    selectedModel = selectedModel,
                    onModelSelected = { viewModel.updateModel(it) },
                    customApiKey = customApiKey,
                    onApiKeySaved = { viewModel.updateApiKey(it) },
                    wakeWordSensitivity = wakeWordSensitivity,
                    onSensitivityChanged = { viewModel.updateSensitivity(it) },
                    voicePitch = voicePitch,
                    onVoicePitchChanged = { viewModel.updateVoicePitch(it) },
                    voiceSpeed = voiceSpeed,
                    onVoiceSpeedChanged = { viewModel.updateVoiceSpeed(it) },
                    languageMode = languageMode,
                    onLanguageModeChanged = { viewModel.updateLanguageMode(it) },
                    onTestTool = { viewModel.testToolDirectly(it) },
                    onClose = { showSettings = false }
                )
            }
        }
    }
}

@Composable
private fun StateStatusChip(state: KovaState) {
    val (color, label) = when (state) {
        is KovaState.Idle -> Pair(CyberCyan, "STANDBY")
        is KovaState.Listening -> Pair(PulseBlue, "LISTENING")
        is KovaState.Thinking -> Pair(NeonViolet, "PROCESSING")
        is KovaState.Speaking -> Pair(MatrixGreen, "SPEAKING")
        is KovaState.ExecutingTool -> Pair(MatrixGreen, "EXECUTING TOOL")
        is KovaState.ConfirmationRequired -> Pair(EnergyAmber, "CONFIRMATION")
        is KovaState.DisambiguationRequired -> Pair(PulseBlue, "CLARIFYING")
        is KovaState.PermissionRequired -> Pair(EnergyAmber, "PERMISSION")
        is KovaState.Error -> Pair(EnergyAmber, "ERROR")
    }

    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getEmotionEmoji(emotion: KovaEmotion): String {
    return when (emotion) {
        KovaEmotion.HAPPY -> "✨"
        KovaEmotion.EXCITED -> "⚡"
        KovaEmotion.FUNNY -> "😏"
        KovaEmotion.CALM -> "🌸"
        KovaEmotion.SERIOUS -> "🛡️"
        KovaEmotion.CONCERNED -> "💭"
        KovaEmotion.EMPATHETIC -> "💙"
        KovaEmotion.PLAYFUL -> "😜"
        KovaEmotion.DEFAULT -> "🤖"
    }
}
