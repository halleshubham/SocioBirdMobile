package com.postiz.mobile.ui.screens.connect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postiz.mobile.BuildConfig
import com.postiz.mobile.data.local.SessionManager
import com.postiz.mobile.data.remote.PostizApiProvider
import com.postiz.mobile.data.repository.PostizRepository
import com.postiz.mobile.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val serverUrl: String = "",
    val apiToken: String = "",
    val isCloud: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiProvider: PostizApiProvider,
    private val repository: PostizRepository
) : ViewModel() {

    /** True in the sociobird flavor: the server is fixed, only the API key is asked for. */
    val serverLocked: Boolean = BuildConfig.LOCKED_SERVER_URL.isNotEmpty()

    var uiState by mutableStateOf(
        ConnectUiState(serverUrl = BuildConfig.LOCKED_SERVER_URL.removePrefix("https://"))
    )
        private set

    fun onServerUrlChange(value: String) {
        uiState = uiState.copy(serverUrl = value, error = null)
    }

    fun onTokenChange(value: String) {
        uiState = uiState.copy(apiToken = value, error = null)
    }

    fun onCloudToggle(value: Boolean) {
        uiState = uiState.copy(isCloud = value)
    }

    /**
     * Saves the entered URL/token, then immediately calls GET /is-connected
     * to validate them. On failure the half-saved session is cleared so the
     * user isn't left in limbo. On success, sessionManager's Flow emits the
     * new (valid) session and the root nav graph swaps to the main app --
     * no explicit navigation call needed from here.
     */
    fun connect() {
        val url = uiState.serverUrl.trim()
        val token = uiState.apiToken.trim()

        if (url.isBlank() || token.isBlank()) {
            uiState = uiState.copy(error = if (serverLocked) "Enter your API key" else "Enter both the server URL and your API key")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            sessionManager.saveSession(url, token, uiState.isCloud)
            apiProvider.invalidate()

            when (val result = repository.checkConnection()) {
                is Resource.Success -> {
                    if (result.data) {
                        uiState = uiState.copy(isLoading = false)
                    } else {
                        sessionManager.clearSession()
                        uiState = uiState.copy(isLoading = false, error = "Server reached, but the key was rejected")
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
