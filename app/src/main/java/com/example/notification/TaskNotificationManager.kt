package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Task
import com.example.data.priority.PriorityEngine
import com.example.data.priority.PriorityQuadrant
import kotlin.math.absoluteValue

object TaskNotificationManager {

    const val CHANNEL_ID = "taskflow_smart_reminders"
    const val CHANNEL_NAME = "Smart Task Reminders"
    const val CHANNEL_DESC = "Notifications for urgent deadlines and prioritized task suggestions"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun scheduleTaskReminder(context: Context, task: Task) {
        if (!task.notificationEnabled || task.completed || task.dueDate <= 0L) {
            cancelTaskReminder(context, task.id)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (task.dueDate <= currentTime) {
            // Already overdue, don't schedule past alarm
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Remind 30 minutes before due date, or halfway if less than 30 minutes left
        val timeUntilDue = task.dueDate - currentTime
        val alertOffset = if (timeUntilDue > 30 * 60 * 1000L) 30 * 60 * 1000L else (timeUntilDue / 2)
        val triggerAtMillis = (task.dueDate - alertOffset).coerceAtLeast(currentTime + 5000L)

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskAlarmReceiver.EXTRA_IMPORTANCE, task.importance)
            putExtra(TaskAlarmReceiver.EXTRA_EFFORT, task.estimatedEffortMinutes)
            putExtra(TaskAlarmReceiver.EXTRA_PRIORITY_SCORE, task.priorityScore)
        }

        val requestCode = task.id.hashCode().absoluteValue
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            // Security or permission constraint
        }
    }

    fun cancelTaskReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val requestCode = taskId.hashCode().absoluteValue
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showTaskNotification(
        context: Context,
        taskId: String,
        title: String,
        importance: Int,
        effortMinutes: Int,
        priorityScore: Double
    ) {
        initNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val quadrant = PriorityEngine.getQuadrant(
            Task(
                id = taskId,
                title = title,
                importance = importance,
                estimatedEffortMinutes = effortMinutes,
                priorityScore = priorityScore
            )
        )

        val headerPrefix = when (quadrant) {
            PriorityQuadrant.DO_FIRST -> "🔥 Urgent Deadline"
            PriorityQuadrant.QUICK_WIN -> "⚡ Quick Win Ready"
            PriorityQuadrant.SCHEDULE -> "📅 High Priority Task"
            PriorityQuadrant.BACKLOG -> "📌 Task Reminder"
        }

        val bodyText = "$title • Estimated: ${effortMinutes}m • Priority: ${priorityScore.toInt()}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.taskflow_icon_1792741836104)
            .setContentTitle(headerPrefix)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = taskId.hashCode().absoluteValue
        notificationManager?.notify(notificationId, notification)
    }

    fun triggerPriorityBriefing(context: Context, activeTasks: List<Task>) {
        if (activeTasks.isEmpty()) return
        initNotificationChannel(context)

        val urgentTasks = activeTasks.filter { !it.completed }
            .sortedByDescending { it.priorityScore }
            .take(3)

        if (urgentTasks.isEmpty()) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryText = urgentTasks.joinToString(separator = "\n") {
            "• ${it.title} (Priority: ${it.priorityScore.toInt()})"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.taskflow_icon_1792741836104)
            .setContentTitle("🎯 TaskFlow Daily Focus Briefing")
            .setContentText("You have ${activeTasks.count { !it.completed }} active tasks. Top focus item: ${urgentTasks.first().title}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Top Focus for Today:\n$summaryText\n\nTap to open TaskFlow."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(9999, notification)
    }
}
