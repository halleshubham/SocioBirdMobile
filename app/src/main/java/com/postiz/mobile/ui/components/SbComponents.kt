package com.postiz.mobile.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.postiz.mobile.R

/** Logo of a connected platform, by the server's provider identifier. Null for providers without a bundled logo. */
@DrawableRes
fun platformIconRes(identifier: String?): Int? = when (identifier) {
    "x" -> R.drawable.ic_platform_x
    "facebook" -> R.drawable.ic_platform_facebook
    "instagram", "instagram-standalone" -> R.drawable.ic_platform_instagram
    "linkedin" -> R.drawable.ic_platform_linkedin
    "linkedin-page" -> R.drawable.ic_platform_linkedin_page
    "youtube" -> R.drawable.ic_platform_youtube
    "threads" -> R.drawable.ic_platform_threads
    "telegram" -> R.drawable.ic_platform_telegram
    "mastodon" -> R.drawable.ic_platform_mastodon
    "mastodon-custom" -> R.drawable.ic_platform_mastodon_custom
    "wordpress" -> R.drawable.ic_platform_wordpress
    "medium" -> R.drawable.ic_platform_medium
    "devto" -> R.drawable.ic_platform_devto
    "hashnode" -> R.drawable.ic_platform_hashnode
    "lemmy" -> R.drawable.ic_platform_lemmy
    "listmonk" -> R.drawable.ic_platform_listmonk
    "botsab" -> R.drawable.ic_platform_botsab
    else -> null
}

/** Channel avatar: the profile picture (or the first letter) with the platform logo as a small badge. */
@Composable
fun PlatformAvatar(
    picture: String?,
    name: String,
    identifier: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Box(modifier = modifier.size(size)) {
        val base = Modifier.size(size).clip(CircleShape)
        if (!picture.isNullOrBlank()) {
            AsyncImage(
                model = picture,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = base
            )
        } else {
            Box(
                modifier = base.background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.take(1).uppercase().ifBlank { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        platformIconRes(identifier)?.let { res ->
            val badge = size * 0.42f
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).size(badge),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface)
            ) {
                Image(
                    painter = painterResource(res),
                    contentDescription = identifier,
                    modifier = Modifier.padding(1.dp).clip(CircleShape)
                )
            }
        }
    }
}

/** Standard content card: tonal surface, rounded, no shadow. Pass [onClick] to make it tappable. */
@Composable
fun SbCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = container)
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors) { content() }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors) { content() }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

/** Post state as a coloured pill. States come from the API: DRAFT, QUEUE, PUBLISHED, ERROR. */
@Composable
fun StatusChip(state: String?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (label, container, content) = when (state) {
        "PUBLISHED" -> Triple("Published", scheme.tertiaryContainer, scheme.onTertiaryContainer)
        "ERROR" -> Triple("Failed", scheme.errorContainer, scheme.onErrorContainer)
        "DRAFT" -> Triple("Draft", scheme.surfaceContainerHigh, scheme.onSurfaceVariant)
        else -> Triple("Scheduled", scheme.primaryContainer, scheme.onPrimaryContainer)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Pulsing placeholder block for loading states. */
@Composable
fun Shimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer-alpha"
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

@Composable
fun SbEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}


private val providerNames = mapOf(
    "x" to "X",
    "linkedin" to "LinkedIn",
    "linkedin-page" to "LinkedIn Page",
    "instagram" to "Instagram",
    "instagram-standalone" to "Instagram",
    "facebook" to "Facebook Page",
    "threads" to "Threads",
    "mastodon" to "Mastodon",
    "mastodon-custom" to "Mastodon",
    "telegram" to "Telegram",
    "youtube" to "YouTube",
    "wordpress" to "WordPress",
    "devto" to "Dev.to",
    "hashnode" to "Hashnode",
    "medium" to "Medium",
    "lemmy" to "Lemmy",
    "listmonk" to "Listmonk",
    "botsab" to "Botsab"
)

/** Readable platform name for a provider identifier, e.g. "linkedin-page" -> "LinkedIn Page". */
fun providerDisplayName(identifier: String?): String =
    providerNames[identifier] ?: identifier.orEmpty().split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
