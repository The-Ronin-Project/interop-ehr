package com.projectronin.interop.fhir.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.projectronin.interop.fhir.r4.datatype.DynamicValue

/**
 * Writes the supplied [value] list as a field named [fieldName], if the list is populated.
 */
fun JsonGenerator.writeListField(fieldName: String, value: List<*>) {
    if (value.isNotEmpty()) {
        this.writeObjectField(fieldName, value)
    }
}

/**
 * Writes the supplied [value] as a field named [fieldName], if the value is non-null.
 */
fun JsonGenerator.writeNullableField(fieldName: String, value: Any?) {
    value?.let {
        when (it) {
            is String -> this.writeStringField(fieldName, it)
            is Double -> this.writeNumberField(fieldName, it)
            is Int -> this.writeNumberField(fieldName, it)
            is Boolean -> this.writeBooleanField(fieldName, it)
            else -> this.writeObjectField(fieldName, it)
        }
    }
}

/**
 * Writes the supplied [value] as a field based on the values type and prefixed by [prefix], if the value is non-null.
 */
fun JsonGenerator.writeDynamicValueField(prefix: String, value: DynamicValue<*>?) {
    value?.let { it.writeToJson(prefix, this) }
}
