package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Task
import com.example.data.priority.PriorityEngine
import com.example.data.priority.PriorityQuadrant
import com.example.data.repository.TaskRepository
import com.example.notification.TaskNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TasksUiState {
    data object Loading : TasksUiState
    data class Success(val tasks: List<Task>) : TasksUiState
    data class Error(val message: String) : TasksUiState
}

enum class SortOption(val displayName: String) {
    SMART_PRIORITY("Intelligent Priority"),
    DEADLINE("Soonest Deadline"),
    IMPORTANCE("Highest Importance"),
    EFFORT("Quickest Effort")
}

data class PrioritySummary(
    val totalActive: Int = 0,
    val doFirstCount: Int = 0,
    val quickWinCount: Int = 0,
    val scheduleCount: Int = 0,
    val overdueCount: Int = 0,
    val totalEstimatedMinutes: Int = 0,
    val nextUpcomingTask: Task? = null
)

data class TaskFilters(
    val sortOption: SortOption = SortOption.SMART_PRIORITY,
    val selectedQuadrant: PriorityQuadrant? = null,
    val selectedCategory: String? = "All",
    val searchQuery: String = "",
    val hideCompleted: Boolean = false
)

class TaskViewModel(
    private val repository: TaskRepository,
    val currentUserId: String
) : ViewModel() {

    private val _filters = MutableStateFlow(TaskFilters())
    val filters: StateFlow<TaskFilters> = _filters.asStateFlow()

    // Expose individual convenience properties for UI bindings
    val sortOption: StateFlow<SortOption> = _filters.map { it.sortOption }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SortOption.SMART_PRIORITY)

    val selectedQuadrant: StateFlow<PriorityQuadrant?> = _filters.map { it.selectedQuadrant }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val selectedCategory: StateFlow<String?> = _filters.map { it.selectedCategory }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "All")

    val searchQuery: StateFlow<String> = _filters.map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    val hideCompleted: StateFlow<Boolean> = _filters.map { it.hideCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Upstream Firestore snapshot observation
    val rawTasksState: StateFlow<TasksUiState> = repository.observeTasks()
        .map<List<Task>, TasksUiState> { TasksUiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing tasks", error)
            emit(TasksUiState.Error(error.localizedMessage ?: "Failed to sync tasks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = TasksUiState.Loading
        )

    // Filtered and intelligently sorted tasks
    val filteredTasks: StateFlow<List<Task>> = combine(
        rawTasksState,
        _filters
    ) { rawState: TasksUiState, currentFilters: TaskFilters ->
        if (rawState !is TasksUiState.Success) {
            emptyList()
        } else {
            val now = System.currentTimeMillis()
            var list = rawState.tasks

            if (currentFilters.hideCompleted) {
                list = list.filter { !it.completed }
            }

            if (currentFilters.selectedQuadrant != null) {
                list = list.filter { PriorityEngine.getQuadrant(it, now) == currentFilters.selectedQuadrant }
            }

            if (!currentFilters.selectedCategory.isNullOrBlank() && currentFilters.selectedCategory != "All") {
                list = list.filter { it.category.equals(currentFilters.selectedCategory, ignoreCase = true) }
            }

            if (currentFilters.searchQuery.isNotBlank()) {
                val q = currentFilters.searchQuery.trim().lowercase()
                list = list.filter {
                    it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
                }
            }

            // Apply dynamic intelligent sorting
            when (currentFilters.sortOption) {
                SortOption.SMART_PRIORITY -> {
                    list.sortedWith(
                        compareByDescending<Task> { !it.completed }
                            .thenByDescending {
                                PriorityEngine.calculateScore(
                                    it.importance,
                                    it.estimatedEffortMinutes,
                                    it.dueDate,
                                    now
                                )
                            }
                    )
                }
                SortOption.DEADLINE -> {
                    list.sortedWith(
                        compareByDescending<Task> { !it.completed }
                            .thenBy { if (it.dueDate <= 0L) Long.MAX_VALUE else it.dueDate }
                    )
                }
                SortOption.IMPORTANCE -> {
                    list.sortedWith(
                        compareByDescending<Task> { !it.completed }
                            .thenByDescending { it.importance }
                    )
                }
                SortOption.EFFORT -> {
                    list.sortedWith(
                        compareByDescending<Task> { !it.completed }
                            .thenBy { it.estimatedEffortMinutes }
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Summary analytics for dashboard header
    val prioritySummary: StateFlow<PrioritySummary> = rawTasksState.map { state ->
        if (state !is TasksUiState.Success) {
            PrioritySummary()
        } else {
            val now = System.currentTimeMillis()
            val active = state.tasks.filter { !it.completed }
            var doFirst = 0
            var quickWin = 0
            var schedule = 0
            var overdue = 0
            var totalMinutes = 0

            active.forEach { task ->
                totalMinutes += task.estimatedEffortMinutes
                if (task.dueDate in 1 until now) {
                    overdue++
                }
                when (PriorityEngine.getQuadrant(task, now)) {
                    PriorityQuadrant.DO_FIRST -> doFirst++
                    PriorityQuadrant.QUICK_WIN -> quickWin++
                    PriorityQuadrant.SCHEDULE -> schedule++
                    PriorityQuadrant.BACKLOG -> {}
                }
            }

            val nextTask = active.filter { it.dueDate > 0L }
                .minByOrNull { it.dueDate }

            PrioritySummary(
                totalActive = active.size,
                doFirstCount = doFirst,
                quickWinCount = quickWin,
                scheduleCount = schedule,
                overdueCount = overdue,
                totalEstimatedMinutes = totalMinutes,
                nextUpcomingTask = nextTask
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PrioritySummary()
    )

    fun setSortOption(option: SortOption) {
        _filters.update { it.copy(sortOption = option) }
    }

    fun setQuadrant(quadrant: PriorityQuadrant?) {
        _filters.update {
            it.copy(selectedQuadrant = if (it.selectedQuadrant == quadrant) null else quadrant)
        }
    }

    fun setCategory(category: String?) {
        _filters.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _filters.update { it.copy(searchQuery = query) }
    }

    fun setHideCompleted(hide: Boolean) {
        _filters.update { it.copy(hideCompleted = hide) }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun createTask(
        context: Context,
        title: String,
        description: String,
        importance: Int,
        estimatedEffortMinutes: Int,
        dueDate: Long,
        category: String,
        notificationEnabled: Boolean
    ) {
        if (title.isBlank()) {
            _userMessage.value = "Title cannot be empty"
            return
        }

        viewModelScope.launch {
            val result = repository.createTask(
                title = title,
                description = description,
                importance = importance,
                estimatedEffortMinutes = estimatedEffortMinutes,
                dueDate = dueDate,
                category = category,
                notificationEnabled = notificationEnabled
            )
            result.onSuccess { taskId ->
                _userMessage.value = "Task created with priority score ${
                    PriorityEngine.calculateScore(importance, estimatedEffortMinutes, dueDate).toInt()
                }"
                if (notificationEnabled && dueDate > System.currentTimeMillis()) {
                    val task = Task(
                        id = taskId,
                        title = title,
                        importance = importance,
                        estimatedEffortMinutes = estimatedEffortMinutes,
                        dueDate = dueDate,
                        notificationEnabled = true
                    )
                    TaskNotificationManager.scheduleTaskReminder(context, task)
                }
            }.onFailure { error ->
                _userMessage.value = error.localizedMessage ?: "Failed to create task"
            }
        }
    }

    fun updateTask(context: Context, task: Task) {
        viewModelScope.launch {
            val result = repository.updateTask(task)
            result.onSuccess {
                _userMessage.value = "Task updated"
                if (task.notificationEnabled && !task.completed && task.dueDate > System.currentTimeMillis()) {
                    TaskNotificationManager.scheduleTaskReminder(context, task)
                } else {
                    TaskNotificationManager.cancelTaskReminder(context, task.id)
                }
            }.onFailure { error ->
                _userMessage.value = error.localizedMessage ?: "Failed to update task"
            }
        }
    }

    fun toggleCompleted(context: Context, task: Task) {
        viewModelScope.launch {
            val result = repository.toggleTaskCompleted(task)
            result.onSuccess {
                val newStatus = !task.completed
                if (newStatus) {
                    _userMessage.value = "Task marked completed! Great job!"
                    TaskNotificationManager.cancelTaskReminder(context, task.id)
                } else {
                    _userMessage.value = "Task marked incomplete"
                    if (task.notificationEnabled && task.dueDate > System.currentTimeMillis()) {
                        TaskNotificationManager.scheduleTaskReminder(context, task)
                    }
                }
            }.onFailure { error ->
                _userMessage.value = error.localizedMessage ?: "Failed to update status"
            }
        }
    }

    fun deleteTask(context: Context, taskId: String) {
        viewModelScope.launch {
            val result = repository.deleteTask(taskId)
            result.onSuccess {
                _userMessage.value = "Task deleted"
                TaskNotificationManager.cancelTaskReminder(context, taskId)
            }.onFailure { error ->
                _userMessage.value = error.localizedMessage ?: "Failed to delete task"
            }
        }
    }

    fun triggerDailyBriefingNotification(context: Context) {
        val state = rawTasksState.value
        if (state is TasksUiState.Success) {
            TaskNotificationManager.triggerPriorityBriefing(context, state.tasks)
            _userMessage.value = "Focus briefing alert sent"
        }
    }

    companion object {
        private const val TAG = "TaskViewModel"
    }
}
