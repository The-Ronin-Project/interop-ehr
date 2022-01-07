package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.common.jackson.writeNullableField
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.r4.datatype.Annotation

/**
 * Jackson serializer for [Annotation]s
 */
class AnnotationSerializer : BaseElementSerializer<Annotation>(Annotation::class.java) {
    override fun serializeSpecificElement(value: Annotation, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeDynamicValueField("author", value.author)
        gen.writeNullableField("time", value.time)
        gen.writeObjectField("text", value.text)
    }
}
