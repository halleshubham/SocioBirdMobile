package com.postiz.mobile.ui.screens.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postiz.mobile.data.remote.dto.IntegrationDto
import com.postiz.mobile.data.repository.PostizRepository
import com.postiz.mobile.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntegrationsViewModel @Inject constructor(
    private val repository: PostizRepository
) : ViewModel() {

    private val _state = MutableStateFlow<Resource<List<IntegrationDto>>>(Resource.Loading)
    val state: StateFlow<Resource<List<IntegrationDto>>> = _state.asStateFlow()

    private val _disconnectError = MutableStateFlow<String?>(null)
    val disconnectError: StateFlow<String?> = _disconnectError.asStateFlow()

    // No init{load()} here -- the screen calls load() itself from LifecycleResumeEffect, since this
    // ViewModel survives navigating to Add Channel and back (its NavBackStackEntry isn't destroyed),
    // so an init-only load would show stale data after connecting a new channel.

    /** The list stays on screen while it refreshes; only the first load shows placeholders. */
    fun load(): Job = viewModelScope.launch {
        if (_state.value !is Resource.Success) _state.value = Resource.Loading
        val result = repository.getIntegrations()
        if (result is Resource.Error && _state.value is Resource.Success) {
            _disconnectError.value = result.message
        } else {
            _state.value = result
        }
    }

    fun disconnect(id: String) {
        viewModelScope.launch {
            when (val result = repository.deleteIntegration(id)) {
                is Resource.Error -> _disconnectError.value = result.message
                else -> load()
            }
        }
    }

    fun dismissDisconnectError() {
        _disconnectError.value = null
    }
}
