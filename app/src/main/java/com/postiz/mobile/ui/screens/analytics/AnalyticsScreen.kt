package com.postiz.mobile.ui.screens.analytics

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.data.remote.dto.AnalyticsDataDto
import com.postiz.mobile.ui.components.EmptyState
import com.postiz.mobile.ui.components.FullScreenError
import com.postiz.mobile.ui.components.FullScreenLoading
import com.postiz.mobile.util.Resource
import java.util.Locale

@Composable
fun ChannelAnalyticsScreen(onBack: () -> Unit, viewModel: ChannelAnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    AnalyticsScaffold(
        label = "CHANNEL",
        title = "Analytics",
        subtitle = "Last 7 days",
        onBack = onBack,
        state = state,
        onRetry = viewModel::load,
        emptyMessage = "No analytics yet for this channel."
    )
}

@Composable
fun PostAnalyticsScreen(onBack: () -> Unit, viewModel: PostAnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    AnalyticsScaffold(
        label = "POST",
        title = "Analytics",
        subtitle = "Last 7 days",
        onBack = onBack,
        state = state,
        onRetry = viewModel::load,
        emptyMessage = "No analytics yet -- this post may not have been published, or the platform doesn't report analytics."
    )
}

@Composable
private fun AnalyticsScaffold(
    label: String,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    state: Resource<List<AnalyticsDataDto>>,
    onRetry: () -> Unit,
    emptyMessage: String
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onBackground
    val hairline = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)) {
            Text(
                "‹ Back",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Spacer(Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = muted)
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
            when (state) {
                is Resource.Loading -> FullScreenLoading()
                is Resource.Error -> FullScreenError(state.message, onRetry = onRetry)
                is Resource.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyState(emptyMessage)
                    } else {
                        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                            items(state.data) { metric ->
                                MetricRow(metric, ink, muted, accent, hairline)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    metric: AnalyticsDataDto,
    ink: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
    hairline: androidx.compose.ui.graphics.Color
) {
    val total = metric.data.sumOf { it.total.toDoubleOrNull() ?: 0.0 }
    val positive = metric.percentageChange >= 0
    val errorColor = MaterialTheme.colorScheme.error

    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Text(metric.label.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = muted)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatCount(total), style = MaterialTheme.typography.titleLarge, color = ink)
            val sign = if (positive) "+" else ""
            Text(
                "$sign${String.format(Locale.getDefault(), "%.1f", metric.percentageChange)}%",
                style = MaterialTheme.typography.bodyLarge,
                color = if (positive) accent else errorColor
            )
        }
    }
    HorizontalDivider(color = hairline, thickness = 1.dp)
}

private fun formatCount(value: Double): String {
    val rounded = value.toLong()
    return when {
        rounded >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", rounded / 1_000_000.0)
        rounded >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", rounded / 1_000.0)
        else -> rounded.toString()
    }
}
