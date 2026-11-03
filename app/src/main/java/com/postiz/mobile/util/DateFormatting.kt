package com.postiz.mobile.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * The API always deals in UTC instants (`2025-01-01T10:00:00.000Z`). Every
 * display path must convert to the device's zone here rather than showing
 * the raw string, or "9am" silently means UTC 9am to a user in IST.
 */
/** The local calendar date a UTC instant falls on, for grouping/filtering by day. */
fun localDateOf(isoUtc: String?): LocalDate? {
    if (isoUtc.isNullOrBlank()) return null
    return try {
        Instant.parse(isoUtc).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (e: DateTimeParseException) {
        null
    }
}

fun formatLocalSchedule(isoUtc: String?): String {
    if (isoUtc.isNullOrBlank()) return "–"
    val zoned = try {
        Instant.parse(isoUtc).atZone(ZoneId.systemDefault())
    } catch (e: DateTimeParseException) {
        return isoUtc
    }
    val today = LocalDate.now(ZoneId.systemDefault())
    val pattern = if (zoned.toLocalDate() == today) "h:mma" else "EEE h:mma"
    return zoned.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())).lowercase(Locale.getDefault())
}

/** e.g. "Tue, 4 Nov · 5:30 PM" in the device's zone, for the post detail sheet. */
fun formatLocalDateTime(isoUtc: String?): String {
    if (isoUtc.isNullOrBlank()) return "–"
    val zoned = try {
        Instant.parse(isoUtc).atZone(ZoneId.systemDefault())
    } catch (e: DateTimeParseException) {
        return isoUtc
    }
    return zoned.format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault()))
}

/** Time of day only, e.g. "5:30 PM", for rows grouped under a day. */
fun formatLocalTime(isoUtc: String?): String {
    if (isoUtc.isNullOrBlank()) return "–"
    val zoned = try {
        Instant.parse(isoUtc).atZone(ZoneId.systemDefault())
    } catch (e: DateTimeParseException) {
        return isoUtc
    }
    return zoned.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}
