package com.postiz.mobile.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.postiz.mobile.ui.navigation.Screen
import com.postiz.mobile.ui.screens.analytics.ChannelAnalyticsScreen
import com.postiz.mobile.ui.screens.analytics.PostAnalyticsScreen
import com.postiz.mobile.ui.screens.home.HomeScreen
import com.postiz.mobile.ui.screens.integrations.AddChannelScreen
import com.postiz.mobile.ui.screens.integrations.IntegrationsScreen
import com.postiz.mobile.ui.screens.posts.CreatePostScreen
import com.postiz.mobile.ui.screens.posts.PostsListScreen
import com.postiz.mobile.ui.screens.settings.SettingsScreen

private fun NavController.navigateSingleTopTo(route: String) =
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

/** Full-screen detail routes reached via a drill-in, not a tab: no bottom bar on these. */
private fun String?.isDetailRoute(): Boolean =
    this == Screen.CreatePost.route ||
        this == Screen.AddChannel.route ||
        this?.startsWith("channel_analytics/") == true ||
        this?.startsWith("post_analytics/") == true

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!currentRoute.isDetailRoute()) {
                AppNavigationBar(
                    currentRoute = currentRoute,
                    onHome = { navController.navigateSingleTopTo(Screen.Home.route) },
                    onCalendar = { navController.navigateSingleTopTo(Screen.Posts.route) },
                    onCreate = { navController.navigate(Screen.CreatePost.route) },
                    onChannels = { navController.navigateSingleTopTo(Screen.Integrations.route) },
                    onMore = { navController.navigateSingleTopTo(Screen.Settings.route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            // Edge-to-edge: keep content below the status bar / camera cutout (screens add no hard-coded top gap).
            modifier = Modifier.padding(padding).statusBarsPadding(),
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                    onOpenCalendar = { navController.navigateSingleTopTo(Screen.Posts.route) },
                    onOpenChannels = { navController.navigateSingleTopTo(Screen.Integrations.route) },
                    onAddChannel = { navController.navigate(Screen.AddChannel.route) },
                    onPostTap = { post ->
                        // Published posts have analytics; anything else is managed in the calendar.
                        if (post.state == "PUBLISHED") navController.navigate(Screen.PostAnalytics.route(post.id))
                        else navController.navigateSingleTopTo(Screen.Posts.route)
                    }
                )
            }
            composable(Screen.Posts.route) {
                PostsListScreen(
                    onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                    onPostTap = { postId -> navController.navigate(Screen.PostAnalytics.route(postId)) }
                )
            }
            composable(Screen.CreatePost.route) {
                CreatePostScreen(onDone = { navController.popBackStack() })
            }
            composable(Screen.Integrations.route) {
                IntegrationsScreen(
                    onAddChannel = { navController.navigate(Screen.AddChannel.route) },
                    onChannelTap = { id -> navController.navigate(Screen.ChannelAnalytics.route(id)) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.AddChannel.route) {
                AddChannelScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.ChannelAnalytics.route) {
                ChannelAnalyticsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.PostAnalytics.route) {
                PostAnalyticsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun AppNavigationBar(
    currentRoute: String?,
    onHome: () -> Unit,
    onCalendar: () -> Unit,
    onCreate: () -> Unit,
    onChannels: () -> Unit,
    onMore: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, currentRoute == Screen.Home.route, onHome)
        NavItem("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, currentRoute == Screen.Posts.route, onCalendar)
        // Create is an action, not a destination: a filled brand button in the middle of the bar.
        NavigationBarItem(
            selected = false,
            onClick = onCreate,
            icon = {
                Box(
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create a post", tint = MaterialTheme.colorScheme.onPrimary)
                }
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.surfaceContainer)
        )
        NavItem("Channels", Icons.Filled.Hub, Icons.Outlined.Hub, currentRoute == Screen.Integrations.route, onChannels)
        NavItem("More", Icons.Filled.Settings, Icons.Outlined.Settings, currentRoute == Screen.Settings.route, onMore)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(if (selected) selectedIcon else unselectedIcon, contentDescription = null) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        alwaysShowLabel = true
    )
}
