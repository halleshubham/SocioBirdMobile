package com.postiz.mobile.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Posts : Screen("posts") // the calendar
    data object CreatePost : Screen("create_post")
    data object Integrations : Screen("integrations")
    data object Settings : Screen("settings")
    data object AddChannel : Screen("add_channel")

    data object ChannelAnalytics : Screen("channel_analytics/{integrationId}") {
        // Integration ids are server-generated cuids -- no path-unsafe characters.
        fun route(integrationId: String) = "channel_analytics/$integrationId"
    }

    data object PostAnalytics : Screen("post_analytics/{postId}") {
        fun route(postId: String) = "post_analytics/$postId"
    }
}
