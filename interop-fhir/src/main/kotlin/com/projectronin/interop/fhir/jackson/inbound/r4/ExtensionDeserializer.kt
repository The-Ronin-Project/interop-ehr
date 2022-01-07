package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.common.jackson.getAs
import com.projectronin.interop.common.jackson.getAsList
import com.projectronin.interop.common.jackson.getAsTextOrNull
import com.projectronin.interop.fhir.jackson.getDynamicValue
import com.projectronin.interop.fhir.r4.datatype.Extension

/**
 * Jackson deserializer for [Extension]s
 */
class ExtensionDeserializer : StdDeserializer<Extension>(Extension::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Extension {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return Extension(
            id = node.getAsTextOrNull("id"),
            extension = node.getAsList("extension", p),
            url = node.getAs("url", p),
            value = node.getDynamicValue("value", p)
        )
    }
}
