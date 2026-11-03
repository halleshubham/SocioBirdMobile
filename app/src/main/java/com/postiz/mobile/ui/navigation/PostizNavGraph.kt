package com.postiz.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.ui.components.FullScreenLoading
import com.postiz.mobile.ui.main.MainScaffold
import com.postiz.mobile.BuildConfig
import com.postiz.mobile.ui.screens.connect.ConnectScreen
import com.postiz.mobile.ui.screens.connect.LoginScreen

@Composable
fun PostizNavGraph(rootViewModel: RootViewModel = hiltViewModel()) {
    val hasSession by rootViewModel.hasSession.collectAsState()
    // sociobird flavor: email/password by default, API key for accounts without a password (Google sign-up)
    var useApiKey by remember { mutableStateOf(false) }

    when (hasSession) {
        null -> FullScreenLoading()
        false -> when {
            BuildConfig.LOCKED_SERVER_URL.isEmpty() -> ConnectScreen()
            useApiKey -> ConnectScreen(onBack = { useApiKey = false })
            else -> LoginScreen(onUseApiKey = { useApiKey = true })
        }
        true -> MainScaffold()
    }
}
