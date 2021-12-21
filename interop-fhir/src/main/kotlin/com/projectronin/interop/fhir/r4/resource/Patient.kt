package com.projectronin.interop.fhir.r4.resource

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
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Link
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

/**
 * Demographics and other administrative information about an individual or animal receiving care or other
 * health-related services.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/patient.html)
 */
@JsonDeserialize(using = PatientDeserializer::class)
@JsonSerialize(using = PatientSerializer::class)
data class Patient(
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val text: Narrative? = null,
    override val contained: List<ContainedResource> = listOf(),
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val identifier: List<Identifier> = listOf(),
    val active: Boolean? = null,
    val name: List<HumanName> = listOf(),
    val telecom: List<ContactPoint> = listOf(),
    val gender: AdministrativeGender? = null,
    val birthDate: Date? = null,
    val deceased: DynamicValue<Any>? = null,
    val address: List<Address> = listOf(),
    val maritalStatus: CodeableConcept? = null,
    val multipleBirth: DynamicValue<Any>? = null,
    val photo: List<Attachment> = listOf(),
    val contact: List<Contact> = listOf(),
    val communication: List<Communication> = listOf(),
    val generalPractitioner: List<Reference> = listOf(),
    val managingOrganization: Reference? = null,
    val link: List<Link> = listOf()
) : DomainResource {
    companion object {
        val acceptedDeceasedTypes = listOf(DynamicValueType.BOOLEAN, DynamicValueType.DATE_TIME)
        val acceptedMultipleBirtTypes = listOf(DynamicValueType.BOOLEAN, DynamicValueType.INTEGER)
    }

    init {
        deceased?.let {
            require(acceptedDeceasedTypes.contains(deceased.type)) { "deceased can only be one of the following: ${acceptedDeceasedTypes.joinToString { it.code }}" }
        }

        multipleBirth?.let {
            require(acceptedMultipleBirtTypes.contains(multipleBirth.type)) { "multipleBirth can only be one of the following: ${acceptedMultipleBirtTypes.joinToString { it.code }}" }
        }

        require(
            contact.all {
                (it.name != null) or (it.telecom.isNotEmpty()) or (it.address != null) or (it.organization != null)
            }
        ) { "contact SHALL at least contain a contact's details or a reference to an organization." }
    }
}
