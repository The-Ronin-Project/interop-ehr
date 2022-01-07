package com.projectronin.interop.fhir.jackson

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.projectronin.interop.common.jackson.fieldsStartingWith
import com.projectronin.interop.fhir.r4.datatype.DynamicValue

/**
 * Finds the [DynamicValue] on the node for the supplied [prefix] using the [currentParser].
 */
fun JsonNode.getDynamicValue(prefix: String, currentParser: JsonParser): DynamicValue<Any> {
    return getDynamicValueOrNull(prefix, currentParser)
        ?: throw JsonParseException(currentParser, "No value fields found")
}

/**
 * Finds the [DynamicValue] on the node for the supplied [prefix] using the [currentParser] if present, or null.
 */
fun JsonNode.getDynamicValueOrNull(prefix: String, currentParser: JsonParser): DynamicValue<Any>? {
    val valueFields = this.fieldsStartingWith(prefix)
    val valueField = when (valueFields.size) {
        1 -> valueFields[0]
        0 -> return null
        else -> throw JsonParseException(currentParser, "Multiple value fields found")
    }

    val type = valueField.substring(prefix.length)
    return DynamicValueResolver(currentParser).resolveDynamicValue(this.get(valueField), type)
}
