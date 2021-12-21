package com.projectronin.interop.fhir.jackson

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.projectronin.interop.fhir.r4.datatype.DynamicValue

/**
 * Reads the supplied [fieldName] from the node and returns the data as a String if present, or null.
 */
fun JsonNode.getAsTextOrNull(fieldName: String): String? = this.get(fieldName)?.asText()

/**
 * Reads the supplied [fieldName] from the node and returns the data as an Int if present, or null.
 */
fun JsonNode.getAsIntOrNull(fieldName: String): Int? = this.get(fieldName)?.asInt()

/**
 * Reads the supplied [fieldName] from the node and returns the data as a Double if present, or null.
 */
fun JsonNode.getAsDoubleOrNull(fieldName: String): Double? = this.get(fieldName)?.asDouble()

/**
 * Reads the supplied [fieldName] from the node and returns the data as the requested type [T] using the [currentParser].
 */
inline fun <reified T> JsonNode.getAs(fieldName: String, currentParser: JsonParser): T =
    this.getAsOrNull(fieldName, currentParser)!!

/**
 * Reads the supplied [fieldName] from the node and returns the data as the requested type [T] using the [currentParser] if present, or null.
 */
inline fun <reified T> JsonNode.getAsOrNull(fieldName: String, currentParser: JsonParser): T? =
    this.get(fieldName)?.readValueAs(currentParser, T::class.java)

/**
 * Reads the supplied [fieldName] from the node and returns the data as a List of the requested type [T] using the [currentParser] if present, or an empty List.
 */
inline fun <reified T> JsonNode.getAsList(fieldName: String, currentParser: JsonParser): List<T> {
    val parser = this.get(fieldName)?.traverse() ?: return listOf()
    parser.codec = currentParser.codec
    return parser.readValueAs(object : TypeReference<List<T>>() {})
}

/**
 * Reads the current value as type [T] using the [currentParser].
 */
fun <T> JsonNode.readValueAs(currentParser: JsonParser, clazz: Class<T>): T {
    val parser = this.traverse()
    parser.codec = currentParser.codec
    return parser.readValueAs(clazz)
}

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

/**
 * Returns the name of all fields on the node starting with the [prefix].
 */
fun JsonNode.fieldsStartingWith(prefix: String): List<String> =
    this.fieldNames().asSequence().filter { it.startsWith(prefix) }.toList()
