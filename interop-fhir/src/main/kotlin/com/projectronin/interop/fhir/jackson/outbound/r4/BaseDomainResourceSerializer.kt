package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.projectronin.interop.fhir.jackson.writeListField
import com.projectronin.interop.fhir.jackson.writeNullableField
import com.projectronin.interop.fhir.r4.resource.DomainResource

/**
 * Base serializer for helping serialize [DomainResource]s
 */
abstract class BaseDomainResourceSerializer<T : DomainResource>(clazz: Class<T>) : StdSerializer<T>(clazz) {
    /**
     * Writes the specific element attributes of [value] to the [gen]. Common element items such as id and extension do not need to be written.
     */
    abstract fun serializeSpecificElement(value: T, gen: JsonGenerator, provider: SerializerProvider)

    override fun serialize(value: T, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNullableField("resourceType", value.resourceType)
        gen.writeNullableField("id", value.id)
        gen.writeNullableField("meta", value.meta)
        gen.writeNullableField("implicitRules", value.implicitRules)
        gen.writeNullableField("language", value.language)
        gen.writeNullableField("text", value.text)
        gen.writeListField("contained", value.contained)
        gen.writeListField("extension", value.extension)
        gen.writeListField("modifierExtension", value.modifierExtension)

        serializeSpecificElement(value, gen, provider)

        gen.writeEndObject()
    }
}
