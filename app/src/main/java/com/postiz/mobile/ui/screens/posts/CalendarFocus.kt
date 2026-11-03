package com.postiz.mobile.ui.screens.posts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lets other screens (Home's "Needs attention") open the calendar already filtered, e.g. to failed posts.
 * The calendar's view model consumes the request once.
 */
@Singleton
class CalendarFocus @Inject constructor() {
    private val _request = MutableStateFlow<StatusFilter?>(null)
    val request: StateFlow<StatusFilter?> = _request

    fun request(filter: StatusFilter) {
        _request.value = filter
    }

    fun consume() {
        _request.value = null
    }
}
