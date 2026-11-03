package com.postiz.mobile.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What the backend's /auth/oauth-mobile-callback bounced into the app: `com.shackyapps.sociobird://auth/callback?code=..&state=..`. */
data class OAuthCallback(val code: String?, val state: String?, val error: String?)

/**
 * MainActivity receives the deep link and drops it here; the login screen's view model picks it up.
 * A StateFlow keeps the callback if it arrives before the view model is collecting (process just restarted).
 */
@Singleton
class OAuthCallbackBus @Inject constructor() {
    private val _callback = MutableStateFlow<OAuthCallback?>(null)
    val callback: StateFlow<OAuthCallback?> = _callback

    fun publish(callback: OAuthCallback) {
        _callback.value = callback
    }

    fun consume() {
        _callback.value = null
    }
}
