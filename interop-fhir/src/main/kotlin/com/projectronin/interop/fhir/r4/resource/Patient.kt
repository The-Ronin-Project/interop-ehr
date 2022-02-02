package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.PatientDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.PatientSerializer
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Communication
import com.projectronin.interop.fhir.r4.datatype.Contact
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.PatientLink
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.base.BasePatient
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

/**
 * Demographics and other administrative information about an individual or animal receiving care or other
 * health-related services.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/R4/patient.html)
 */
@JsonDeserialize(using = PatientDeserializer::class)
@JsonSerialize(using = PatientSerializer::class)
@JsonTypeName("Patient")
data class Patient(
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val text: Narrative? = null,
    override val contained: List<ContainedResource> = listOf(),
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    override val identifier: List<Identifier> = listOf(),
    override val active: Boolean? = null,
    override val name: List<HumanName> = listOf(),
    override val telecom: List<ContactPoint> = listOf(),
    override val gender: AdministrativeGender? = null,
    override val birthDate: Date? = null,
    override val deceased: DynamicValue<Any>? = null,
    override val address: List<Address> = listOf(),
    override val maritalStatus: CodeableConcept? = null,
    override val multipleBirth: DynamicValue<Any>? = null,
    override val photo: List<Attachment> = listOf(),
    override val contact: List<Contact> = listOf(),
    override val communication: List<Communication> = listOf(),
    override val generalPractitioner: List<Reference> = listOf(),
    override val managingOrganization: Reference? = null,
    override val link: List<PatientLink> = listOf()
) : DomainResource, BasePatient() {
    init {
        validate()
    }
}
