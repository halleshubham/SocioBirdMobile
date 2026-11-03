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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import com.postiz.mobile.data.remote.dto.IntegrationDto
import com.postiz.mobile.ui.components.EmptyState
import com.postiz.mobile.ui.components.FullScreenError
import com.postiz.mobile.ui.components.FullScreenLoading
import com.postiz.mobile.util.Resource

@Composable
fun IntegrationsScreen(
    onBack: () -> Unit,
    onAddChannel: () -> Unit,
    onChannelTap: (String) -> Unit,
    viewModel: IntegrationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val disconnectError by viewModel.disconnectError.collectAsState()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    var confirmDisconnect by remember { mutableStateOf<IntegrationDto?>(null) }

    // Connecting a channel happens in the browser, on the Add Channel screen
    // underneath this one on the back stack -- this ViewModel instance isn't
    // recreated when we navigate there and back, so refresh explicitly every
    // time this screen becomes visible again (covers both "back from Add
    // Channel" and "resumed after the OAuth browser tab closes").
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.width(1.dp)) // header actions stay on the right
            Row {
                Text(
                    "↻ Refresh",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                    modifier = Modifier.clickable(onClick = viewModel::load)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "+ Add",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    modifier = Modifier.clickable(onClick = onAddChannel)
                )
            }
        }

        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp)) {
            Text("CHANNELS", style = MaterialTheme.typography.labelLarge, color = accent)
            Text("Connected", style = MaterialTheme.typography.titleLarge)
            val count = (state as? Resource.Success)?.data?.size ?: 0
            Text(
                if (count == 1) "1 channel" else "$count channels",
                style = MaterialTheme.typography.bodyMedium,
                color = muted
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
            when (val s = state) {
                is Resource.Loading -> FullScreenLoading()
                is Resource.Error -> FullScreenError(s.message, onRetry = viewModel::load)
                is Resource.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyState("No channels connected yet. Tap + Add to connect one.")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
                            items(s.data, key = { it.id }) { integration ->
                                IntegrationRow(
                                    integration = integration,
                                    onTap = { onChannelTap(integration.id) },
                                    onDisconnect = { confirmDisconnect = integration }
                                )
                            }
                        }
                    }
                }
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
                }) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = null }) { Text("Cancel") }
            }
        )
    }

    disconnectError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDisconnectError,
            title = { Text("Couldn't disconnect") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDisconnectError) { Text("OK") }
            }
        )
    }
}

@Composable
private fun IntegrationRow(
    integration: IntegrationDto,
    onTap: () -> Unit,
    onDisconnect: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val hairline = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (integration.picture != null) {
            AsyncImage(
                model = integration.picture,
                contentDescription = integration.name,
                modifier = Modifier.size(42.dp).clip(CircleShape)
            )
        } else {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        integration.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ink
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                integration.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (integration.disabled) muted else ink
            )
            val meta = integration.identifier.uppercase() +
                (integration.customer?.let { " · ${it.name.uppercase()}" } ?: "")
            Text(meta, style = MaterialTheme.typography.labelLarge, color = muted)
        }

        Text(
            text = if (integration.disabled) "Disabled" else "Active",
            style = MaterialTheme.typography.bodyMedium,
            color = if (integration.disabled) muted.copy(alpha = 0.7f) else accent
        )
        IconButton(onClick = onDisconnect) {
            Icon(Icons.Default.Close, contentDescription = "Disconnect ${integration.name}", tint = muted)
        }
    }
    HorizontalDivider(color = hairline, thickness = 1.dp)
}
