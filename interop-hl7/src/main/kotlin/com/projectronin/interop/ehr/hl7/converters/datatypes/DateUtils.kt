package com.interop.ehr.hl7.fhir.converters.datatypes

import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

fun Date.toFormattedDate(): String {
    val instantFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy")
        .optionalStart()
        .appendPattern("-MM")
        .optionalStart()
        .appendPattern("-dd")
        .optionalEnd()
        .optionalEnd()
        .toFormatter()
    val hl7Format = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .optionalStart()
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .optionalStart()
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .optionalEnd()
        .optionalEnd()
        .toFormatter()
    val parsedDate = instantFormatter.parse(this.value)
    return hl7Format.format(parsedDate)
}
