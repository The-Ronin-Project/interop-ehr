package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.jackson.writeListField
import com.projectronin.interop.fhir.jackson.writeNullableField
import com.projectronin.interop.fhir.r4.datatype.DataRequirement

/**
 * Jackson serializer for [DataRequirement]s
 */
class DataRequirementSerializer : BaseElementSerializer<DataRequirement>(DataRequirement::class.java) {
    override fun serializeSpecificElement(value: DataRequirement, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeObjectField("type", value.type)
        gen.writeListField("profile", value.profile)
        gen.writeDynamicValueField("subject", value.subject)
        gen.writeListField("mustSupport", value.mustSupport)
        gen.writeListField("codeFilter", value.codeFilter)
        gen.writeListField("dateFilter", value.dateFilter)
        gen.writeNullableField("limit", value.limit)
        gen.writeListField("sort", value.sort)
    }
}
