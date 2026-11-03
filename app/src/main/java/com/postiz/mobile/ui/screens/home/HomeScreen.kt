package com.postiz.mobile.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.postiz.mobile.data.remote.dto.PostDto
import com.postiz.mobile.ui.components.PlatformAvatar
import com.postiz.mobile.ui.components.SbCard
import com.postiz.mobile.ui.components.SbEmptyState
import com.postiz.mobile.ui.components.SectionHeader
import com.postiz.mobile.ui.components.Shimmer
import com.postiz.mobile.ui.components.StatusChip
import com.postiz.mobile.util.Resource
import com.postiz.mobile.util.formatLocalSchedule
import com.postiz.mobile.util.stripHtml
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/** Landing screen after sign-in: what needs attention, what is going out next, how the week looks. */
@Composable
fun HomeScreen(
    onCreatePost: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenChannels: () -> Unit,
    onAddChannel: () -> Unit,
    onPostTap: (PostDto) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Coming back from the composer or the channel flow: refresh so Home never shows stale counts.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { HomeHeader(onRefresh = viewModel::load) }

        when (val s = state) {
            is Resource.Loading -> item { HomeSkeleton() }
            is Resource.Error -> item {
                SbCard(container = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Couldn't load your overview",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = viewModel::load) { Text("Try again") }
                    }
                }
            }
            is Resource.Success -> {
                val ui = s.data
                if (ui.isGettingStarted) {
                    item { GettingStartedCard(ui, onAddChannel = onAddChannel, onCreatePost = onCreatePost) }
                }
                if (ui.needsAttention) {
                    item { NeedsAttentionCard(ui, onOpenCalendar = onOpenCalendar, onOpenChannels = onOpenChannels) }
                }
                item {
                    Column {
                        SectionHeader("Up next", actionLabel = "Calendar", onAction = onOpenCalendar)
                        Spacer(Modifier.height(8.dp))
                        if (ui.upNext.isEmpty()) {
                            SbCard {
                                SbEmptyState(
                                    icon = Icons.Default.EditNote,
                                    title = "Nothing scheduled",
                                    message = "Write a post and schedule it. It will show up here.",
                                    actionLabel = "New post",
                                    onAction = onCreatePost
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ui.upNext.forEach { post -> UpNextCard(post, onClick = { onPostTap(post) }) }
                            }
                        }
                    }
                }
                item { WeekCard(ui) }
                item { QuickActions(onCreatePost, onAddChannel, onOpenCalendar) }
                if (ui.channels.isNotEmpty()) {
                    item {
                        Column {
                            SectionHeader("Your channels", actionLabel = "Manage", onAction = onOpenChannels)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                                items(ui.channels, key = { it.id }) { channel ->
                                    Column(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .clickable(onClick = onOpenChannels)
                                            .alpha(if (channel.disabled) 0.45f else 1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        PlatformAvatar(channel.picture, channel.name, channel.identifier, size = 52.dp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            channel.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onRefresh: () -> Unit) {
    val now = LocalTime.now().hour
    val greeting = when {
        now < 12 -> "Good morning"
        now < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(greeting, style = MaterialTheme.typography.headlineMedium)
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Shimmer(Modifier.fillMaxWidth().height(88.dp))
        Shimmer(Modifier.fillMaxWidth().height(88.dp))
        Shimmer(Modifier.fillMaxWidth().height(88.dp))
        Shimmer(Modifier.fillMaxWidth().height(120.dp))
    }
}

@Composable
private fun GettingStartedCard(ui: HomeUi, onAddChannel: () -> Unit, onCreatePost: () -> Unit) {
    val channelDone = ui.channels.isNotEmpty()
    val postDone = ui.hasAnyPost
    val scheduledDone = ui.hasScheduledOrPublished
    SbCard(container = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Get started",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Three steps to your first scheduled post.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            ChecklistRow("Connect a channel", channelDone, if (channelDone) null else "Connect", onAddChannel)
            ChecklistRow("Write your first post", postDone, if (!postDone && channelDone) "Write" else null, onCreatePost)
            ChecklistRow("Schedule it", scheduledDone, if (postDone && !scheduledDone) "Schedule" else null, onCreatePost)
        }
    }
}

@Composable
private fun ChecklistRow(label: String, done: Boolean, actionLabel: String?, onAction: () -> Unit) {
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (done) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (done) "Done" else "Not done",
            tint = if (done) MaterialTheme.colorScheme.primary else content.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (done) content.copy(alpha = 0.6f) else content,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null) {
            FilledTonalButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun NeedsAttentionCard(ui: HomeUi, onOpenCalendar: () -> Unit, onOpenChannels: () -> Unit) {
    SbCard(container = MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(8.dp))
                Text("Needs attention", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            if (ui.failed.isNotEmpty()) {
                AttentionRow(
                    text = if (ui.failed.size == 1) "1 post failed to publish" else "${ui.failed.size} posts failed to publish",
                    action = "Review",
                    onClick = onOpenCalendar
                )
            }
            if (ui.disabledChannels.isNotEmpty()) {
                AttentionRow(
                    text = if (ui.disabledChannels.size == 1) "1 channel is disabled" else "${ui.disabledChannels.size} channels are disabled",
                    action = "Fix",
                    onClick = onOpenChannels
                )
            }
        }
    }
}

@Composable
private fun AttentionRow(text: String, action: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action, color = MaterialTheme.colorScheme.onErrorContainer) }
    }
}

@Composable
private fun UpNextCard(post: PostDto, onClick: () -> Unit) {
    SbCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlatformAvatar(
                picture = post.integration?.picture,
                name = post.integration?.name ?: "?",
                identifier = post.integration?.providerIdentifier,
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stripHtml(post.content).ifBlank { "(no text)" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(post.integration?.name, formatLocalSchedule(post.publishDate)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(post.state)
        }
    }
}

@Composable
private fun WeekCard(ui: HomeUi) {
    SbCard {
        Column(Modifier.padding(16.dp)) {
            Text("This week", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Scheduled", ui.scheduledThisWeek)
                Stat("Published", ui.publishedLast7Days)
                Stat("Drafts", ui.drafts)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ui.week.forEachIndexed { index, day ->
                    val busy = day.count > 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            day.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).take(3),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .then(
                                    if (busy) Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                    else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (busy) day.count.toString() else day.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (busy) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index == 0) {
                            Text("Today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickActions(onCreatePost: () -> Unit, onAddChannel: () -> Unit, onOpenCalendar: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickAction("New post", Icons.Default.Add, onCreatePost, Modifier.weight(1f), primary = true)
        QuickAction("Add channel", Icons.Default.Hub, onAddChannel, Modifier.weight(1f))
        QuickAction("Calendar", Icons.Default.CalendarMonth, onOpenCalendar, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier, primary: Boolean = false) {
    SbCard(
        modifier = modifier,
        onClick = onClick,
        container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
