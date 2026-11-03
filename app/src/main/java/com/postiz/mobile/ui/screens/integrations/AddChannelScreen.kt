package com.postiz.mobile.ui.screens.integrations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.data.remote.dto.KNOWN_PROVIDER_IDENTIFIERS

@Composable
fun AddChannelScreen(onBack: () -> Unit, viewModel: AddChannelViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onBackground
    val hairline = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)) {
            Text(
                "‹ Channels",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Spacer(Modifier.height(10.dp))
            Text("ADD CHANNEL", style = MaterialTheme.typography.labelLarge, color = accent)
            Text("Connect a new channel", style = MaterialTheme.typography.titleLarge)
            Text(
                "Opens your server's connect flow in the browser. Come back and pull to refresh once it's linked.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (viewModel.error != null) {
            Text(
                viewModel.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        LazyColumn(modifier = Modifier.padding(horizontal = 24.dp)) {
            items(KNOWN_PROVIDER_IDENTIFIERS) { identifier ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = viewModel.connectingId == null) {
                            viewModel.connect(identifier) { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        providerDisplayName(identifier),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (viewModel.connectingId == identifier) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accent)
                    }
                }
                HorizontalDivider(color = hairline, thickness = 1.dp)
            }
        }
    }
}

private val displayNames = mapOf(
    "x" to "X",
    "linkedin" to "LinkedIn",
    "linkedin-page" to "LinkedIn Page",
    "instagram" to "Instagram",
    "instagram-standalone" to "Instagram Standalone",
    "facebook" to "Facebook",
    "threads" to "Threads",
    "mastodon" to "Mastodon",
    "bluesky" to "Bluesky",
    "telegram" to "Telegram",
    "youtube" to "YouTube",
    "tiktok" to "TikTok",
    "pinterest" to "Pinterest",
    "reddit" to "Reddit",
    "discord" to "Discord",
    "slack" to "Slack",
    "wordpress" to "WordPress",
    "devto" to "Dev.to",
    "hashnode" to "Hashnode",
    "medium" to "Medium",
    "gmb" to "Google Business Profile",
    "wrapcast" to "Farcaster",
    "lemmy" to "Lemmy",
    "listmonk" to "Listmonk",
    "dribbble" to "Dribbble",
    "nostr" to "Nostr",
    "vk" to "VK"
)

private fun providerDisplayName(identifier: String): String = displayNames[identifier]
    ?: identifier.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
