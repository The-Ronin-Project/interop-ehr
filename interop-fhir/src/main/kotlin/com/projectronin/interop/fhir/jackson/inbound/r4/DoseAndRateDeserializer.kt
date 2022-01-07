package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.common.jackson.getAsList
import com.projectronin.interop.common.jackson.getAsOrNull
import com.projectronin.interop.common.jackson.getAsTextOrNull
import com.projectronin.interop.fhir.jackson.getDynamicValueOrNull
import com.projectronin.interop.fhir.r4.datatype.DoseAndRate

/**
 * Jackson deserializer for [DoseAndRate]s
 */
class DoseAndRateDeserializer : StdDeserializer<DoseAndRate>(DoseAndRate::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DoseAndRate {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return DoseAndRate(
            id = node.getAsTextOrNull("id"),
            extension = node.getAsList("extension", p),
            type = node.getAsOrNull("type", p),
            dose = node.getDynamicValueOrNull("dose", p),
            rate = node.getDynamicValueOrNull("rate", p)
        )
    }
}
