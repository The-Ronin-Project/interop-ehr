package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.fhir.r4.resource.ContainedResource

/**
 * Jackson deserializer for [ContainedResource]s
 */
class ContainedResourceDeserializer : StdDeserializer<ContainedResource>(ContainedResource::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ContainedResource {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return ContainedResource(node.toString())
    }
}
