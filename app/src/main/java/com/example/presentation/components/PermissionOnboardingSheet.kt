package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.permissions.PermissionManager
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun PermissionOnboardingSheet(
    status: PermissionManager.PermissionStatus,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCanvas),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security",
                    tint = CyberCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "KOVA PERMISSIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Voice-First Assistant Setup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kova requires device access to listen to wake words, place calls, and find contacts.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Permission Items
            PermissionItem(
                icon = Icons.Default.Mic,
                title = "Microphone",
                description = "Required so Kova can hear you and detect the 'Kova' wake word locally.",
                isGranted = status.hasRecordAudio
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                icon = Icons.Default.Contacts,
                title = "Contacts",
                description = "Needed when you ask Kova to search contacts or send WhatsApp messages.",
                isGranted = status.hasContacts
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                icon = Icons.Default.Call,
                title = "Phone Calls",
                description = "Needed when you ask Kova to place calls directly.",
                isGranted = status.hasCallPhone
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "Keeps Kova's background wake-word listening service reliable.",
                isGranted = status.hasNotifications
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("grant_permissions_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (status.hasRecordAudio) "GRANT REMAINING PERMISSIONS" else "ENABLE VOICE ASSISTANT",
                    color = VoidBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .border(1.dp, if (isGranted) MatrixGreen.copy(alpha = 0.4f) else SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isGranted) MatrixGreen.copy(alpha = 0.15f) else NeonViolet.copy(alpha = 0.15f),
                    CircleShape
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isGranted) MatrixGreen else CyberCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = if (isGranted) "GRANTED" else "REQUIRED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGranted) MatrixGreen else TextMuted
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
