package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime

fun generateEffectiveDateTime(
    dateTime: DynamicValue<Any>?,
    possibleDateTime: DynamicValue<DateTime>,
): DynamicValue<Any> {
    return dateTime ?: possibleDateTime
}
