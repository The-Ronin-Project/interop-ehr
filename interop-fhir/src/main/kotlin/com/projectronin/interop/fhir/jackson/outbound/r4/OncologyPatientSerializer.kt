package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.common.jackson.writeListField
import com.projectronin.interop.common.jackson.writeNullableField
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPatient

/**
 * Jackson serializer for [OncologyPatient]s
 */
class OncologyPatientSerializer : BaseDomainResourceSerializer<OncologyPatient>(OncologyPatient::class.java) {
    override fun serializeSpecificElement(value: OncologyPatient, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeListField("identifier", value.identifier)
        gen.writeNullableField("active", value.active)
        gen.writeListField("name", value.name)
        gen.writeListField("telecom", value.telecom)
        gen.writeNullableField("gender", value.gender)
        gen.writeNullableField("birthDate", value.birthDate)
        gen.writeDynamicValueField("deceased", value.deceased)
        gen.writeListField("address", value.address)
        gen.writeNullableField("maritalStatus", value.maritalStatus)
        gen.writeDynamicValueField("multipleBirth", value.multipleBirth)
        gen.writeListField("photo", value.photo)
        gen.writeListField("contact", value.contact)
        gen.writeListField("communication", value.communication)
        gen.writeListField("generalPractitioner", value.generalPractitioner)
        gen.writeNullableField("managingOrganization", value.managingOrganization)
        gen.writeListField("link", value.link)
    }
}
