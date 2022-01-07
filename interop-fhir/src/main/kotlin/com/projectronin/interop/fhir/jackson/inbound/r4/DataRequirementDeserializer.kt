package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.common.jackson.getAs
import com.projectronin.interop.common.jackson.getAsList
import com.projectronin.interop.common.jackson.getAsOrNull
import com.projectronin.interop.common.jackson.getAsTextOrNull
import com.projectronin.interop.fhir.jackson.getDynamicValueOrNull
import com.projectronin.interop.fhir.r4.datatype.DataRequirement

/**
 * Jackson deserializer for [DataRequirement]s
 */
class DataRequirementDeserializer : StdDeserializer<DataRequirement>(DataRequirement::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DataRequirement {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return DataRequirement(
            id = node.getAsTextOrNull("id"),
            extension = node.getAsList("extension", p),
            type = node.getAs("type", p),
            profile = node.getAsList("profile", p),
            subject = node.getDynamicValueOrNull("subject", p),
            mustSupport = node.getAsList("mustSupport", p),
            codeFilter = node.getAsList("codeFilter", p),
            dateFilter = node.getAsList("dateFilter", p),
            limit = node.getAsOrNull("limit", p),
            sort = node.getAsList("sort", p)
        )
    }
}
