package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.jackson.writeNullableField
import com.projectronin.interop.fhir.r4.datatype.DoseAndRate

/**
 * Jackson serializer for [DoseAndRate]s
 */
class DoseAndRateSerializer : BaseElementSerializer<DoseAndRate>(DoseAndRate::class.java) {
    override fun serializeSpecificElement(value: DoseAndRate, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeNullableField("type", value.type)
        gen.writeDynamicValueField("dose", value.dose)
        gen.writeDynamicValueField("rate", value.rate)
    }
}
