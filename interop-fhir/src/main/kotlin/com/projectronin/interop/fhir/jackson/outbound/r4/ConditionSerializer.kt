package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.common.jackson.writeListField
import com.projectronin.interop.common.jackson.writeNullableField
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.r4.resource.Condition

/**
 * Jackson serializer for [Condition]s
 */
class ConditionSerializer : BaseDomainResourceSerializer<Condition>(Condition::class.java) {
    override fun serializeSpecificDomainElement(value: Condition, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeListField("identifier", value.identifier)
        gen.writeNullableField("clinicalStatus", value.clinicalStatus)
        gen.writeNullableField("verificationStatus", value.verificationStatus)
        gen.writeListField("category", value.category)
        gen.writeNullableField("severity", value.severity)
        gen.writeNullableField("code", value.code)
        gen.writeListField("bodySite", value.bodySite)
        gen.writeObjectField("subject", value.subject)
        gen.writeNullableField("encounter", value.encounter)
        gen.writeDynamicValueField("onset", value.onset)
        gen.writeDynamicValueField("abatement", value.abatement)
        gen.writeNullableField("recordedDate", value.recordedDate)
        gen.writeNullableField("recorder", value.recorder)
        gen.writeNullableField("asserter", value.asserter)
        gen.writeListField("stage", value.stage)
        gen.writeListField("evidence", value.evidence)
        gen.writeListField("note", value.note)
    }
}
