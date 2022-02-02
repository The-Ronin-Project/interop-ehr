package com.projectronin.interop.fhir.r4.ronin.resource

import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.ronin.OncologyPatientDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.ronin.OncologyPatientSerializer
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.base.BasePatient
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

/**
 * Project Ronin definition of an Oncology Patient.
 *
 * See [Project Ronin Profile Spec](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-patient.html)
 */
@JsonDeserialize(using = OncologyPatientDeserializer::class)
@JsonSerialize(using = OncologyPatientSerializer::class)
@JsonTypeName("Patient")
data class OncologyPatient(
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
    override val name: List<HumanName>,
    override val telecom: List<ContactPoint>,
    override val gender: AdministrativeGender,
    override val birthDate: Date,
    override val deceased: DynamicValue<Any>? = null,
    override val address: List<Address>,
    override val maritalStatus: CodeableConcept,
    override val multipleBirth: DynamicValue<Any>? = null,
    override val photo: List<Attachment> = listOf(),
    override val contact: List<Contact> = listOf(),
    override val communication: List<Communication> = listOf(),
    override val generalPractitioner: List<Reference> = listOf(),
    override val managingOrganization: Reference? = null,
    override val link: List<PatientLink> = listOf()
) : RoninDomainResource, BasePatient() {
    init {
        validate()

        requireTenantIdentifier(identifier)

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
    }
}
