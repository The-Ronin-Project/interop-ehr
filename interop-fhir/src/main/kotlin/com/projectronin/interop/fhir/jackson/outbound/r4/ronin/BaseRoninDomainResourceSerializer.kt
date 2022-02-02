package com.projectronin.interop.fhir.jackson.outbound.r4.ronin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.common.jackson.writeListField
import com.projectronin.interop.common.jackson.writeNullableField
import com.projectronin.interop.fhir.r4.resource.DomainResource
import com.projectronin.interop.fhir.r4.ronin.resource.RoninDomainResource

/**
 * Base serializer for helping serialize [DomainResource]s
 */
abstract class BaseRoninDomainResourceSerializer<T : RoninDomainResource>(clazz: Class<T>) :
    BaseRoninResourceSerializer<T>(clazz) {
    /**
     * Writes the specific domain element attributes of [value] to the [gen]. Common element items such as id and extension do not need to be written.
     */
    abstract fun serializeSpecificDomainElement(value: T, gen: JsonGenerator, provider: SerializerProvider)

    override fun serializeSpecificElement(value: T, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeNullableField("text", value.text)
        gen.writeListField("contained", value.contained)
        gen.writeListField("extension", value.extension)
        gen.writeListField("modifierExtension", value.modifierExtension)

        serializeSpecificDomainElement(value, gen, provider)
    }
}
