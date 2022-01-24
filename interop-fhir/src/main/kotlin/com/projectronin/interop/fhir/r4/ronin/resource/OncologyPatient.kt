package com.projectronin.interop.fhir.r4.ronin.resource

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.OncologyPatientDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.OncologyPatientSerializer
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

/**
 * Project Ronin definition of an Oncology Patient.
 *
 * See [Project Ronin Profile Spec](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-patient.html)
 */
@JsonDeserialize(using = OncologyPatientDeserializer::class)
@JsonSerialize(using = OncologyPatientSerializer::class)
data class OncologyPatient(
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
    val name: List<HumanName>,
    val telecom: List<ContactPoint>,
    val gender: AdministrativeGender,
    val birthDate: Date,
    val deceased: DynamicValue<Any>? = null,
    val address: List<Address>,
    val maritalStatus: CodeableConcept,
    val multipleBirth: DynamicValue<Any>? = null,
    val photo: List<Attachment> = listOf(),
    val contact: List<Contact> = listOf(),
    val communication: List<Communication> = listOf(),
    val generalPractitioner: List<Reference> = listOf(),
    val managingOrganization: Reference? = null,
    val link: List<Link> = listOf()
) :
    RoninDomainResource(id, meta, implicitRules, language, text, contained, extension, modifierExtension, identifier) {
    companion object {
        val acceptedDeceasedTypes = listOf(DynamicValueType.BOOLEAN, DynamicValueType.DATE_TIME)
        val acceptedMultipleBirthTypes = listOf(DynamicValueType.BOOLEAN, DynamicValueType.INTEGER)
    }

    init {
        // Dynamic values
        deceased?.let {
            require(Patient.acceptedDeceasedTypes.contains(deceased.type)) {
                "Bad dynamic value indicating if the patient is deceased"
            }
        }

        multipleBirth?.let {
            require(Patient.acceptedMultipleBirthTypes.contains(multipleBirth.type)) {
                "Bad dynamic value indicating whether the patient was part of a multiple birth"
            }
        }

        // MRN
        val mrnIdentifier = identifier.find { it.system == CodeSystem.MRN.uri }
        requireNotNull(mrnIdentifier) { "mrn identifier is required" }

        mrnIdentifier.type?.let {
            require(it == CodeableConcepts.MRN) { "mrn identifier type defined without proper CodeableConcept" }
        }

        requireNotNull(mrnIdentifier.value) { "mrn value is required" }

        // fhir_stu3_id
        val fhirStu3IdIdentifier = identifier.find { it.system == CodeSystem.FHIR_STU3_ID.uri }
        requireNotNull(fhirStu3IdIdentifier) { "fhir_stu3_id identifier is required" }

        fhirStu3IdIdentifier.type?.let {
            require(it == CodeableConcepts.FHIR_STU3_ID) { "fhir_stu3_id identifier type defined without proper CodeableConcept" }
        }

        requireNotNull(fhirStu3IdIdentifier.value) { "fhir_stu3_id value is required" }

        // Name
        require(name.isNotEmpty()) { "At least one name must be provided" }

        // Telecom
        require(telecom.isNotEmpty()) { "At least one telecom must be provided" }
        require(
            telecom.all { (it.system != null) and (it.value != null) and (it.use != null) }
        ) { "Telecoms must have a system, value and use" }

        // Address
        require(address.isNotEmpty()) { "At least one address must be provided" }

        // Contact
        require(
            contact.all { (it.name != null) or (it.telecom.isNotEmpty()) or (it.address != null) or (it.organization != null) }
        ) { "[pat-1](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-patient.html#constraints): contact SHALL at least contain a contact's details or a reference to an organization" }
    }

    override val resourceType: String = "Patient"
}
