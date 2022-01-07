package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.common.jackson.writeListField
import com.projectronin.interop.common.jackson.writeNullableField
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.r4.datatype.Dosage

/**
 * Jackson serializer for [Dosage]s
 */
class DosageSerializer : BaseElementSerializer<Dosage>(Dosage::class.java) {
    override fun serializeSpecificElement(value: Dosage, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeListField("modifierExtension", value.modifierExtension)
        gen.writeNullableField("sequence", value.sequence)
        gen.writeNullableField("text", value.text)
        gen.writeListField("additionalInstruction", value.additionalInstruction)
        gen.writeNullableField("patientInstruction", value.patientInstruction)
        gen.writeNullableField("timing", value.timing)
        gen.writeDynamicValueField("asNeeded", value.asNeeded)
        gen.writeNullableField("site", value.site)
        gen.writeNullableField("route", value.route)
        gen.writeNullableField("method", value.method)
        gen.writeListField("doseAndRate", value.doseAndRate)
        gen.writeNullableField("maxDosePerPeriod", value.maxDosePerPeriod)
        gen.writeNullableField("maxDosePerAdministration", value.maxDosePerAdministration)
        gen.writeNullableField("maxDosePerLifetime", value.maxDosePerLifetime)
    }
}
