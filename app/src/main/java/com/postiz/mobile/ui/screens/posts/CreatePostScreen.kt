package com.postiz.mobile.ui.screens.posts

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.postiz.mobile.data.remote.dto.IntegrationDto
import com.postiz.mobile.ui.components.PlatformAvatar
import com.postiz.mobile.ui.components.SbCard

/** New post: pick channels, write, add media, choose when, preview, send. The primary button is always visible above the keyboard. */
@Composable
fun CreatePostScreen(
    onDone: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(state.submitted) {
        if (state.submitted) onDone()
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> -> viewModel.onImagesPicked(uris) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }

    fun leave() {
        if (state.hasChanges && !state.isSubmitting) showDiscard = true else onDone()
    }
    BackHandler(onBack = ::leave)

    val selectedChannels = state.integrations.filter { it.id in state.selectedIntegrationIds }
    val sendLabel = if (state.scheduleMode == ScheduleMode.NOW) "Publish now" else "Schedule post"

    Column(modifier = Modifier.fillMaxSize().background(scheme.background).imePadding()) {

        // ---- Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = ::leave) { Icon(Icons.Default.Close, contentDescription = "Close") }
            Text("New post", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(
                onClick = viewModel::saveDraft,
                enabled = !state.isSubmitting && state.blocker == null
            ) { Text("Save draft") }
        }

        // ---- Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            SectionLabel("Post to", trailing = if (state.selectedIntegrationIds.isEmpty()) null else "${state.selectedIntegrationIds.size} selected")
            Spacer(Modifier.height(10.dp))
            ChannelPicker(state, viewModel)

            Spacer(Modifier.height(20.dp))
            TextCard(state, viewModel)

            Spacer(Modifier.height(20.dp))
            SectionLabel("Media")
            Spacer(Modifier.height(10.dp))
            MediaTray(
                state = state,
                onAdd = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                onAddUrl = { showUrlDialog = true },
                onRemove = viewModel::removeImage
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("When")
            Spacer(Modifier.height(10.dp))
            ScheduleCard(
                state = state,
                zoneLabel = viewModel.zoneLabel,
                onMode = viewModel::onScheduleModeChange,
                onNextSlot = viewModel::suggestTime,
                onEvening = viewModel::scheduleTodayEvening,
                onMorning = viewModel::scheduleTomorrowMorning,
                onPickDate = { showDatePicker = true },
                onPickTime = { showTimePicker = true }
            )

            if (state.content.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Preview")
                Spacer(Modifier.height(10.dp))
                PreviewCard(state, selectedChannels.firstOrNull())
            }
            Spacer(Modifier.height(24.dp))
        }

        // ---- Sticky action area (stays above the keyboard)
        Surface(color = scheme.surfaceContainer, tonalElevation = 2.dp) {
            Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                val message = state.error ?: state.blocker
                if (message != null) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.error != null) scheme.error else scheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.isSubmitting && state.blocker == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = scheme.onPrimary)
                    } else {
                        Text(sendLabel, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard this post?") },
            text = { Text("Your text and media will be lost. You can also keep it as a draft.") },
            confirmButton = {
                TextButton(onClick = { showDiscard = false; onDone() }) { Text("Discard", color = scheme.error) }
            },
            dismissButton = {
                Row {
                    if (state.blocker == null) {
                        TextButton(onClick = { showDiscard = false; viewModel.saveDraft() }) { Text("Save draft") }
                    }
                    TextButton(onClick = { showDiscard = false }) { Text("Keep editing") }
                }
            }
        )
    }

    if (showUrlDialog) {
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Add from a link") },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    placeholder = { Text("https://…") },
                    label = { Text("Image or video URL") }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.attachByUrl(url); showUrlDialog = false }, enabled = url.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.scheduleDateMillisUtc)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onScheduleDateSelected(datePickerState.selectedDateMillis)
                    viewModel.onScheduleModeChange(ScheduleMode.LATER)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.scheduleHour,
            initialMinute = state.scheduleMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onScheduleTimeSelected(timePickerState.hour, timePickerState.minute)
                    viewModel.onScheduleModeChange(ScheduleMode.LATER)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun SectionLabel(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------------------------------------------------------------- channels
@Composable
private fun ChannelPicker(state: CreatePostUiState, viewModel: CreatePostViewModel) {
    val scheme = MaterialTheme.colorScheme
    when {
        state.isLoadingIntegrations -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        state.integrations.isEmpty() -> Text(
            "No connected channels yet. Add one from the Channels tab, then come back.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
        else -> Column {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val all = state.selectedIntegrationIds.size == state.integrations.count { !it.disabled }
                    FilterChip(
                        selected = all,
                        onClick = { if (all) viewModel.clearChannels() else viewModel.selectAllChannels() },
                        label = { Text("All channels") }
                    )
                }
                items(state.brands, key = { it.id }) { brand ->
                    FilterChip(
                        selected = state.isBrandSelected(brand.id),
                        onClick = { viewModel.toggleBrand(brand.id) },
                        label = { Text(brand.name, maxLines = 1) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(state.integrations, key = { it.id }) { integration ->
                    ChannelOption(
                        integration = integration,
                        selected = integration.id in state.selectedIntegrationIds,
                        onClick = { viewModel.toggleIntegration(integration.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelOption(integration: IntegrationDto, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(72.dp)
            .alpha(if (integration.disabled) 0.4f else if (selected) 1f else 0.75f)
            .clickable(enabled = !integration.disabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .then(if (selected) Modifier.border(3.dp, scheme.primary, CircleShape).padding(3.dp) else Modifier.padding(3.dp))
            ) {
                PlatformAvatar(integration.picture, integration.name, integration.identifier, size = 52.dp)
            }
            if (selected) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = scheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            integration.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------- text
@Composable
private fun TextCard(state: CreatePostUiState, viewModel: CreatePostViewModel) {
    val scheme = MaterialTheme.colorScheme
    SbCard(container = scheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp)) {
            BasicTextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                decorationBox = { inner ->
                    Box {
                        if (state.content.isEmpty()) {
                            Text(
                                "What do you want to share?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        inner()
                    }
                }
            )
            state.maxLength?.let { limit ->
                val over = state.content.length > limit
                Text(
                    "${state.content.length} / $limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (over) scheme.error else scheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ---------------------------------------------------------------- media
@Composable
private fun MediaTray(
    state: CreatePostUiState,
    onAdd: () -> Unit,
    onAddUrl: () -> Unit,
    onRemove: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.uploadedImages, key = { it.id }) { image ->
            Box(modifier = Modifier.size(92.dp)) {
                if (isVideoPath(image.path)) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(scheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Video", tint = scheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                    }
                } else {
                    AsyncImage(
                        model = image.path,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium)
                    )
                }
                IconButton(
                    onClick = { onRemove(image.id) },
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                ) {
                    Box(
                        modifier = Modifier.size(22.dp).clip(CircleShape).background(scheme.inverseSurface.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = scheme.inverseOnSurface, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .size(92.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(scheme.surfaceContainerLow)
                    .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.medium)
                    .clickable(enabled = !state.isUploadingImage, onClick = onAdd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (state.isUploadingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = scheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                }
            }
        }
    }

    if (state.isUploadingImage) {
        Spacer(Modifier.height(10.dp))
        val progress = state.uploadProgress
        Text(
            (state.uploadLabel ?: "Uploading") + (progress?.let { " · ${(it * 100).toInt()}%" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        TextButton(onClick = onAddUrl, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Add from a link") }
    }
}

// ---------------------------------------------------------------- schedule
@Composable
private fun ScheduleCard(
    state: CreatePostUiState,
    zoneLabel: String,
    onMode: (ScheduleMode) -> Unit,
    onNextSlot: () -> Unit,
    onEvening: () -> Unit,
    onMorning: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val options = listOf(ScheduleMode.NOW to "Publish now", ScheduleMode.LATER to "Schedule")
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = state.scheduleMode == mode,
                onClick = { onMode(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label) }
            )
        }
    }
    if (state.scheduleMode == ScheduleMode.LATER) {
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                AssistChip(
                    onClick = onNextSlot,
                    enabled = !state.isSuggestingSlot,
                    label = { Text(if (state.isSuggestingSlot) "Finding…" else "Next free slot") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            item { AssistChip(onClick = onEvening, label = { Text("Today 6 PM") }) }
            item { AssistChip(onClick = onMorning, label = { Text("Tomorrow 9 AM") }) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onPickDate, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    state.scheduleLocalDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", java.util.Locale.getDefault())),
                    maxLines = 1
                )
            }
            OutlinedButton(onClick = onPickTime, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    java.time.LocalTime.of(state.scheduleHour, state.scheduleMinute)
                        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.getDefault())),
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Times shown in $zoneLabel", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- preview
/** A generic feed-style preview. It is not pixel-accurate per platform, but shows the text, media and channel as people will read them. */
@Composable
private fun PreviewCard(state: CreatePostUiState, channel: IntegrationDto?) {
    val scheme = MaterialTheme.colorScheme
    SbCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformAvatar(channel?.picture, channel?.name ?: "?", channel?.identifier, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(channel?.name ?: "Your channel", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (state.scheduleMode == ScheduleMode.NOW) "Now" else state.scheduleDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(state.content, style = MaterialTheme.typography.bodyMedium)
            state.uploadedImages.firstOrNull()?.let { media ->
                Spacer(Modifier.height(10.dp))
                if (isVideoPath(media.path)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium).background(scheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Video", tint = scheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                    }
                } else {
                    AsyncImage(
                        model = media.path,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.medium)
                    )
                }
                if (state.uploadedImages.size > 1) {
                    Text(
                        "+${state.uploadedImages.size - 1} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private val videoExtensions = setOf("mp4", "mov", "webm", "mkv", "avi", "m4v", "3gp")

private fun isVideoPath(path: String): Boolean {
    val ext = path.substringAfterLast('.', "").substringBefore('?').lowercase()
    return ext in videoExtensions
}
