package com.postiz.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.ui.components.FullScreenLoading
import com.postiz.mobile.ui.main.MainScaffold
import com.postiz.mobile.BuildConfig
import com.postiz.mobile.ui.screens.connect.ConnectScreen
import com.postiz.mobile.ui.screens.connect.LoginScreen

@Composable
fun PostizNavGraph(rootViewModel: RootViewModel = hiltViewModel()) {
    val hasSession by rootViewModel.hasSession.collectAsState()

    when (hasSession) {
        null -> FullScreenLoading()
        false -> if (BuildConfig.LOCKED_SERVER_URL.isNotEmpty()) LoginScreen() else ConnectScreen()
        true -> MainScaffold()
    }
}
