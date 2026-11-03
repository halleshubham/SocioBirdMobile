package com.postiz.mobile.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postiz.mobile.data.remote.dto.IntegrationDto
import com.postiz.mobile.data.remote.dto.PostDto
import com.postiz.mobile.data.repository.PostizRepository
import com.postiz.mobile.ui.screens.posts.CalendarFocus
import com.postiz.mobile.ui.screens.posts.StatusFilter
import com.postiz.mobile.util.Resource
import com.postiz.mobile.util.localDateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.postiz.mobile.data.local.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** One day of the 7-day strip on Home. */
data class WeekDay(val date: LocalDate, val count: Int)

/** Everything Home shows, computed once from GET /posts and GET /integrations. */
data class HomeUi(
    val upNext: List<PostDto>,
    val failed: List<PostDto>,
    val channels: List<IntegrationDto>,
    val disabledChannels: List<IntegrationDto>,
    val scheduledThisWeek: Int,
    val publishedLast7Days: Int,
    val drafts: Int,
    val week: List<WeekDay>,
    val hasAnyPost: Boolean,
    val hasScheduledOrPublished: Boolean
) {
    val needsAttention: Boolean get() = failed.isNotEmpty() || disabledChannels.isNotEmpty()
    /** New account: no channel connected or nothing written yet, so Home shows the getting-started checklist. */
    val isGettingStarted: Boolean get() = channels.isEmpty() || !hasScheduledOrPublished
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PostizRepository,
    private val calendarFocus: CalendarFocus,
    sessionManager: SessionManager
) : ViewModel() {

    /** First word of the account name, for "Good morning, Priya"; null when unknown (email-less or API-key sessions). */
    val firstName: StateFlow<String?> = sessionManager.sessionFlow
        .map { session -> session.name.trim().substringBefore(" ").takeIf { it.isNotBlank() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _state = MutableStateFlow<Resource<HomeUi>>(Resource.Loading)
    val state: StateFlow<Resource<HomeUi>> = _state.asStateFlow()

    init {
        load()
    }

    /** "Review" on the attention card: the calendar opens on the failed posts. */
    fun reviewFailed() = calendarFocus.request(StatusFilter.FAILED)

    fun load() {
        viewModelScope.launch {
            // Keep showing the last data while refreshing; only the very first load shows skeletons.
            if (_state.value !is Resource.Success) _state.value = Resource.Loading
            val (posts, integrations) = coroutineScope {
                val p = async { repository.getPosts() }
                val i = async { repository.getIntegrations() }
                p.await() to i.await()
            }
            _state.value = when {
                posts is Resource.Error -> Resource.Error(posts.message)
                integrations is Resource.Error -> Resource.Error(integrations.message)
                posts is Resource.Success && integrations is Resource.Success ->
                    Resource.Success(build(posts.data, integrations.data))
                else -> Resource.Loading
            }
        }
    }

    private fun build(posts: List<PostDto>, integrations: List<IntegrationDto>): HomeUi {
        val now = Instant.now()
        val today = LocalDate.now()
        val weekEnd = today.plusDays(6)

        fun instantOf(post: PostDto): Instant? = runCatching { Instant.parse(post.publishDate) }.getOrNull()

        val upNext = posts
            .filter { it.state == "QUEUE" }
            .mapNotNull { post -> instantOf(post)?.takeIf { it >= now }?.let { post to it } }
            .sortedBy { it.second }
            .take(3)
            .map { it.first }

        val failed = posts
            .filter { it.state == "ERROR" }
            .mapNotNull { post -> instantOf(post)?.takeIf { it >= now.minus(30, ChronoUnit.DAYS) }?.let { post to it } }
            .sortedByDescending { it.second }
            .map { it.first }

        val week = (0..6).map { offset ->
            val date = today.plusDays(offset.toLong())
            WeekDay(date, posts.count { it.state != "ERROR" && localDateOf(it.publishDate) == date })
        }

        return HomeUi(
            upNext = upNext,
            failed = failed,
            channels = integrations,
            disabledChannels = integrations.filter { it.disabled },
            scheduledThisWeek = posts.count { post ->
                post.state == "QUEUE" && localDateOf(post.publishDate)?.let { it >= today && it <= weekEnd } == true
            },
            publishedLast7Days = posts.count { post ->
                post.state == "PUBLISHED" && instantOf(post)?.let { it >= now.minus(7, ChronoUnit.DAYS) && it <= now } == true
            },
            drafts = posts.count { it.state == "DRAFT" },
            week = week,
            hasAnyPost = posts.isNotEmpty(),
            hasScheduledOrPublished = posts.any { it.state == "QUEUE" || it.state == "PUBLISHED" }
        )
    }
}
