package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.common.jackson.writeListField
import com.projectronin.interop.common.jackson.writeNullableField
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.r4.datatype.TriggerDefinition

/**
 * Jackson serializer for [TriggerDefinition]s
 */
class TriggerDefinitionSerializer : BaseElementSerializer<TriggerDefinition>(TriggerDefinition::class.java) {
    override fun serializeSpecificElement(value: TriggerDefinition, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeObjectField("type", value.type)
        gen.writeNullableField("name", value.name)
        gen.writeDynamicValueField("timing", value.timing)
        gen.writeListField("data", value.data)
        gen.writeNullableField("condition", value.condition)
    }
}
