package com.postiz.mobile.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.data.remote.dto.AnalyticsDataDto
import com.postiz.mobile.ui.components.SbCard
import com.postiz.mobile.ui.components.SbEmptyState
import com.postiz.mobile.ui.components.Shimmer
import com.postiz.mobile.util.Resource
import java.util.Locale
import kotlin.math.abs

@Composable
fun ChannelAnalyticsScreen(onBack: () -> Unit, viewModel: ChannelAnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val days by viewModel.days.collectAsState()
    InsightsScaffold(
        title = "Channel insights",
        days = days,
        onDays = viewModel::setDays,
        onBack = onBack,
        state = state,
        onRetry = viewModel::load,
        emptyTitle = "No analytics yet",
        emptyMessage = "This platform may not report analytics yet, or the channel is new. Check back after a few posts."
    )
}

@Composable
fun PostAnalyticsScreen(onBack: () -> Unit, viewModel: PostAnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val days by viewModel.days.collectAsState()
    InsightsScaffold(
        title = "Post insights",
        days = days,
        onDays = viewModel::setDays,
        onBack = onBack,
        state = state,
        onRetry = viewModel::load,
        emptyTitle = "No analytics for this post yet",
        emptyMessage = "Numbers usually appear a day after a post is published, and not every platform shares them."
    )
}

@Composable
private fun InsightsScaffold(
    title: String,
    days: Int,
    onDays: (Int) -> Unit,
    onBack: () -> Unit,
    state: Resource<List<AnalyticsDataDto>>,
    onRetry: () -> Unit,
    emptyTitle: String,
    emptyMessage: String
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            AnalyticsRanges.forEachIndexed { index, range ->
                SegmentedButton(
                    selected = days == range,
                    onClick = { onDays(range) },
                    shape = SegmentedButtonDefaults.itemShape(index, AnalyticsRanges.size),
                    label = { Text("$range days") }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                is Resource.Loading -> items(3) { Shimmer(Modifier.fillMaxWidth().height(150.dp)) }
                is Resource.Error -> item {
                    SbCard(container = MaterialTheme.colorScheme.errorContainer) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Couldn't load analytics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            TextButton(onClick = onRetry) { Text("Try again") }
                        }
                    }
                }
                is Resource.Success -> {
                    if (state.data.isEmpty()) {
                        item {
                            SbEmptyState(
                                icon = Icons.Default.BarChart,
                                title = emptyTitle,
                                message = emptyMessage,
                                modifier = Modifier.padding(top = 32.dp)
                            )
                        }
                    } else {
                        items(state.data, key = { it.label }) { metric -> MetricCard(metric) }
                    }
                }
            }
        }
    }
}

/** One metric: label, headline number, change vs the previous period and a sparkline of the daily values. */
@Composable
private fun MetricCard(metric: AnalyticsDataDto) {
    val scheme = MaterialTheme.colorScheme
    val values = metric.data.map { it.total.toDoubleOrNull() ?: 0.0 }
    // Counts that accumulate (followers, subscribers) are shown as the latest value; everything else is a daily amount to add up.
    val isSnapshot = metric.label.lowercase().let { "follower" in it || "subscriber" in it || "connection" in it || "friends" in it }
    val headline = if (isSnapshot) values.lastOrNull() ?: 0.0 else values.sum()
    SbCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        metric.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(formatCount(headline), style = MaterialTheme.typography.headlineMedium)
                }
                ChangeChip(metric.percentageChange)
            }
            Spacer(Modifier.height(12.dp))
            Sparkline(values, modifier = Modifier.fillMaxWidth().height(64.dp), color = scheme.primary)
        }
    }
}

@Composable
private fun ChangeChip(change: Double) {
    val scheme = MaterialTheme.colorScheme
    val flat = abs(change) < 0.05
    val up = change >= 0
    val container = when {
        flat -> scheme.surfaceContainerHigh
        up -> scheme.tertiaryContainer
        else -> scheme.errorContainer
    }
    val content = when {
        flat -> scheme.onSurfaceVariant
        up -> scheme.onTertiaryContainer
        else -> scheme.onErrorContainer
    }
    Row(
        modifier = Modifier.clip(CircleShape).background(container).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!flat) {
            Icon(
                if (up) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(4.dp))
        }
        Text(
            (if (up && !flat) "+" else "") + String.format(Locale.getDefault(), "%.1f%%", change),
            style = MaterialTheme.typography.labelMedium,
            color = content
        )
    }
}

/** A small line chart with a soft fill; draws a flat line when there is one point or no variation. */
@Composable
private fun Sparkline(values: List<Double>, modifier: Modifier, color: Color) {
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        if (values.isEmpty()) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        fun point(index: Int): Offset {
            val normalised = if (max == min) 0.5f else ((values[index] - min) / range).toFloat()
            // keep a little padding so the stroke is not clipped at the top and bottom
            val y = size.height - (normalised * (size.height - 8.dp.toPx())) - 4.dp.toPx()
            return Offset(if (values.size > 1) stepX * index else size.width / 2f, y)
        }
        if (values.size == 1) {
            drawCircle(color, radius = 4.dp.toPx(), center = point(0))
            return@Canvas
        }
        val line = Path().apply {
            moveTo(point(0).x, point(0).y)
            for (i in 1 until values.size) lineTo(point(i).x, point(i).y)
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f))))
        drawPath(line, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun formatCount(value: Double): String {
    val rounded = value.toLong()
    return when {
        rounded >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", rounded / 1_000_000.0)
        rounded >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", rounded / 1_000.0)
        else -> rounded.toString()
    }
}
