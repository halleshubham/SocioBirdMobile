package com.postiz.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.data.auth.OAuthCallback
import com.postiz.mobile.data.auth.OAuthCallbackBus
import com.postiz.mobile.data.local.ThemeMode
import com.postiz.mobile.ui.navigation.PostizNavGraph
import com.postiz.mobile.ui.theme.PostizMobileTheme
import com.postiz.mobile.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var oauthCallbackBus: OAuthCallbackBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthIntent(intent)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PostizMobileTheme(darkTheme = darkTheme) {
                PostizNavGraph()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    /** Google sign-in comes back as com.shackyapps.sociobird://auth/callback?code=...&state=... */
    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "com.shackyapps.sociobird" || data.host != "auth") return
        oauthCallbackBus.publish(
            OAuthCallback(
                code = data.getQueryParameter("code"),
                state = data.getQueryParameter("state"),
                error = data.getQueryParameter("error")
            )
        )
        intent.data = null
    }
}
