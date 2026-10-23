package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val importance: Int = 2, // 1: Low, 2: Medium, 3: High, 4: Urgent
    val estimatedEffortMinutes: Int = 30,
    val dueDate: Long = 0L, // Epoch ms, 0 if none
    val completed: Boolean = false,
    val category: String = "Personal",
    val priorityScore: Double = 0.0,
    val notificationEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
