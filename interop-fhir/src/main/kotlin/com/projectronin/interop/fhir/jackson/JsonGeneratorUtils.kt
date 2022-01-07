package com.projectronin.interop.fhir.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.projectronin.interop.fhir.r4.datatype.DynamicValue

/**
 * Writes the supplied [value] as a field based on the values type and prefixed by [prefix], if the value is non-null.
 */
fun JsonGenerator.writeDynamicValueField(prefix: String, value: DynamicValue<*>?) {
    value?.let { it.writeToJson(prefix, this) }
}
