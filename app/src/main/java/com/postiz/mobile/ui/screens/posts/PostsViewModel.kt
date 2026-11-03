package com.postiz.mobile.ui.screens.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postiz.mobile.data.remote.dto.CustomerDto
import com.postiz.mobile.data.remote.dto.PostDto
import com.postiz.mobile.data.repository.PostizRepository
import com.postiz.mobile.util.Resource
import com.postiz.mobile.util.localDateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class CalendarMode { WEEK, MONTH }

/** Status filter chips on the calendar. [states] are the API's post states. */
enum class StatusFilter(val label: String, val states: Set<String>?) {
    ALL("All", null),
    SCHEDULED("Scheduled", setOf("QUEUE")),
    PUBLISHED("Published", setOf("PUBLISHED")),
    DRAFTS("Drafts", setOf("DRAFT")),
    FAILED("Failed", setOf("ERROR"));

    fun matches(state: String?): Boolean = states == null || state in states
}

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val repository: PostizRepository,
    private val calendarFocus: CalendarFocus
) : ViewModel() {

    private val _state = MutableStateFlow<Resource<List<PostDto>>>(Resource.Loading)
    val state: StateFlow<Resource<List<PostDto>>> = _state.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _mode = MutableStateFlow(CalendarMode.WEEK)
    val mode: StateFlow<CalendarMode> = _mode.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    /** Brands = the account's customer groups (GET /groups), same concept as the web app's brand filter. */
    private val _brands = MutableStateFlow<List<CustomerDto>>(emptyList())
    val brands: StateFlow<List<CustomerDto>> = _brands.asStateFlow()

    private val _selectedBrandId = MutableStateFlow<String?>(null)
    val selectedBrandId: StateFlow<String?> = _selectedBrandId.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    /** Posts the user deleted that are waiting out the undo window; hidden from the lists meanwhile. */
    private val _hiddenIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenIds: StateFlow<Set<String>> = _hiddenIds.asStateFlow()
    private val pendingDeletes = mutableMapOf<String, Job>()

    init {
        loadBrands()
        load()
        viewModelScope.launch {
            calendarFocus.request.collect { filter ->
                if (filter != null) {
                    calendarFocus.consume()
                    _statusFilter.value = filter
                    _mode.value = CalendarMode.MONTH
                    jumpToNewestMatching(filter)
                }
            }
        }
    }

    /** Selects the day of the most recent post matching [filter] (e.g. the latest failed post), once the list is loaded. */
    private fun jumpToNewestMatching(filter: StatusFilter) {
        viewModelScope.launch {
            val loaded = _state.first { it is Resource.Success } as Resource.Success
            loaded.data
                .filter { filter.matches(it.state) }
                .mapNotNull { localDateOf(it.publishDate) }
                .maxOrNull()
                ?.let { _selectedDate.value = it }
        }
    }

    private fun loadBrands() {
        viewModelScope.launch {
            val result = repository.getGroups()
            if (result is Resource.Success) {
                _brands.value = result.data.sortedBy { it.name }
            }
        }
    }

    /** First load shows the spinner; later refreshes keep the current list on screen. */
    fun load(): Job =
        viewModelScope.launch {
            if (_state.value !is Resource.Success) _state.value = Resource.Loading
            val result = repository.getPosts(customer = _selectedBrandId.value)
            // A failed refresh must not wipe a list that is already showing.
            if (result is Resource.Error && _state.value is Resource.Success) {
                _actionError.value = result.message
            } else {
                _state.value = result
            }
        }

    /** null selects "All brands". Filtering happens server-side (GET /posts?customer=). */
    fun selectBrand(id: String?) {
        if (_selectedBrandId.value == id) return
        _selectedBrandId.value = id
        _state.value = Resource.Loading
        load()
    }

    fun selectStatus(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun setMode(mode: CalendarMode) {
        _mode.value = mode
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    /** Moves the visible week (or month) forward/back, keeping the selected weekday (or day of month). */
    fun shift(direction: Int) {
        _selectedDate.value = when (_mode.value) {
            CalendarMode.WEEK -> _selectedDate.value.plusWeeks(direction.toLong())
            CalendarMode.MONTH -> _selectedDate.value.plusMonths(direction.toLong())
        }
    }

    /**
     * Soft delete: hide the post now and really delete it after [UNDO_WINDOW_MS] unless [undoDelete] is called.
     * The job lives in the view model, so leaving the screen during the window does not lose the delete.
     */
    fun requestDelete(post: PostDto) {
        _hiddenIds.value = _hiddenIds.value + post.id
        pendingDeletes[post.id]?.cancel()
        pendingDeletes[post.id] = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            when (val result = repository.deletePost(post.id)) {
                is Resource.Error -> {
                    _hiddenIds.value = _hiddenIds.value - post.id
                    _actionError.value = result.message
                }
                else -> load()
            }
            // The post is gone from the server; keep it hidden until the reload replaces the list.
            pendingDeletes.remove(post.id)
        }
    }

    fun undoDelete(id: String) {
        pendingDeletes.remove(id)?.cancel()
        _hiddenIds.value = _hiddenIds.value - id
    }

    /** Only DRAFT <-> QUEUE is a valid transition server-side; PUBLISHED/ERROR posts have no toggle. */
    fun toggleStatus(post: PostDto) {
        val newStatus = when (post.state) {
            "DRAFT" -> "schedule"
            "QUEUE" -> "draft"
            else -> return
        }
        viewModelScope.launch {
            when (val result = repository.changePostStatus(post.id, newStatus)) {
                is Resource.Error -> _actionError.value = result.message
                else -> load()
            }
        }
    }

    fun dismissActionError() {
        _actionError.value = null
    }

    private companion object {
        const val UNDO_WINDOW_MS = 4500L
    }
}
