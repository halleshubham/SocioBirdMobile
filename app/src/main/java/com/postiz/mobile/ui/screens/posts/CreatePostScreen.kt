package com.postiz.mobile.ui.screens.posts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.postiz.mobile.data.remote.dto.IntegrationDto

@Composable
fun CreatePostScreen(
    onDone: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val hairline = MaterialTheme.colorScheme.outline

    LaunchedEffect(state.submitted) {
        if (state.submitted) onDone()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> -> viewModel.onImagesPicked(uris) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp, start = 12.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = ink)
            }
            Text("NEW POST", style = MaterialTheme.typography.labelLarge, color = accent)
            Text(
                "Cancel",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.clickable(onClick = onDone)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            BasicTextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    color = ink
                ),
                modifier = Modifier.fillMaxWidth().height(120.dp),
                decorationBox = { inner ->
                    if (state.content.isEmpty()) {
                        Text(
                            "What's the story?",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 21.sp,
                                color = muted.copy(alpha = 0.7f)
                            )
                        )
                    }
                    inner()
                }
            )

            state.maxLength?.let { limit ->
                val overLimit = state.content.length > limit
                Text(
                    "${state.content.length} / $limit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (overLimit) MaterialTheme.colorScheme.error else muted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            Spacer(Modifier.height(6.dp))
            Text("CHANNELS", style = MaterialTheme.typography.labelLarge, color = muted)
            Spacer(Modifier.height(12.dp))

            if (state.isLoadingIntegrations) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = accent)
            } else if (state.integrations.isEmpty()) {
                Text(
                    "No connected channels found. Connect one in the Postiz web app first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.integrations, key = { it.id }) { integration: IntegrationDto ->
                        val selected = integration.id in state.selectedIntegrationIds
                        ChannelChip(
                            integration = integration,
                            selected = selected,
                            accent = accent,
                            ink = ink,
                            muted = muted,
                            hairline = hairline,
                            onClick = { viewModel.toggleIntegration(integration.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("MEDIA", style = MaterialTheme.typography.labelLarge, color = muted)
            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.uploadedImages, key = { it.id }) { image ->
                    Box(modifier = Modifier.size(72.dp)) {
                        if (isVideoPath(image.path)) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(hairline.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Video", tint = muted)
                            }
                        } else {
                            AsyncImage(
                                model = image.path,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(4.dp))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { viewModel.removeImage(image.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .size(72.dp)
                            .border(1.dp, hairline, RoundedCornerShape(4.dp))
                            .clickable(enabled = !state.isUploadingImage) {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isUploadingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accent)
                        } else {
                            Text("+ Attach", style = MaterialTheme.typography.bodyMedium, color = muted)
                        }
                    }
                }
            }

            var showUrlField by remember { mutableStateOf(false) }
            var urlText by remember { mutableStateOf("") }

            if (showUrlField) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = ink),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 6.dp),
                        decorationBox = { inner ->
                            if (urlText.isEmpty()) {
                                Text(
                                    "https://…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = muted.copy(alpha = 0.6f)
                                )
                            }
                            inner()
                        }
                    )
                    Text(
                        "Add",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                        modifier = Modifier.clickable {
                            viewModel.attachByUrl(urlText)
                            urlText = ""
                            showUrlField = false
                        }
                    )
                }
            } else {
                Text(
                    "or paste an image/video URL",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                    modifier = Modifier.padding(top = 8.dp).clickable { showUrlField = true }
                )
            }

            Spacer(Modifier.height(22.dp))
            Text("WHEN", style = MaterialTheme.typography.labelLarge, color = muted)
            Spacer(Modifier.height(12.dp))

            ScheduleChoiceRow(
                label = "Now",
                selected = state.scheduleMode == ScheduleMode.NOW,
                accent = accent,
                ink = ink,
                muted = muted,
                onClick = { viewModel.onScheduleModeChange(ScheduleMode.NOW) }
            )
            Spacer(Modifier.height(10.dp))
            ScheduleChoiceRow(
                label = "Schedule",
                selected = state.scheduleMode == ScheduleMode.LATER,
                accent = accent,
                ink = ink,
                muted = muted,
                onClick = { viewModel.onScheduleModeChange(ScheduleMode.LATER) }
            )

            if (state.scheduleMode == ScheduleMode.LATER) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.scheduleDisplay,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                        color = ink,
                        modifier = Modifier.clickable { showDatePicker = true }.padding(bottom = 2.dp)
                    )
                    if (state.isSuggestingSlot) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = accent)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Edit date · Edit time · shown in ${viewModel.zoneLabel}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = muted,
                    modifier = Modifier.clickable { showTimePicker = true }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Suggest a time",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = accent,
                    modifier = Modifier.clickable(enabled = !state.isSuggestingSlot, onClick = viewModel::suggestTime)
                )
            }

            if (state.error != null) {
                Spacer(Modifier.height(14.dp))
                Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent, RoundedCornerShape(999.dp))
                    .clickable(enabled = !state.isSubmitting, onClick = viewModel::submit)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        if (state.scheduleMode == ScheduleMode.NOW) "Publish now" else "Schedule post",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.scheduleDateMillisUtc)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onScheduleDateSelected(datePickerState.selectedDateMillis)
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
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun ChannelChip(
    integration: IntegrationDto,
    selected: Boolean,
    accent: Color,
    ink: Color,
    muted: Color,
    hairline: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 130.dp)
            .border(1.dp, if (selected) accent else hairline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (integration.picture != null) {
                AsyncImage(
                    model = integration.picture,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (selected) accent else hairline),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        integration.name.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else muted
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                integration.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) accent else ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(2.dp))
        val caption = integration.identifier.uppercase() +
            (integration.customer?.let { " · ${it.name.uppercase()}" } ?: "")
        Text(
            caption,
            style = MaterialTheme.typography.labelLarge,
            color = muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val videoExtensions = setOf("mp4", "mov", "webm", "mkv", "avi", "m4v", "3gp")

private fun isVideoPath(path: String): Boolean {
    val ext = path.substringAfterLast('.', "").substringBefore('?').lowercase()
    return ext in videoExtensions
}

@Composable
private fun ScheduleChoiceRow(
    label: String,
    selected: Boolean,
    accent: Color,
    ink: Color,
    muted: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.Transparent)
                .border(1.dp, if (selected) accent else muted, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (selected) ink else muted)
    }
}
