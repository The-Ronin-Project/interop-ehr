package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.fhir.jackson.getAs
import com.projectronin.interop.fhir.jackson.getAsList
import com.projectronin.interop.fhir.jackson.getAsOrNull
import com.projectronin.interop.fhir.jackson.getAsTextOrNull
import com.projectronin.interop.fhir.jackson.getDynamicValueOrNull
import com.projectronin.interop.fhir.r4.datatype.Annotation

/**
 * Jackson deserializer for [Annotation]s
 */
class AnnotationDeserializer : StdDeserializer<Annotation>(Annotation::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Annotation {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return Annotation(
            id = node.getAsTextOrNull("id"),
            extension = node.getAsList("extension", p),
            author = node.getDynamicValueOrNull("author", p),
            time = node.getAsOrNull("time", p),
            text = node.getAs("text", p)
        )
    }
}
