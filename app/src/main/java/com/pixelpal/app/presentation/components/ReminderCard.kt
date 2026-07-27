package com.pixelpal.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelpal.app.domain.model.Reminder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReminderCard(
    reminder: Reminder,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(reminder.triggerTime))

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                reminder.message?.takeIf { it.isNotEmpty() }?.let { message ->
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⏰ $formattedTime",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                if (reminder.status == "PENDING") {
                    IconButton(onClick = onComplete) {
                        Text("✓", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDelete) {
                    Text("🗑", fontSize = 16.sp)
                }
            }
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this.isNullOrEmpty()
