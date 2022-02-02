package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.fhir.r4.resource.UnknownResource

/**
 * Jackson serializer for [UnknownResource]s
 */
class UnknownResourceSerializer : BaseResourceSerializer<UnknownResource>(UnknownResource::class.java) {
    override fun serializeSpecificElement(value: UnknownResource, gen: JsonGenerator, provider: SerializerProvider) {
        value.otherData.entries.forEach { (key, value) ->
            value?.let {
                gen.writeObjectField(key, it)
            }
        }
    }
}
