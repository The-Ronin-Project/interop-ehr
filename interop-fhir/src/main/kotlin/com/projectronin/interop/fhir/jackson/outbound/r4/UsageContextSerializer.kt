package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.r4.datatype.UsageContext

/**
 * Jackson serializer for [UsageContext]s
 */
class UsageContextSerializer : BaseElementSerializer<UsageContext>(UsageContext::class.java) {
    override fun serializeSpecificElement(value: UsageContext, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeObjectField("code", value.code)
        gen.writeDynamicValueField("value", value.value)
    }
}
