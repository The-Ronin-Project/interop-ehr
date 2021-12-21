package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.projectronin.interop.fhir.jackson.writeListField
import com.projectronin.interop.fhir.jackson.writeNullableField
import com.projectronin.interop.fhir.r4.datatype.Element

/**
 * Base serializer for helping serialize [Element]s
 */
abstract class BaseElementSerializer<T : Element>(clazz: Class<T>) : StdSerializer<T>(clazz) {
    /**
     * Writes the specific element attributes of [value] to the [gen]. Common element items such as id and extension do not need to written.
     */
    abstract fun serializeSpecificElement(value: T, gen: JsonGenerator, provider: SerializerProvider)

    override fun serialize(value: T, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNullableField("id", value.id)
        gen.writeListField("extension", value.extension)

        serializeSpecificElement(value, gen, provider)

        gen.writeEndObject()
    }
}
