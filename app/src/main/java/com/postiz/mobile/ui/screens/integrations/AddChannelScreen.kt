package com.postiz.mobile.ui.screens.integrations

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.BuildConfig
import com.postiz.mobile.ui.components.SbCard
import com.postiz.mobile.ui.components.Shimmer
import com.postiz.mobile.ui.components.platformIconRes
import com.postiz.mobile.util.Resource

/** Connect a new channel: the providers the server supports, as a grid. OAuth providers open in a browser tab. */
@Composable
fun AddChannelScreen(onBack: () -> Unit, viewModel: AddChannelViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val providers by viewModel.providers.collectAsState()
    var webSetup by remember { mutableStateOf<ProviderOption?>(null) }
    val serverUrl = BuildConfig.LOCKED_SERVER_URL

    fun openInBrowser(url: String) {
        runCatching { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url)) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column {
                Text("Add a channel", style = MaterialTheme.typography.titleLarge)
                Text(
                    "You'll sign in to the platform in your browser, then return here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        viewModel.error?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        when (val s = providers) {
            is Resource.Loading, is Resource.Error -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(6) { Shimmer(Modifier.fillMaxWidth().height(112.dp)) }
            }
            is Resource.Success -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(s.data, key = { it.identifier }) { provider ->
                    ProviderCard(
                        provider = provider,
                        connecting = viewModel.connectingId == provider.identifier,
                        enabled = viewModel.connectingId == null,
                        onClick = {
                            if (provider.connectableOnPhone) {
                                viewModel.connect(provider.identifier) { url -> openInBrowser(url) }
                            } else {
                                webSetup = provider
                            }
                        }
                    )
                }
            }
        }
    }

    webSetup?.let { provider ->
        AlertDialog(
            onDismissRequest = { webSetup = null },
            title = { Text("Connect ${provider.name} on the web") },
            text = {
                Text(
                    "${provider.name} needs a few details such as a site address or access token, which are easier to enter on a computer or in your browser. " +
                        "Once it is connected it appears in this app."
                )
            },
            confirmButton = {
                if (serverUrl.isNotBlank()) {
                    TextButton(onClick = { openInBrowser(serverUrl); webSetup = null }) { Text("Open SocioBird") }
                } else {
                    TextButton(onClick = { webSetup = null }) { Text("OK") }
                }
            },
            dismissButton = {
                if (serverUrl.isNotBlank()) TextButton(onClick = { webSetup = null }) { Text("Not now") }
            }
        )
    }
}

@Composable
private fun ProviderCard(provider: ProviderOption, connecting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    SbCard(modifier = Modifier.fillMaxWidth(), onClick = if (enabled) onClick else null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                val res = platformIconRes(provider.identifier)
                if (res != null) {
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(scheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(provider.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = scheme.onSecondaryContainer)
                    }
                }
                if (connecting) CircularProgressIndicator(modifier = Modifier.size(52.dp), strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                provider.name,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (provider.connectableOnPhone) "Connect" else "Set up on web",
                style = MaterialTheme.typography.bodySmall,
                color = if (provider.connectableOnPhone) scheme.primary else scheme.onSurfaceVariant
            )
        }
    }
}
