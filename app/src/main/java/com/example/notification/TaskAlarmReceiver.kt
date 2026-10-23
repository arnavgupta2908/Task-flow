package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val importance = intent.getIntExtra(EXTRA_IMPORTANCE, 2)
        val effort = intent.getIntExtra(EXTRA_EFFORT, 30)
        val priorityScore = intent.getDoubleExtra(EXTRA_PRIORITY_SCORE, 50.0)

        TaskNotificationManager.showTaskNotification(
            context = context,
            taskId = taskId,
            title = title,
            importance = importance,
            effortMinutes = effort,
            priorityScore = priorityScore
        )
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_IMPORTANCE = "extra_importance"
        const val EXTRA_EFFORT = "extra_effort"
        const val EXTRA_PRIORITY_SCORE = "extra_priority_score"
    }
}
