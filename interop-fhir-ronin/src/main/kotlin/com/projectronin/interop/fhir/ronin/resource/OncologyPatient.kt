package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

/**
 * Validator and Transformer for the Ronin [OncologyPatient](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-patient.html) profile.
 */
class OncologyPatient private constructor(private val identifierService: IdentifierService) :
    BaseRoninProfile<Patient>(KotlinLogging.logger { }) {
    companion object {
        /**
         * Creates an OncologyPatient with the supplied [identifierService].
         */
        fun create(identifierService: IdentifierService): OncologyPatient =
            OncologyPatient(identifierService)
    }

    override fun validate(resource: Patient) {
        requireTenantIdentifier(resource.identifier)

        val mrnIdentifier = resource.identifier.find { it.system == CodeSystem.MRN.uri }
        requireNotNull(mrnIdentifier) {
            "mrn identifier is required"
        }

        mrnIdentifier.type?.let { type ->
            require(type == CodeableConcepts.MRN) {
                "mrn identifier type defined without proper CodeableConcept"
            }
        }

        requireNotNull(mrnIdentifier.value) {
            "mrn value is required"
        }

        val fhirStu3IdIdentifier = resource.identifier.find { it.system == CodeSystem.FHIR_STU3_ID.uri }
        requireNotNull(fhirStu3IdIdentifier) {
            "fhir_stu3_id identifier is required"
        }

        fhirStu3IdIdentifier.type?.let { type ->
            require(type == CodeableConcepts.FHIR_STU3_ID) {
                "fhir_stu3_id identifier type defined without proper CodeableConcept"
            }
        }

        requireNotNull(fhirStu3IdIdentifier.value) {
            "fhir_stu3_id value is required"
        }

        require(resource.name.isNotEmpty()) {
            "At least one name must be provided"
        }
    }

    override fun transformInternal(original: Patient, tenant: Tenant): Patient? {
        val id = original.id
        if (id == null) {
            logger.warn { "Unable to transform patient due to missing ID" }
            return null
        }

        val gender = original.gender
        if (gender == null) {
            logger.warn { "Unable to transform patient due to missing gender" }
            return null
        }

        val birthDate = original.birthDate
        if (birthDate == null) {
            logger.warn { "Unable to transform patient due to missing birth date" }
        }

        val maritalStatus = original.maritalStatus ?: CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
                    code = Code("NI"),
                    display = "NoInformation"
                )
            )
        )

        val fhirStu3IdIdentifier = Identifier(
            value = id.value,
            system = CodeSystem.FHIR_STU3_ID.uri,
            type = CodeableConcepts.FHIR_STU3_ID
        )

        val existingMRN = try {
            identifierService.getMRNIdentifier(tenant, original.identifier)
        } catch (e: Exception) {
            logger.warn(e) { "Unable to find MRN for patient: ${e.message}" }
            return null
        }

        val mrnIdentifier = Identifier(
            value = existingMRN.value,
            system = CodeSystem.MRN.uri,
            type = CodeableConcepts.MRN
        )

        return original.copy(
            id = id.localize(tenant),
            meta = original.meta?.localize(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier() +
                fhirStu3IdIdentifier + mrnIdentifier,
            name = original.name.map { it.localize(tenant) },
            telecom = original.telecom.map { it.localize(tenant) },
            address = original.address.map { it.localize(tenant) },
            maritalStatus = maritalStatus,
            photo = original.photo.map { it.localize(tenant) },
            contact = original.contact.map { it.localize(tenant) },
            communication = original.communication.map { it.localize(tenant) },
            generalPractitioner = original.generalPractitioner.map { it.localize(tenant) },
            managingOrganization = original.managingOrganization?.localize(tenant),
            link = original.link.map { it.localize(tenant) }
        )
    }
}
