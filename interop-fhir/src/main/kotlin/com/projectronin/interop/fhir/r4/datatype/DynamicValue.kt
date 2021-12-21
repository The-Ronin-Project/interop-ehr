package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode

/**
 * Represents a dynamic value. These tend to be shown as "value```[x]```" within FHIR documentation.
 */
data class DynamicValue<T>(
    val type: DynamicValueType,
    val value: T
) {
    /**
     * Writes this value to the [gen] under a field based on the value's type and the [prefix]
     */
    fun writeToJson(prefix: String, gen: JsonGenerator) {
        gen.writeObjectField("$prefix${type.code}", value)
    }
}

/**
 * Interface for reading values for a [DynamicValue].
 */
interface DynamicValueReader {
    /**
     * Reads the value from the supplied [jsonNode], using the [jsonParser] if needed.
     */
    fun readValue(jsonNode: JsonNode, jsonParser: JsonParser): Any
}
