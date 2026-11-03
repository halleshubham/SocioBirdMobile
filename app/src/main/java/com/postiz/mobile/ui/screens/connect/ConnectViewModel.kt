package com.postiz.mobile.ui.screens.connect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postiz.mobile.data.local.SessionManager
import com.postiz.mobile.data.remote.PostizApiProvider
import com.postiz.mobile.data.repository.PostizRepository
import com.postiz.mobile.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val apiToken: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiProvider: PostizApiProvider,
    private val repository: PostizRepository
) : ViewModel() {

    var uiState by mutableStateOf(ConnectUiState())
        private set

    fun onTokenChange(value: String) {
        uiState = uiState.copy(apiToken = value, error = null)
    }

    /**
     * Saves the entered URL/token, then immediately calls GET /is-connected
     * to validate them. On failure the half-saved session is cleared so the
     * user isn't left in limbo. On success, sessionManager's Flow emits the
     * new (valid) session and the root nav graph swaps to the main app --
     * no explicit navigation call needed from here.
     */
    fun connect() {
        val token = uiState.apiToken.trim()

        if (token.isBlank()) {
            uiState = uiState.copy(error = "Enter your SocioBird API key")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            sessionManager.saveSession(token)
            apiProvider.invalidate()

            when (val result = repository.checkConnection()) {
                is Resource.Success -> {
                    if (result.data) {
                        uiState = uiState.copy(isLoading = false)
                    } else {
                        sessionManager.clearSession()
                        uiState = uiState.copy(isLoading = false, error = "SocioBird rejected this API key")
                    }
                }
                is Resource.Error -> {
                    sessionManager.clearSession()
                    uiState = uiState.copy(isLoading = false, error = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }
}
