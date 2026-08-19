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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun ConfirmationDialog(
    toolName: String,
    promptText: String,
    targetEntity: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, EnergyAmber.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCanvas),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Confirm",
                    tint = EnergyAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ACTION CONFIRMATION REQUIRED",
                    style = MaterialTheme.typography.labelSmall,
                    color = EnergyAmber
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = promptText,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Say \"Yes\" / \"Confirm\" or tap below to proceed safely.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_cancel_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = CriticalRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "CANCEL", color = TextSecondary)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_confirm_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        tint = VoidBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "PROCEED", color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
