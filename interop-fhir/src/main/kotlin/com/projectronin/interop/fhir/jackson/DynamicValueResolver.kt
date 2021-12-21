package com.projectronin.interop.fhir.jackson

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType

/**
 * Resolver for [DynamicValue]s.
 */
class DynamicValueResolver(private val jsonParser: JsonParser) {
    /**
     * Resolves a [DynamicValue] from the supplied [node] based off the [typeString].
     */
    fun resolveDynamicValue(node: JsonNode, typeString: String): DynamicValue<Any> {
        val type = CodedEnum.byCode<DynamicValueType>(typeString)
            ?: throw JsonParseException(jsonParser, "Unknown type for a dynamic value")
        val value = type.readValue(node, jsonParser)

        return DynamicValue(type, value)
    }
}
