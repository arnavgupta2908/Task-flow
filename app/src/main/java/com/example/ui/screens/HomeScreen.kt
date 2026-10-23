package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Task
import com.example.data.priority.PriorityEngine
import com.example.data.priority.PriorityQuadrant
import com.example.ui.components.CreateEditTaskDialog
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.formatDeadline
import com.example.ui.components.formatEffort
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent
import com.example.ui.viewmodel.PrioritySummary
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.TaskViewModel
import com.example.ui.viewmodel.TasksUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val rawState by viewModel.rawTasksState.collectAsStateWithLifecycle()
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val summary by viewModel.prioritySummary.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val selectedQuadrant by viewModel.selectedQuadrant.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val hideCompleted by viewModel.hideCompleted.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TaskFlow",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Cloud Synced",
                                    tint = PriorityLow,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cloud Synced",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PriorityLow
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Smart Daily Focus Briefing Button
                    IconButton(
                        onClick = { viewModel.triggerDailyBriefingNotification(context) },
                        modifier = Modifier.testTag("daily_briefing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Trigger Focus Briefing",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Sign Out Action
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Task",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Priority Summary Dashboard Card
            item {
                PrioritySummaryHeader(
                    summary = summary,
                    onQuadrantSelected = { viewModel.setQuadrant(it) },
                    selectedQuadrant = selectedQuadrant
                )
            }

            // Search & Sort Bar
            item {
                SearchAndFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    sortOption = sortOption,
                    onSortOptionChange = { viewModel.setSortOption(it) },
                    hideCompleted = hideCompleted,
                    onHideCompletedToggle = { viewModel.setHideCompleted(it) },
                    showSortMenu = showSortMenu,
                    onToggleSortMenu = { showSortMenu = it }
                )
            }

            // Category Chips Row
            item {
                CategoryChipsRow(
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.setCategory(it) }
                )
            }

            // Tasks List
            when (val state = rawState) {
                is TasksUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is TasksUiState.Error -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = "Sync error: ${state.message}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is TasksUiState.Success -> {
                    if (tasks.isEmpty()) {
                        item {
                            EmptyTasksPlaceholder(
                                hasFilters = selectedQuadrant != null || selectedCategory != "All" || searchQuery.isNotBlank(),
                                onAddTask = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(tasks, key = { it.id }) { task ->
                            TaskItemCard(
                                task = task,
                                onToggleCompleted = { viewModel.toggleCompleted(context, task) },
                                onEdit = { taskToEdit = task },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateEditTaskDialog(
            initialTask = null,
            onDismiss = { showCreateDialog = false },
            onSave = { title, desc, imp, effort, due, cat, notif ->
                viewModel.createTask(context, title, desc, imp, effort, due, cat, notif)
                showCreateDialog = false
            }
        )
    }

    taskToEdit?.let { task ->
        CreateEditTaskDialog(
            initialTask = task,
            onDismiss = { taskToEdit = null },
            onSave = { title, desc, imp, effort, due, cat, notif ->
                viewModel.updateTask(
                    context,
                    task.copy(
                        title = title,
                        description = desc,
                        importance = imp,
                        estimatedEffortMinutes = effort,
                        dueDate = due,
                        category = cat,
                        notificationEnabled = notif
                    )
                )
                taskToEdit = null
            }
        )
    }

    taskToDelete?.let { task ->
        DeleteConfirmationDialog(
            taskTitle = task.title,
            onConfirm = {
                viewModel.deleteTask(context, task.id)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
}

@Composable
fun PrioritySummaryHeader(
    summary: PrioritySummary,
    selectedQuadrant: PriorityQuadrant?,
    onQuadrantSelected: (PriorityQuadrant?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Next Deadline Spotlight
            if (summary.nextUpcomingTask != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Next Deadline: ${summary.nextUpcomingTask.title} (${formatDeadline(summary.nextUpcomingTask.dueDate)})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Summary metrics header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Intelligent Workload (${summary.totalActive} Active)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "⏱️ ${formatEffort(summary.totalEstimatedMinutes)} focus needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quadrant Breakdown Pills (Interactive filters)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuadrantSummaryCard(
                    title = "Do First",
                    count = summary.doFirstCount,
                    icon = "🔥",
                    color = PriorityUrgent,
                    isSelected = selectedQuadrant == PriorityQuadrant.DO_FIRST,
                    modifier = Modifier.weight(1f),
                    onClick = { onQuadrantSelected(PriorityQuadrant.DO_FIRST) }
                )
                QuadrantSummaryCard(
                    title = "Quick Wins",
                    count = summary.quickWinCount,
                    icon = "⚡",
                    color = PriorityLow,
                    isSelected = selectedQuadrant == PriorityQuadrant.QUICK_WIN,
                    modifier = Modifier.weight(1f),
                    onClick = { onQuadrantSelected(PriorityQuadrant.QUICK_WIN) }
                )
                QuadrantSummaryCard(
                    title = "Schedule",
                    count = summary.scheduleCount,
                    icon = "📅",
                    color = MaterialTheme.colorScheme.primary,
                    isSelected = selectedQuadrant == PriorityQuadrant.SCHEDULE,
                    modifier = Modifier.weight(1f),
                    onClick = { onQuadrantSelected(PriorityQuadrant.SCHEDULE) }
                )
            }
        }
    }
}

@Composable
fun QuadrantSummaryCard(
    title: String,
    count: Int,
    icon: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    hideCompleted: Boolean,
    onHideCompletedToggle: (Boolean) -> Unit,
    showSortMenu: Boolean,
    onToggleSortMenu: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search tasks...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                } else null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_tasks_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Dropdown Button
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleSortMenu(true) }
                        .testTag("sort_menu_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onToggleSortMenu(false) }
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.displayName,
                                    fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onSortOptionChange(option)
                                onToggleSortMenu(false)
                            },
                            leadingIcon = if (sortOption == option) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hide completed toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Sorted by: ${sortOption.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hide Completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = hideCompleted,
                    onCheckedChange = onHideCompletedToggle,
                    modifier = Modifier.size(height = 24.dp, width = 40.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryChipsRow(
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit
) {
    val categories = listOf("All", "Personal", "Work", "Health", "Study", "Finance", "Urgent")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = selectedCategory == cat
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(cat) },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val quadrant = PriorityEngine.getQuadrant(task)
    val score = task.priorityScore.toInt()

    val priorityColor = when {
        task.completed -> Color(0xFF94A3B8)
        score >= 85 -> PriorityUrgent
        score >= 65 -> PriorityHigh
        score >= 45 -> PriorityMedium
        else -> PriorityLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.completed) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Checkbox, Title, Priority Score Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Interactive Checkbox
                IconButton(
                    onClick = onToggleCompleted,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("checkbox_${task.id}")
                ) {
                    Icon(
                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (task.completed) "Mark Incomplete" else "Mark Complete",
                        tint = if (task.completed) PriorityLow else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Priority Score Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = priorityColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, priorityColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Score $score",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Row: Quadrant chip, Deadline, Effort, Category, Notifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quadrant Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (quadrant) {
                            PriorityQuadrant.DO_FIRST -> PriorityUrgent.copy(alpha = 0.15f)
                            PriorityQuadrant.QUICK_WIN -> PriorityLow.copy(alpha = 0.15f)
                            PriorityQuadrant.SCHEDULE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            PriorityQuadrant.BACKLOG -> Color(0xFF64748B).copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = quadrant.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (quadrant) {
                                PriorityQuadrant.DO_FIRST -> PriorityUrgent
                                PriorityQuadrant.QUICK_WIN -> PriorityLow
                                PriorityQuadrant.SCHEDULE -> MaterialTheme.colorScheme.primary
                                PriorityQuadrant.BACKLOG -> Color(0xFF475569)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Estimated Effort
                    Text(
                        text = "⏱️ ${formatEffort(task.estimatedEffortMinutes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Deadline
                    if (task.dueDate > 0L) {
                        val isOverdue = !task.completed && task.dueDate < System.currentTimeMillis()
                        Text(
                            text = "📅 ${formatDeadline(task.dueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverdue) PriorityUrgent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Smart Notification indicator
                    if (task.notificationEnabled) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notification Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Edit and Delete Buttons
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_task_${task.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Task",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_task_${task.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Task",
                            modifier = Modifier.size(16.dp),
                            tint = PriorityUrgent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTasksPlaceholder(
    hasFilters: Boolean,
    onAddTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasFilters) "No tasks match your filters" else "All caught up! Nothing to prioritize.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (hasFilters) "Try resetting category or quadrant filters to view more tasks." else "Add tasks with deadlines, importance, and effort to start organizing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        androidx.compose.material3.Button(
            onClick = onAddTask,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create First Task")
        }
    }
}
