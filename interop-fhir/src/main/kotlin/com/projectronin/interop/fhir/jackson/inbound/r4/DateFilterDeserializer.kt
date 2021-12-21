package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.fhir.jackson.getAsList
import com.projectronin.interop.fhir.jackson.getAsTextOrNull
import com.projectronin.interop.fhir.jackson.getDynamicValueOrNull
import com.projectronin.interop.fhir.r4.datatype.DateFilter

/**
 * Jackson deserializer for [DateFilter]s
 */
class DateFilterDeserializer : StdDeserializer<DateFilter>(DateFilter::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DateFilter {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return DateFilter(
            id = node.getAsTextOrNull("id"),
            extension = node.getAsList("extension", p),
            path = node.getAsTextOrNull("path"),
            searchParam = node.getAsTextOrNull("searchParam"),
            value = node.getDynamicValueOrNull("value", p)
        )
    }
}
