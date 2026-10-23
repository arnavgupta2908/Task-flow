package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Task
import com.example.data.priority.PriorityEngine
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TaskRepository(
    private val db: FirebaseFirestore
) {
    // Secondary constructor resolving named database ID from string resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Realtime observation of all tasks belonging to the signed-in user.
     */
    fun observeTasks(): Flow<List<Task>> = flow {
        val uid = requireUserId()
        val path = "tasks"
        val queryFlow = db.collection(path)
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
        emitAll(queryFlow)
    }

    /**
     * Fetch a single task by ID.
     */
    suspend fun getTaskById(taskId: String): Result<Task> {
        val uid = requireUserId()
        val docRef = db.collection("tasks").document(taskId)
        return try {
            val snapshot = docRef.get().await()
            val task = snapshot.toObject(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (task == null) {
                Result.failure(NoSuchElementException("Task not found"))
            } else if (task.userId != uid) {
                Result.failure(SecurityException("Unauthorized access to task"))
            } else {
                Result.success(task)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Create a new task with intelligent priority scoring and server timestamps.
     */
    suspend fun createTask(
        title: String,
        description: String = "",
        importance: Int = 2,
        estimatedEffortMinutes: Int = 30,
        dueDate: Long = 0L,
        category: String = "Personal",
        notificationEnabled: Boolean = true
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val calculatedScore = PriorityEngine.calculateScore(
            importance = importance,
            estimatedEffortMinutes = estimatedEffortMinutes,
            dueDate = dueDate
        )

        val tasksRef = db.collection("tasks")
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim(),
            "description" to description.trim(),
            "importance" to importance,
            "estimatedEffortMinutes" to estimatedEffortMinutes,
            "dueDate" to dueDate,
            "completed" to false,
            "category" to category.trim().ifEmpty { "Personal" },
            "priorityScore" to calculatedScore,
            "notificationEnabled" to notificationEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val docRef = tasksRef.add(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, tasksRef.path)
            Result.failure(e)
        }
    }

    /**
     * Update an existing task.
     */
    suspend fun updateTask(task: Task): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        requireNotNull(task.createdAt) { "Task createdAt must not be null for update" }

        val newScore = PriorityEngine.calculateScore(
            importance = task.importance,
            estimatedEffortMinutes = task.estimatedEffortMinutes,
            dueDate = task.dueDate
        )

        val docRef = db.collection("tasks").document(task.id)
        val payload = mapOf<String, Any>(
            "userId" to uid,
            "title" to task.title.trim(),
            "description" to task.description.trim(),
            "importance" to task.importance,
            "estimatedEffortMinutes" to task.estimatedEffortMinutes,
            "dueDate" to task.dueDate,
            "completed" to task.completed,
            "category" to task.category.trim().ifEmpty { "Personal" },
            "priorityScore" to newScore,
            "notificationEnabled" to task.notificationEnabled,
            "createdAt" to task.createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Toggle completion status of a task.
     */
    suspend fun toggleTaskCompleted(task: Task): Result<Unit> {
        return updateTask(task.copy(completed = !task.completed))
    }

    /**
     * Delete a task.
     */
    suspend fun deleteTask(taskId: String): Result<Unit> {
        val docRef = db.collection("tasks").document(taskId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
