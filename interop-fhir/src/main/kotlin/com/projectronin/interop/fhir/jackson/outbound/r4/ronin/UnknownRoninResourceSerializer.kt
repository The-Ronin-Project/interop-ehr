package com.projectronin.interop.fhir.jackson.outbound.r4.ronin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.fhir.r4.ronin.resource.UnknownRoninResource

/**
 * Jackson serializer for [UnknownRoninResource]s
 */
class UnknownRoninResourceSerializer :
    BaseRoninResourceSerializer<UnknownRoninResource>(UnknownRoninResource::class.java) {
    override fun serializeSpecificElement(
        value: UnknownRoninResource,
        gen: JsonGenerator,
        provider: SerializerProvider
    ) {
        value.otherData.entries.forEach { (key, value) ->
            value?.let {
                gen.writeObjectField(key, it)
            }
        }
    }
}
