package com.projectronin.interop.ehr.hl7.converters.datatypes

import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

fun Instant.toFormattedDate(): String {
    val instantFormatter =
        DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .appendLiteral("T")
            .appendPattern("HH:mm:ss.SSS")
            .optionalStart()
            .appendLiteral("Z")
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HH:mm", "+00:00")
            .optionalEnd()
            .toFormatter()
    val hl7Format =
        DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter()
    val parsedDate = instantFormatter.parse(this.value)
    return hl7Format.format(parsedDate)
}
