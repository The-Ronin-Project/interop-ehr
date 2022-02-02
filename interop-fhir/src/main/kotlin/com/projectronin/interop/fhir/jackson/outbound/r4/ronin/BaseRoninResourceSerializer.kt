package com.projectronin.interop.fhir.jackson.outbound.r4.ronin

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.projectronin.interop.common.jackson.writeNullableField
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.ronin.resource.RoninResource

/**
 * Base serializer for helping serialize [Resource]s
 */
abstract class BaseRoninResourceSerializer<T : RoninResource>(clazz: Class<T>) : StdSerializer<T>(clazz) {
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

        serializeSpecificElement(value, gen, provider)

        gen.writeEndObject()
    }

    override fun serializeWithType(
        value: T,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer
    ) {
        serialize(value, gen, serializers)
    }
}
