package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.r4.datatype.Extension

/**
 * Jackson serializer for [Extension]s
 */
class ExtensionSerializer : BaseElementSerializer<Extension>(Extension::class.java) {
    override fun serializeSpecificElement(value: Extension, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeObjectField("url", value.url)
        gen.writeDynamicValueField("value", value.value)
    }
}
