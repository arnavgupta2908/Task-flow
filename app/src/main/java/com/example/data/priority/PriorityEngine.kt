package com.example.data.priority

import com.example.data.model.Task
import kotlin.math.abs
import kotlin.math.min

enum class PriorityQuadrant(val label: String, val tag: String) {
    DO_FIRST("Do First", "urgent_important"),
    QUICK_WIN("Quick Win", "quick_impact"),
    SCHEDULE("Schedule", "deep_work"),
    BACKLOG("Backlog", "low_priority")
}

object PriorityEngine {

    /**
     * Calculates an intelligent priority score (0.0 to 150.0+) based on:
     * - Deadline urgency (hours remaining / overdue penalty)
     * - Importance weight (1..4)
     * - Estimated effort (quick-win boost vs deep-work scheduling)
     */
    fun calculateScore(
        importance: Int,
        estimatedEffortMinutes: Int,
        dueDate: Long,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Double {
        // 1. Importance baseline (10 to 90 points)
        val importanceScore = when (importance) {
            4 -> 90.0 // Urgent
            3 -> 60.0 // High
            2 -> 35.0 // Medium
            else -> 15.0 // Low
        }

        // 2. Deadline Urgency (0 to 100+ points)
        val urgencyScore = if (dueDate <= 0L) {
            10.0 // No deadline
        } else {
            val millisRemaining = dueDate - currentTimeMillis
            val hoursRemaining = millisRemaining / 3_600_000.0

            when {
                hoursRemaining < 0 -> {
                    // Overdue: Heavy penalty scaling with how overdue it is
                    100.0 + min(abs(hoursRemaining), 72.0) * 0.5
                }
                hoursRemaining <= 4.0 -> 95.0
                hoursRemaining <= 12.0 -> 85.0
                hoursRemaining <= 24.0 -> 72.0
                hoursRemaining <= 48.0 -> 55.0
                hoursRemaining <= 168.0 -> 35.0 // within a week
                else -> 18.0
            }
        }

        // 3. Effort Efficiency Adjustment:
        // Short high-impact tasks get a "Quick Win" momentum boost (+12..+20 pts)
        // Moderate tasks (30-60m) get a small boost (+6 pts)
        // Heavy tasks (>120m) get slight decay if far out, so they don't block quick wins
        val effortBonus = when {
            estimatedEffortMinutes <= 15 -> 18.0
            estimatedEffortMinutes <= 30 -> 12.0
            estimatedEffortMinutes <= 60 -> 6.0
            estimatedEffortMinutes <= 120 -> 0.0
            else -> -4.0
        }

        val total = (urgencyScore * 0.52) + (importanceScore * 0.36) + effortBonus
        return (Math.round(total * 10.0) / 10.0).coerceAtLeast(0.0)
    }

    /**
     * Categorizes a task into an actionable quadrant for the user.
     */
    fun getQuadrant(task: Task, currentTimeMillis: Long = System.currentTimeMillis()): PriorityQuadrant {
        val isDueSoonOrOverdue = if (task.dueDate > 0L) {
            (task.dueDate - currentTimeMillis) <= 24 * 3600 * 1000L
        } else false

        val isHighImportance = task.importance >= 3
        val isQuickEffort = task.estimatedEffortMinutes <= 30

        return when {
            isDueSoonOrOverdue && isHighImportance -> PriorityQuadrant.DO_FIRST
            isQuickEffort && isHighImportance -> PriorityQuadrant.QUICK_WIN
            isHighImportance -> PriorityQuadrant.SCHEDULE
            isDueSoonOrOverdue -> PriorityQuadrant.DO_FIRST
            isQuickEffort -> PriorityQuadrant.QUICK_WIN
            else -> PriorityQuadrant.BACKLOG
        }
    }
}
