package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.fhir.jackson.getAs
import com.projectronin.interop.fhir.jackson.getAsList
import com.projectronin.interop.fhir.jackson.getAsTextOrNull
import com.projectronin.interop.fhir.jackson.getDynamicValue
import com.projectronin.interop.fhir.r4.datatype.UsageContext

/**
 * Jackson deserializer for [UsageContext]s
 */
class UsageContextDeserializer : StdDeserializer<UsageContext>(UsageContext::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UsageContext {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return UsageContext(
            id = node.getAsTextOrNull("id"),
            extension = node.getAsList("extension", p),
            code = node.getAs("code", p),
            value = node.getDynamicValue("value", p)
        )
    }
}
