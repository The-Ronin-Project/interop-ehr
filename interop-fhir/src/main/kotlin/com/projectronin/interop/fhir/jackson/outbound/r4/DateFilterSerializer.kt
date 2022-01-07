package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.common.jackson.writeNullableField
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.r4.datatype.DateFilter

/**
 * Jackson serializer for [DateFilter]s
 */
class DateFilterSerializer : BaseElementSerializer<DateFilter>(DateFilter::class.java) {
    override fun serializeSpecificElement(value: DateFilter, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeNullableField("path", value.path)
        gen.writeNullableField("searchParam", value.searchParam)
        gen.writeDynamicValueField("value", value.value)
    }
}
