package com.postiz.mobile.ui.screens.integrations

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.postiz.mobile.data.remote.dto.IntegrationDto
import com.postiz.mobile.ui.components.PlatformAvatar
import com.postiz.mobile.ui.components.SbCard
import com.postiz.mobile.ui.components.SbEmptyState
import com.postiz.mobile.ui.components.Shimmer
import com.postiz.mobile.ui.components.providerDisplayName
import com.postiz.mobile.util.Resource

/** Connected channels as cards, grouped by brand, with status and a menu (analytics, disconnect). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(
    onAddChannel: () -> Unit,
    onChannelTap: (String) -> Unit,
    viewModel: IntegrationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val disconnectError by viewModel.disconnectError.collectAsState()
    var confirmDisconnect by remember { mutableStateOf<IntegrationDto?>(null) }

    // Connecting a channel happens in the browser (Add channel sits above this screen on the back stack), and this
    // ViewModel survives that round trip, so refresh every time the screen becomes visible again.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    val pull = rememberPullToRefreshState()
    if (pull.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.load().join()
            pull.endRefresh()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Channels", style = MaterialTheme.typography.headlineMedium)
                val count = (state as? Resource.Success)?.data?.size
                if (count != null) {
                    Text(
                        if (count == 1) "1 connected" else "$count connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalButton(onClick = onAddChannel) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add channel")
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).clipToBounds().nestedScroll(pull.nestedScrollConnection)) {
            when (val s = state) {
                is Resource.Loading -> Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { Shimmer(Modifier.fillMaxWidth().height(76.dp)) }
                }
                is Resource.Error -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
                    item {
                        SbCard(container = MaterialTheme.colorScheme.errorContainer) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Couldn't load your channels", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                TextButton(onClick = { viewModel.load() }) { Text("Try again") }
                            }
                        }
                    }
                }
                is Resource.Success -> {
                    if (s.data.isEmpty()) {
                        LazyColumn(Modifier.fillMaxSize()) {
                            item {
                                SbEmptyState(
                                    icon = Icons.Default.Hub,
                                    title = "No channels yet",
                                    message = "Connect your first channel to start scheduling posts.",
                                    modifier = Modifier.padding(top = 48.dp),
                                    actionLabel = "Add channel",
                                    onAction = onAddChannel
                                )
                            }
                        }
                    } else {
                        val groups = s.data.groupBy { it.customer?.name }
                        val showHeaders = groups.keys.any { it != null }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            groups.entries
                                .sortedWith(compareBy({ it.key == null }, { it.key?.lowercase() }))
                                .forEach { (brand, channels) ->
                                    if (showHeaders) {
                                        item(key = "header-${brand ?: "none"}") {
                                            Text(
                                                brand ?: "No brand",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 10.dp)
                                            )
                                        }
                                    }
                                    items(channels, key = { it.id }) { channel ->
                                        ChannelCard(
                                            channel = channel,
                                            onTap = { onChannelTap(channel.id) },
                                            onDisconnect = { confirmDisconnect = channel }
                                        )
                                    }
                                }
                        }
                    }
                }
            }
            if (pull.progress > 0f || pull.isRefreshing) {
                PullToRefreshContainer(state = pull, modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }

    confirmDisconnect?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDisconnect = null },
            title = { Text("Disconnect ${target.name}?") },
            text = { Text("Its scheduled posts on this channel will be removed too. This can't be undone from the app.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.disconnect(target.id)
                    confirmDisconnect = null
                }) { Text("Disconnect", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = null }) { Text("Cancel") }
            }
        )
    }

    disconnectError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDisconnectError,
            title = { Text("Something went wrong") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDisconnectError) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ChannelCard(channel: IntegrationDto, onTap: () -> Unit, onDisconnect: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    SbCard(modifier = Modifier.fillMaxWidth(), onClick = onTap) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlatformAvatar(
                picture = channel.picture,
                name = channel.name,
                identifier = channel.identifier,
                size = 48.dp,
                modifier = Modifier.alpha(if (channel.disabled) 0.5f else 1f)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (channel.disabled) scheme.onSurfaceVariant else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    providerDisplayName(channel.identifier),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            ChannelStatus(disabled = channel.disabled)
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More for ${channel.name}", tint = scheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("View analytics") }, onClick = { menuOpen = false; onTap() })
                    DropdownMenuItem(
                        text = { Text("Disconnect", color = scheme.error) },
                        onClick = { menuOpen = false; onDisconnect() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelStatus(disabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = if (disabled) "Disabled" else "Active",
        style = MaterialTheme.typography.labelMedium,
        color = if (disabled) scheme.onSurfaceVariant else scheme.onTertiaryContainer,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (disabled) scheme.surfaceContainerHigh else scheme.tertiaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
