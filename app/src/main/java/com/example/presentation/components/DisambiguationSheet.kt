package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ContactOption
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.PulseBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DisambiguationSheet(
    title: String,
    options: List<ContactOption>,
    onSelectOption: (ContactOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, PulseBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCanvas),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "CLARIFICATION REQUIRED",
                style = MaterialTheme.typography.labelSmall,
                color = PulseBlue
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark, RoundedCornerShape(12.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectOption(option) }
                            .padding(12.dp)
                            .testTag("disambiguation_option_${option.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PulseBlue.copy(alpha = 0.15f), CircleShape),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (option.type == "phone") Icons.Default.Phone else Icons.Default.Person,
                                contentDescription = null,
                                tint = PulseBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = option.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Select",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
