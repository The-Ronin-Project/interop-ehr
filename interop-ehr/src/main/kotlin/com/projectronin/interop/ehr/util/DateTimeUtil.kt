package com.projectronin.interop.ehr.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

/**
 * Get a date string that is [days] in the past.
 * Requires input of a [dateFormat] to specify the required return string format, such as yyyy-MM-dd
 */
fun daysToPastDate(
    days: Int,
    dateFormat: DateTimeFormatter,
): String {
    return dateFormat.format(LocalDate.now().minus(Period.ofDays(days)))
}
