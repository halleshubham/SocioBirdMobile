package com.postiz.mobile.ui.screens.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.data.remote.dto.PostDto
import com.postiz.mobile.ui.components.PlatformAvatar
import com.postiz.mobile.ui.components.SbCard
import com.postiz.mobile.ui.components.SbEmptyState
import com.postiz.mobile.ui.components.Shimmer
import com.postiz.mobile.ui.components.StatusChip
import com.postiz.mobile.util.Resource
import com.postiz.mobile.util.formatLocalDateTime
import com.postiz.mobile.util.formatLocalTime
import com.postiz.mobile.util.localDateOf
import com.postiz.mobile.util.stripHtml
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/** The calendar: week or month, filter by status and brand, post cards with swipe-to-delete and a detail sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsListScreen(
    onCreatePost: () -> Unit,
    onPostTap: (String) -> Unit,
    viewModel: PostsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val brands by viewModel.brands.collectAsState()
    val selectedBrandId by viewModel.selectedBrandId.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val hiddenIds by viewModel.hiddenIds.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<PostDto?>(null) }

    val allPosts = (state as? Resource.Success)?.data.orEmpty().filter { it.id !in hiddenIds }
    val filtered = allPosts.filter { statusFilter.matches(it.state) }
    val countsByDate = filtered.groupingBy { localDateOf(it.publishDate) }.eachCount()
    val dayPosts = filtered.filter { localDateOf(it.publishDate) == selectedDate }.sortedBy { it.publishDate }
    val failedCount = allPosts.count { it.state == "ERROR" }

    val pull = rememberPullToRefreshState()
    if (pull.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.load().join()
            pull.endRefresh()
        }
    }

    fun delete(post: PostDto) {
        viewModel.requestDelete(post)
        detail = null
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar("Post deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(post.id)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CalendarHeader(
                mode = mode,
                selectedDate = selectedDate,
                onModeChange = viewModel::setMode,
                onPrev = { viewModel.shift(-1) },
                onNext = { viewModel.shift(1) },
                onToday = viewModel::goToToday
            )

            if (mode == CalendarMode.WEEK) {
                WeekStrip(selectedDate, countsByDate, viewModel::selectDate)
            } else {
                MonthGrid(selectedDate, countsByDate, viewModel::selectDate)
            }

            FilterRow(
                statusFilter = statusFilter,
                failedCount = failedCount,
                onStatus = viewModel::selectStatus,
                brands = brands.map { it.id to it.name },
                selectedBrandId = selectedBrandId,
                onBrand = viewModel::selectBrand
            )

            Box(modifier = Modifier.weight(1f).nestedScroll(pull.nestedScrollConnection)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        DayHeading(selectedDate, if (state is Resource.Success) dayPosts.size else null)
                    }
                    when (val s = state) {
                        is Resource.Loading -> items(3) { Shimmer(Modifier.fillMaxWidth().height(96.dp)) }
                        is Resource.Error -> item {
                            SbCard(container = MaterialTheme.colorScheme.errorContainer) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Couldn't load your posts",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    TextButton(onClick = { viewModel.load() }) { Text("Try again") }
                                }
                            }
                        }
                        is Resource.Success -> {
                            if (dayPosts.isEmpty()) {
                                item {
                                    SbEmptyState(
                                        icon = Icons.Default.EventAvailable,
                                        title = if (statusFilter == StatusFilter.ALL) "Nothing on this day" else "No ${statusFilter.label.lowercase()} posts on this day",
                                        message = if (statusFilter == StatusFilter.ALL) "Plan a post and it will show up here." else "Try another day or switch the filter to All.",
                                        actionLabel = "New post",
                                        onAction = onCreatePost
                                    )
                                }
                            } else {
                                items(dayPosts, key = { it.id }) { post ->
                                    SwipeablePostCard(
                                        post = post,
                                        onClick = { detail = post },
                                        onDelete = { delete(post) }
                                    )
                                }
                            }
                        }
                    }
                }
                PullToRefreshContainer(state = pull, modifier = Modifier.align(Alignment.TopCenter))
            }
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }

    detail?.let { post ->
        PostDetailSheet(
            post = post,
            onDismiss = { detail = null },
            onAnalytics = {
                detail = null
                onPostTap(post.id)
            },
            onToggleStatus = {
                viewModel.toggleStatus(post)
                detail = null
            },
            onDelete = { delete(post) }
        )
    }

    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissActionError,
            title = { Text("Couldn't update your posts") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissActionError) { Text("OK") }
            }
        )
    }
}

@Composable
private fun CalendarHeader(
    mode: CalendarMode,
    selectedDate: LocalDate,
    onModeChange: (CalendarMode) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Calendar", style = MaterialTheme.typography.headlineMedium)
                Text(
                    periodLabel(mode, selectedDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
                TextButton(onClick = onToday) { Text("Today") }
                IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
            }
        }
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
            val options = listOf(CalendarMode.WEEK to "Week", CalendarMode.MONTH to "Month")
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = mode == value,
                    onClick = { onModeChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(label) }
                )
            }
        }
    }
}

private fun periodLabel(mode: CalendarMode, date: LocalDate): String =
    if (mode == CalendarMode.MONTH) {
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    } else {
        val start = date.with(DayOfWeek.MONDAY)
        val end = start.plusDays(6)
        val from = start.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
        val to = if (start.month == end.month) end.format(DateTimeFormatter.ofPattern("d", Locale.getDefault()))
        else end.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
        "$from – $to, ${end.year}"
    }

@Composable
private fun WeekStrip(selected: LocalDate, counts: Map<LocalDate?, Int>, onSelect: (LocalDate) -> Unit) {
    val start = selected.with(DayOfWeek.MONDAY)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        (0..6).forEach { offset ->
            val day = start.plusDays(offset.toLong())
            DayCell(day, selected, counts[day] ?: 0, showWeekday = true, onClick = { onSelect(day) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonthGrid(selected: LocalDate, counts: Map<LocalDate?, Int>, onSelect: (LocalDate) -> Unit) {
    val first = selected.withDayOfMonth(1)
    val gridStart = first.with(DayOfWeek.MONDAY)
    val weeks = (0 until 6)
        .map { week -> (0..6).map { day -> gridStart.plusDays((week * 7 + day).toLong()) } }
        .filter { week -> week.any { it.month == selected.month } }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { offset ->
                Text(
                    gridStart.plusDays(offset.toLong()).dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).take(2),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day, selected, counts[day] ?: 0,
                        showWeekday = false,
                        dimmed = day.month != selected.month,
                        onClick = { onSelect(day) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    selected: LocalDate,
    count: Int,
    showWeekday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false
) {
    val isSelected = day == selected
    val isToday = day == LocalDate.now()
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 4.dp).alpha(if (dimmed) 0.4f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showWeekday) {
            Text(
                day.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).take(3),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                        isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        if (count > 0) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterRow(
    statusFilter: StatusFilter,
    failedCount: Int,
    onStatus: (StatusFilter) -> Unit,
    brands: List<Pair<String, String>>,
    selectedBrandId: String?,
    onBrand: (String?) -> Unit
) {
    Column {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
        ) {
            items(StatusFilter.values().toList()) { filter ->
                val isFailed = filter == StatusFilter.FAILED
                FilterChip(
                    selected = statusFilter == filter,
                    onClick = { onStatus(filter) },
                    label = {
                        Text(if (isFailed && failedCount > 0) "${filter.label} · $failedCount" else filter.label)
                    },
                    colors = if (isFailed && failedCount > 0) {
                        androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            labelColor = MaterialTheme.colorScheme.error,
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        androidx.compose.material3.FilterChipDefaults.filterChipColors()
                    }
                )
            }
        }
        if (brands.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp)
            ) {
                item {
                    FilterChip(selected = selectedBrandId == null, onClick = { onBrand(null) }, label = { Text("All brands") })
                }
                items(brands, key = { it.first }) { (id, name) ->
                    FilterChip(selected = selectedBrandId == id, onClick = { onBrand(id) }, label = { Text(name, maxLines = 1) })
                }
            }
        }
    }
}

@Composable
private fun DayHeading(date: LocalDate, count: Int?) {
    val label = when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().plusDays(1) -> "Tomorrow"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
    }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            "$label · ${date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}",
            style = MaterialTheme.typography.titleMedium
        )
        if (count != null) {
            Text(
                if (count == 1) "1 post" else "$count posts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePostCard(post: PostDto, onClick: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        PostCard(post, onClick)
    }
}

@Composable
private fun PostCard(post: PostDto, onClick: () -> Unit) {
    SbCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            PlatformAvatar(
                picture = post.integration?.picture,
                name = post.integration?.name ?: "?",
                identifier = post.integration?.providerIdentifier,
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        formatLocalTime(post.publishDate),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusChip(post.state)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stripHtml(post.content).ifBlank { "(no text)" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                post.integration?.name?.let { name ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailSheet(
    post: PostDto,
    onDismiss: () -> Unit,
    onAnalytics: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformAvatar(
                    picture = post.integration?.picture,
                    name = post.integration?.name ?: "?",
                    identifier = post.integration?.providerIdentifier,
                    size = 48.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.integration?.name ?: "Channel", style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatLocalDateTime(post.publishDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(post.state)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stripHtml(post.content).ifBlank { "(no text)" },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState())
            )
            Spacer(Modifier.height(20.dp))

            if (post.state == "PUBLISHED") {
                Button(onClick = onAnalytics, modifier = Modifier.fillMaxWidth()) { Text("View analytics") }
                Spacer(Modifier.height(8.dp))
            }
            when (post.state) {
                "QUEUE" -> OutlinedButton(onClick = onToggleStatus, modifier = Modifier.fillMaxWidth()) { Text("Move to draft") }
                "DRAFT" -> FilledTonalButton(onClick = onToggleStatus, modifier = Modifier.fillMaxWidth()) { Text("Add to queue") }
                else -> Unit
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Delete post", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
