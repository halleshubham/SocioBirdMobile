package com.postiz.mobile.ui.screens.integrations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postiz.mobile.data.remote.dto.ProviderDto
import com.postiz.mobile.data.repository.PostizRepository
import com.postiz.mobile.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A provider shown on Add channel. [connectableOnPhone] false = needs typed-in credentials, so it is set up on the web. */
data class ProviderOption(val identifier: String, val name: String, val connectableOnPhone: Boolean)

@HiltViewModel
class AddChannelViewModel @Inject constructor(
    private val repository: PostizRepository
) : ViewModel() {

    private val _providers = MutableStateFlow<Resource<List<ProviderOption>>>(Resource.Loading)
    val providers: StateFlow<Resource<List<ProviderOption>>> = _providers.asStateFlow()

    var connectingId by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadProviders()
    }

    /**
     * The server's own list of supported providers, so the app never offers one the web app hides ("coming soon").
     * A server without that list (404, offline) falls back to the providers that are known to connect with one browser hop.
     */
    fun loadProviders() {
        viewModelScope.launch {
            _providers.value = Resource.Loading
            _providers.value = when (val result = repository.getProviderCatalog()) {
                is Resource.Success -> Resource.Success(
                    result.data
                        .filter { it.identifier.isNotBlank() }
                        .map { it.toOption() }
                        .ifEmpty { fallbackProviders }
                )
                else -> Resource.Success(fallbackProviders)
            }
        }
    }

    /** Fetches the OAuth URL for this provider; the caller opens it in a browser. */
    fun connect(identifier: String, onUrlReady: (String) -> Unit) {
        viewModelScope.launch {
            connectingId = identifier
            error = null
            when (val result = repository.getSocialConnectUrl(identifier)) {
                is Resource.Success -> onUrlReady(result.data)
                is Resource.Error -> error = result.message
                Resource.Loading -> Unit
            }
            connectingId = null
        }
    }

    fun dismissError() {
        error = null
    }

    private fun ProviderDto.toOption() = ProviderOption(
        identifier = identifier,
        // the server's names can carry a line break, e.g. "Instagram\n(Facebook Business)"
        name = name.replace("\n", " ").ifBlank { identifier },
        connectableOnPhone = connectableOnPhone
    )

    private companion object {
        val fallbackProviders = listOf(
            ProviderOption("x", "X", true),
            ProviderOption("linkedin", "LinkedIn", true),
            ProviderOption("linkedin-page", "LinkedIn Page", true),
            ProviderOption("instagram", "Instagram (Facebook Business)", true),
            ProviderOption("facebook", "Facebook Page", true),
            ProviderOption("threads", "Threads", true),
            ProviderOption("youtube", "YouTube", true),
            ProviderOption("mastodon", "Mastodon", true)
        )
    }
}
