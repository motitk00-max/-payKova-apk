package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PulseBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    customApiKey: String,
    onApiKeySaved: (String) -> Unit,
    wakeWordSensitivity: Float,
    onSensitivityChanged: (Float) -> Unit,
    voicePitch: Float,
    onVoicePitchChanged: (Float) -> Unit,
    voiceSpeed: Float,
    onVoiceSpeedChanged: (Float) -> Unit,
    languageMode: String,
    onLanguageModeChanged: (String) -> Unit,
    onTestTool: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember { mutableStateOf(customApiKey) }
    var currentSensitivity by remember { mutableFloatStateOf(wakeWordSensitivity) }
    var currentPitch by remember { mutableFloatStateOf(voicePitch) }
    var currentSpeed by remember { mutableFloatStateOf(voiceSpeed) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCanvas),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KOVA CONFIGURATION",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Language Preference
            SectionHeader(icon = Icons.Default.Language, title = "Language & Code-Switching")
            Spacer(modifier = Modifier.height(8.dp))
            val languages = listOf("Auto (Hindi/Eng)", "Hindi Only", "Hinglish Only", "English Only")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { lang ->
                    val isSelected = languageMode == lang
                    Card(
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isSelected) CyberCyan else SurfaceBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onLanguageModeChanged(lang) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CyberCyan.copy(alpha = 0.15f) else SurfaceDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = lang,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CyberCyan else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Gemini Model Selection
            SectionHeader(icon = Icons.Default.Bolt, title = "Gemini Live AI Model")
            Spacer(modifier = Modifier.height(8.dp))
            val models = listOf(
                "gemini-3.1-flash-live-preview" to "Gemini 3.1 Flash Live (Recommended)",
                "gemini-2.5-flash-native-audio-preview-12-2025" to "Gemini 2.5 Flash Native Audio",
                "gemini-3.5-flash" to "Gemini 3.5 Flash (Standard REST)"
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                models.forEach { (modelKey, label) ->
                    val isSelected = selectedModel == modelKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) NeonViolet.copy(alpha = 0.15f) else SurfaceDark,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) NeonViolet else SurfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onModelSelected(modelKey) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                            Text(
                                text = modelKey,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Wake Word Sensitivity
            SectionHeader(icon = Icons.Default.RecordVoiceOver, title = "Wake Word Sensitivity ('Kova')")
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Low", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = "${(currentSensitivity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
                Text(text = "High", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            Slider(
                value = currentSensitivity,
                onValueChange = {
                    currentSensitivity = it
                    onSensitivityChanged(it)
                },
                valueRange = 0.2f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan,
                    inactiveTrackColor = SurfaceBorder
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Voice Pitch & Speed
            SectionHeader(icon = Icons.Default.Speed, title = "Kova Voice Modulation")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Voice Pitch: ${String.format("%.2f", currentPitch)}x",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Slider(
                value = currentPitch,
                onValueChange = {
                    currentPitch = it
                    onVoicePitchChanged(it)
                },
                valueRange = 0.8f..1.4f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonViolet,
                    activeTrackColor = NeonViolet,
                    inactiveTrackColor = SurfaceBorder
                )
            )

            Text(
                text = "Voice Speed: ${String.format("%.2f", currentSpeed)}x",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Slider(
                value = currentSpeed,
                onValueChange = {
                    currentSpeed = it
                    onVoiceSpeedChanged(it)
                },
                valueRange = 0.8f..1.4f,
                colors = SliderDefaults.colors(
                    thumbColor = PulseBlue,
                    activeTrackColor = PulseBlue,
                    inactiveTrackColor = SurfaceBorder
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Quick Test Tools
            SectionHeader(icon = Icons.Default.FlashOn, title = "Device Control Quick-Test")
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "YouTube" to "youtube",
                    "Battery" to "battery",
                    "Flashlight" to "torch",
                    "Calculator" to "calculator",
                    "Time" to "time",
                    "Settings" to "settings"
                ).forEach { (label, action) ->
                    Card(
                        modifier = Modifier
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { onTestTool(action) },
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "⚡ $label",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "DONE", color = VoidBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}
