package com.postiz.mobile.ui.screens.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postiz.mobile.data.remote.dto.AnalyticsDataDto
import com.postiz.mobile.data.repository.PostizRepository
import com.postiz.mobile.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The ranges the analytics screens offer, in days. */
val AnalyticsRanges = listOf(7, 30, 90)

@HiltViewModel
class ChannelAnalyticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PostizRepository
) : ViewModel() {

    private val integrationId: String = checkNotNull(savedStateHandle["integrationId"])

    private val _state = MutableStateFlow<Resource<List<AnalyticsDataDto>>>(Resource.Loading)
    val state: StateFlow<Resource<List<AnalyticsDataDto>>> = _state.asStateFlow()

    private val _days = MutableStateFlow(7)
    val days: StateFlow<Int> = _days.asStateFlow()

    init {
        load()
    }

    fun setDays(days: Int) {
        if (_days.value == days) return
        _days.value = days
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = Resource.Loading
            _state.value = repository.getChannelAnalytics(integrationId, _days.value)
        }
    }
}

@HiltViewModel
class PostAnalyticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PostizRepository
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _state = MutableStateFlow<Resource<List<AnalyticsDataDto>>>(Resource.Loading)
    val state: StateFlow<Resource<List<AnalyticsDataDto>>> = _state.asStateFlow()

    private val _days = MutableStateFlow(7)
    val days: StateFlow<Int> = _days.asStateFlow()

    init {
        load()
    }

    fun setDays(days: Int) {
        if (_days.value == days) return
        _days.value = days
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = Resource.Loading
            _state.value = repository.getPostAnalytics(postId, _days.value)
        }
    }
}
