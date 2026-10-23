package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Task
import com.example.data.priority.PriorityEngine
import com.example.data.priority.PriorityQuadrant
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditTaskDialog(
    initialTask: Task? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        importance: Int,
        estimatedEffortMinutes: Int,
        dueDate: Long,
        category: String,
        notificationEnabled: Boolean
    ) -> Unit
) {
    val isEditing = initialTask != null

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var importance by remember { mutableIntStateOf(initialTask?.importance ?: 2) }
    var estimatedEffortMinutes by remember { mutableIntStateOf(initialTask?.estimatedEffortMinutes ?: 30) }
    var dueDate by remember { mutableLongStateOf(initialTask?.dueDate ?: 0L) }
    var category by remember { mutableStateOf(initialTask?.category ?: "Personal") }
    var notificationEnabled by remember { mutableStateOf(initialTask?.notificationEnabled ?: true) }

    val liveScore by remember {
        derivedStateOf {
            PriorityEngine.calculateScore(
                importance = importance,
                estimatedEffortMinutes = estimatedEffortMinutes,
                dueDate = dueDate
            )
        }
    }

    val liveQuadrant by remember {
        derivedStateOf {
            PriorityEngine.getQuadrant(
                Task(
                    importance = importance,
                    estimatedEffortMinutes = estimatedEffortMinutes,
                    dueDate = dueDate,
                    priorityScore = liveScore
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Task" else "Create Prioritized Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time Intelligent Priority Score Badge Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (liveQuadrant) {
                            PriorityQuadrant.DO_FIRST -> Color(0xFFFEE2E2)
                            PriorityQuadrant.QUICK_WIN -> Color(0xFFD1FAE5)
                            PriorityQuadrant.SCHEDULE -> Color(0xFFEDE9FE)
                            PriorityQuadrant.BACKLOG -> Color(0xFFF1F5F9)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    when (liveQuadrant) {
                                        PriorityQuadrant.DO_FIRST -> PriorityUrgent
                                        PriorityQuadrant.QUICK_WIN -> PriorityLow
                                        PriorityQuadrant.SCHEDULE -> MaterialTheme.colorScheme.primary
                                        PriorityQuadrant.BACKLOG -> Color(0xFF64748B)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Smart Priority: ${liveScore.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${liveQuadrant.label}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (liveQuadrant) {
                                        PriorityQuadrant.DO_FIRST -> PriorityUrgent
                                        PriorityQuadrant.QUICK_WIN -> PriorityLow
                                        PriorityQuadrant.SCHEDULE -> MaterialTheme.colorScheme.primary
                                        PriorityQuadrant.BACKLOG -> Color(0xFF475569)
                                    }
                                )
                            }
                            Text(
                                text = when (liveQuadrant) {
                                    PriorityQuadrant.DO_FIRST -> "🔥 Urgent focus item. Tackle this first!"
                                    PriorityQuadrant.QUICK_WIN -> "⚡ Quick win. Low effort, high momentum boost!"
                                    PriorityQuadrant.SCHEDULE -> "📅 High impact deep-work task. Plan focus time."
                                    PriorityQuadrant.BACKLOG -> "⏳ Low urgency. Work on this when time permits."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g., Complete quarterly review deck") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Details") },
                    placeholder = { Text("Add any relevant context or sub-steps...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .testTag("task_description_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Importance Selector (1..4)
                Text(
                    text = "Importance Level",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val levels = listOf(
                        Triple(1, "Low", PriorityLow),
                        Triple(2, "Medium", PriorityMedium),
                        Triple(3, "High", PriorityHigh),
                        Triple(4, "Urgent", PriorityUrgent)
                    )
                    levels.forEach { (lvl, label, color) ->
                        val selected = importance == lvl
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { importance = lvl }
                                .testTag("importance_$lvl"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, color) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estimated Effort Presets
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Estimated Effort: ${formatEffort(estimatedEffortMinutes)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val effortPresets = listOf(15, 30, 45, 60, 90, 120, 240)
                    effortPresets.forEach { mins ->
                        FilterChip(
                            selected = estimatedEffortMinutes == mins,
                            onClick = { estimatedEffortMinutes = mins },
                            label = { Text(formatEffort(mins)) },
                            leadingIcon = if (estimatedEffortMinutes == mins) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Due Date Presets
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Deadline: ${formatDeadline(dueDate)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val now = System.currentTimeMillis()
                    val calendar = Calendar.getInstance()

                    // Today 5:00 PM
                    calendar.set(Calendar.HOUR_OF_DAY, 17)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val todayEvening = if (calendar.timeInMillis > now) calendar.timeInMillis else now + 4 * 3600 * 1000L

                    // Tomorrow 5:00 PM
                    val tomorrowEvening = todayEvening + 24 * 3600 * 1000L

                    // In 3 Days
                    val in3Days = now + 3 * 24 * 3600 * 1000L

                    // In 1 Week
                    val in1Week = now + 7 * 24 * 3600 * 1000L

                    val deadlineOptions = listOf(
                        "Today" to todayEvening,
                        "Tomorrow" to tomorrowEvening,
                        "In 3 Days" to in3Days,
                        "Next Week" to in1Week,
                        "No Deadline" to 0L
                    )

                    deadlineOptions.forEach { (label, timestamp) ->
                        val isSelected = if (timestamp == 0L) dueDate == 0L else Math.abs(dueDate - timestamp) < 60000L
                        FilterChip(
                            selected = isSelected,
                            onClick = { dueDate = timestamp },
                            label = { Text(label) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("Personal", "Work", "Health", "Study", "Finance", "Urgent")
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Smart Reminder Notification Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Smart Notifications",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Alert 30 min before deadline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = notificationEnabled,
                            onCheckedChange = { notificationEnabled = it },
                            modifier = Modifier.testTag("notification_toggle")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    importance,
                                    estimatedEffortMinutes,
                                    dueDate,
                                    category,
                                    notificationEnabled
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_task_button")
                    ) {
                        Text(if (isEditing) "Save Changes" else "Add Task")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    taskTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Task") },
        text = { Text("Are you sure you want to delete \"$taskTitle\"? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = PriorityUrgent
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatEffort(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

fun formatDeadline(timestamp: Long): String {
    if (timestamp <= 0L) return "None"
    val now = System.currentTimeMillis()
    val diff = timestamp - now
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    return when {
        diff < 0 -> "Overdue (${sdf.format(Date(timestamp))})"
        diff < 24 * 3600 * 1000L -> "Today (${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))})"
        diff < 48 * 3600 * 1000L -> "Tomorrow (${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))})"
        else -> sdf.format(Date(timestamp))
    }
}
