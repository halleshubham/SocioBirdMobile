package com.postiz.mobile.ui.screens.posts

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postiz.mobile.data.remote.dto.CreatePostRequestDto
import com.postiz.mobile.data.remote.dto.CustomerDto
import com.postiz.mobile.data.remote.dto.IntegrationDto
import com.postiz.mobile.data.remote.dto.PostImageDto
import com.postiz.mobile.data.remote.dto.PostIntegrationRefDto
import com.postiz.mobile.data.remote.dto.PostRequestItemDto
import com.postiz.mobile.data.remote.dto.PostValueDto
import com.postiz.mobile.data.remote.dto.UploadResponseDto
import com.postiz.mobile.data.repository.PostizRepository
import com.postiz.mobile.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

enum class ScheduleMode { NOW, LATER }

data class CreatePostUiState(
    val content: String = "",
    val integrations: List<IntegrationDto> = emptyList(),
    val selectedIntegrationIds: Set<String> = emptySet(),
    val scheduleMode: ScheduleMode = ScheduleMode.NOW,
    /**
     * Midnight UTC of the LOCAL calendar date picked in the DatePicker
     * (that's the contract of DatePickerState.selectedDateMillis — it is
     * NOT the user's local midnight). Combined with [scheduleHour]/
     * [scheduleMinute] (picked in the user's wall-clock time) at submit
     * time to build a real zoned Instant. Never sent to the API directly.
     */
    val scheduleDateMillisUtc: Long? = null,
    val scheduleHour: Int = 10,
    val scheduleMinute: Int = 0,
    val uploadedImages: List<PostImageDto> = emptyList(),
    val isUploadingImage: Boolean = false,
    /** 0..1 progress of the file currently uploading, or null when unknown / idle. */
    val uploadProgress: Float? = null,
    /** e.g. "Uploading video 1 of 2" */
    val uploadLabel: String? = null,
    val isLoadingIntegrations: Boolean = true,
    val isSubmitting: Boolean = false,
    val isSuggestingSlot: Boolean = false,
    /** Smallest character limit among the selected channels, from GET /integration-settings/:id. Null = unknown/no limit reported. */
    val maxLength: Int? = null,
    val error: String? = null,
    val submitted: Boolean = false
) {
    /** Brands present among the connected channels, derived from each channel's customer. */
    val brands: List<CustomerDto>
        get() = integrations.mapNotNull { it.customer }.distinctBy { it.id }.sortedBy { it.name.lowercase() }

    /** A brand counts as selected when every one of its channels is selected. */
    fun isBrandSelected(brandId: String): Boolean {
        val ids = integrations.filter { it.customer?.id == brandId }.map { it.id }
        return ids.isNotEmpty() && selectedIntegrationIds.containsAll(ids)
    }

    /** The picked local date, or today if none picked yet (sensible picker default). */
    val scheduleLocalDate: LocalDate
        get() = scheduleDateMillisUtc
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
            ?: LocalDate.now()

    /** Human display string in the device's own timezone, e.g. "Fri, Sep 19 · 10:00 AM". */
    val scheduleDisplay: String
        get() {
            val date = scheduleLocalDate
            val dateLabel = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
            val timeLabel = LocalTime.of(scheduleHour, scheduleMinute)
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
            return "$dateLabel · $timeLabel"
        }
}

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val repository: PostizRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    var uiState by mutableStateOf(CreatePostUiState())
        private set

    /** The device's zone name, shown next to the picker so scheduling is never ambiguous. */
    val zoneLabel: String = ZoneId.systemDefault().getDisplayName(TextStyle.SHORT, Locale.getDefault())

    init {
        viewModelScope.launch {
            when (val result = repository.getIntegrations()) {
                is Resource.Success -> uiState = uiState.copy(
                    integrations = result.data,
                    isLoadingIntegrations = false
                )
                is Resource.Error -> uiState = uiState.copy(
                    isLoadingIntegrations = false,
                    error = result.message
                )
                Resource.Loading -> Unit
            }
        }
    }

    fun onContentChange(value: String) {
        uiState = uiState.copy(content = value)
    }

    fun toggleIntegration(id: String) {
        val current = uiState.selectedIntegrationIds
        uiState = uiState.copy(
            selectedIntegrationIds = if (id in current) current - id else current + id
        )
        refreshMaxLength()
    }

    /** Quick-select: toggles every channel belonging to the brand at once. */
    fun toggleBrand(brandId: String) {
        val ids = uiState.integrations.filter { it.customer?.id == brandId }.map { it.id }.toSet()
        if (ids.isEmpty()) return
        val current = uiState.selectedIntegrationIds
        uiState = uiState.copy(
            selectedIntegrationIds = if (current.containsAll(ids)) current - ids else current + ids
        )
        refreshMaxLength()
    }

    /** The composer's character limit is the tightest one among the currently selected channels. */
    private fun refreshMaxLength() {
        val ids = uiState.selectedIntegrationIds
        if (ids.isEmpty()) {
            uiState = uiState.copy(maxLength = null)
            return
        }
        viewModelScope.launch {
            val lengths = ids.map { id ->
                async { (repository.getIntegrationSettings(id) as? Resource.Success)?.data?.maxLength }
            }.awaitAll()
            uiState = uiState.copy(maxLength = lengths.filterNotNull().minOrNull())
        }
    }

    fun onScheduleModeChange(mode: ScheduleMode) {
        uiState = uiState.copy(scheduleMode = mode)
    }

    fun onScheduleDateSelected(utcMillis: Long?) {
        uiState = uiState.copy(scheduleDateMillisUtc = utcMillis)
    }

    fun onScheduleTimeSelected(hour: Int, minute: Int) {
        uiState = uiState.copy(scheduleHour = hour, scheduleMinute = minute)
    }

    /**
     * Uploads picked files one at a time (a 1 GB video must not compete with
     * other uploads for the mobile uplink) and appends whichever succeed to
     * the existing selection, reporting byte-level progress as it goes.
     */
    fun onImagesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uiState = uiState.copy(isUploadingImage = true, uploadProgress = null, uploadLabel = null, error = null)
            val resolver = appContext.contentResolver
            val uploaded = mutableListOf<PostImageDto>()
            var firstError: String? = null

            uris.forEachIndexed { index, uri ->
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                val isVideo = mimeType.startsWith("video/")
                uiState = uiState.copy(
                    uploadProgress = 0f,
                    uploadLabel = "Uploading ${if (isVideo) "video" else "file"} ${index + 1} of ${uris.size}"
                )
                // The server sniffs the real content from magic bytes and only
                // allows video/mp4 for video (any other container/codec 400s
                // after the whole file has already been uploaded) -- catching
                // it here skips a slow, doomed upload.
                if (isVideo && mimeType != "video/mp4") {
                    if (firstError == null) firstError =
                        "This server only accepts MP4 video; this file is $mimeType. Try converting it to MP4 first."
                    return@forEachIndexed
                }
                val part = try {
                    uriToMultipart(uri, mimeType) { fraction -> uiState = uiState.copy(uploadProgress = fraction) }
                } catch (e: Exception) {
                    if (firstError == null) firstError = "Couldn't read that file"
                    return@forEachIndexed
                }
                val size = runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L
                val response = if (size > MULTIPART_THRESHOLD_BYTES) {
                    // too big to send through the server and the proxy in front of it
                    repository.uploadLargeFile(
                        fileName = multipartFileName(mimeType),
                        mimeType = mimeType,
                        totalBytes = size,
                        openStream = { resolver.openInputStream(uri) },
                        onProgress = { fraction -> uiState = uiState.copy(uploadProgress = fraction) },
                        fallback = { repository.uploadFile(part) }
                    )
                } else {
                    repository.uploadFile(part)
                }
                when (val result = response) {
                    is Resource.Success -> uploaded += PostImageDto(id = result.data.id, path = result.data.path)
                    is Resource.Error -> if (firstError == null) firstError = result.message
                    Resource.Loading -> Unit
                }
            }

            uiState = uiState.copy(
                isUploadingImage = false,
                uploadProgress = null,
                uploadLabel = null,
                uploadedImages = uiState.uploadedImages + uploaded,
                error = firstError
            )
        }
    }

    fun attachByUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            uiState = uiState.copy(isUploadingImage = true, error = null)
            when (val result = repository.uploadFromUrl(url.trim())) {
                is Resource.Success -> uiState = uiState.copy(
                    isUploadingImage = false,
                    uploadedImages = uiState.uploadedImages + PostImageDto(id = result.data.id, path = result.data.path)
                )
                is Resource.Error -> uiState = uiState.copy(isUploadingImage = false, error = result.message)
                Resource.Loading -> Unit
            }
        }
    }

    fun removeImage(id: String) {
        uiState = uiState.copy(uploadedImages = uiState.uploadedImages.filterNot { it.id == id })
    }

    /** Asks the server for the next free slot on the first selected channel and fills the picker with it. */
    fun suggestTime() {
        val integrationId = uiState.selectedIntegrationIds.firstOrNull()
        if (integrationId == null) {
            uiState = uiState.copy(error = "Pick a channel first")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isSuggestingSlot = true, error = null)
            when (val result = repository.findSlot(integrationId)) {
                is Resource.Success -> applySuggestedInstant(result.data)
                is Resource.Error -> uiState = uiState.copy(error = result.message)
                Resource.Loading -> Unit
            }
            uiState = uiState.copy(isSuggestingSlot = false)
        }
    }

    private fun applySuggestedInstant(isoUtc: String) {
        val instant = runCatching { Instant.parse(isoUtc) }.getOrNull() ?: return
        val zoned = instant.atZone(ZoneId.systemDefault())
        val utcMidnightMillis = zoned.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        uiState = uiState.copy(
            scheduleMode = ScheduleMode.LATER,
            scheduleDateMillisUtc = utcMidnightMillis,
            scheduleHour = zoned.hour,
            scheduleMinute = zoned.minute
        )
    }

    /**
     * Combines the picked local calendar date with the picked wall-clock
     * time IN THE DEVICE'S OWN ZONE, then converts that to a true Instant.
     * This is the one place local time becomes UTC for the wire.
     */
    private fun scheduledInstant(): Instant {
        val localDateTime = LocalDateTime.of(
            uiState.scheduleLocalDate,
            LocalTime.of(uiState.scheduleHour, uiState.scheduleMinute)
        )
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant()
    }

    fun submit() {
        val selected = uiState.integrations.filter { it.id in uiState.selectedIntegrationIds }
        if (uiState.content.isBlank()) {
            uiState = uiState.copy(error = "Write something first")
            return
        }
        if (selected.isEmpty()) {
            uiState = uiState.copy(error = "Pick at least one channel")
            return
        }

        val isoDate = if (uiState.scheduleMode == ScheduleMode.NOW) {
            Instant.now().toString()
        } else {
            scheduledInstant().toString()
        }

        val request = CreatePostRequestDto(
            type = if (uiState.scheduleMode == ScheduleMode.NOW) "now" else "schedule",
            date = isoDate,
            posts = selected.map { integration ->
                PostRequestItemDto(
                    integration = PostIntegrationRefDto(id = integration.id),
                    value = listOf(PostValueDto(content = uiState.content, image = uiState.uploadedImages)),
                    settings = buildJsonObject {
                        put("__type", JsonPrimitive(integration.identifier))
                        // These two providers reject the request (400) without
                        // their required extra field; everything else in the
                        // backend's provider list is optional beyond __type.
                        when (integration.identifier) {
                            "x" -> put("who_can_reply_post", JsonPrimitive("everyone"))
                            "instagram", "instagram-standalone" -> put("post_type", JsonPrimitive("post"))
                            // YouTube requires a title (2-100 chars) and a visibility;
                            // the first line of the post text becomes the video title.
                            "youtube" -> {
                                put("title", JsonPrimitive(youtubeTitle(uiState.content)))
                                put("type", JsonPrimitive("public"))
                            }
                        }
                    }
                )
            }
        )

        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, error = null)
            when (val result = repository.createPost(request)) {
                is Resource.Success -> uiState = uiState.copy(isSubmitting = false, submitted = true)
                is Resource.Error -> uiState = uiState.copy(isSubmitting = false, error = result.message)
                Resource.Loading -> Unit
            }
        }
    }

    /**
     * Streams the picked file straight from the ContentResolver into the
     * multipart request instead of reading it into a ByteArray first --
     * the previous approach buffered the whole file (fine for a photo,
     * but slow and OOM-risky for a large video: everything sits in memory
     * before the upload even starts writing to the network).
     */
    private fun uriToMultipart(
        uri: Uri,
        mimeType: String,
        onProgress: (Float) -> Unit
    ): MultipartBody.Part {
        val resolver = appContext.contentResolver
        val fileName = "upload_${System.currentTimeMillis()}"
        val body = object : RequestBody() {
            override fun contentType(): MediaType? = mimeType.toMediaTypeOrNull()

            override fun contentLength(): Long =
                runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L

            override fun writeTo(sink: BufferedSink) {
                val total = contentLength()
                val input = resolver.openInputStream(uri) ?: throw IllegalStateException("Empty file")
                input.use { stream ->
                    val source = stream.source()
                    var written = 0L
                    var lastReported = -1
                    while (true) {
                        val read = source.read(sink.buffer, 64L * 1024)
                        if (read == -1L) break
                        sink.emitCompleteSegments()
                        written += read
                        if (total > 0) {
                            val pct = (written * 100 / total).toInt()
                            if (pct != lastReported) {
                                lastReported = pct
                                onProgress(written.toFloat() / total)
                            }
                        }
                    }
                }
            }
        }
        return MultipartBody.Part.createFormData("file", fileName, body)
    }

    /** The server picks the stored type from the extension, so a multipart upload needs one. */
    private fun multipartFileName(mimeType: String): String {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        return "upload_${System.currentTimeMillis()}.$extension"
    }

    private companion object {
        // above this a file goes straight to storage in parts instead of through /upload
        const val MULTIPART_THRESHOLD_BYTES = 50L * 1024 * 1024
    }
}

/** First non-blank line of the post, trimmed to YouTube's 100-char title limit (min 2 chars). */
internal fun youtubeTitle(content: String): String {
    val firstLine = content.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
    val title = firstLine.take(100).trim()
    return if (title.length >= 2) title else title.padEnd(2, '.')
}
