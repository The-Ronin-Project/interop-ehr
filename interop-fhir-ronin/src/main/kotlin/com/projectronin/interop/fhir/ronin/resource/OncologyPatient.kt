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
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
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

    override fun validateInternal(resource: Patient, validation: Validation) {
        validation.apply {
            requireTenantIdentifier(resource.identifier, this)

            val mrnIdentifier = resource.identifier.find { it.system == CodeSystem.MRN.uri }
            notNull(mrnIdentifier) {
                "mrn identifier is required"
            }

            ifNotNull(mrnIdentifier) {
                mrnIdentifier.type?.let { type ->
                    check(type == CodeableConcepts.MRN) {
                        "mrn identifier type defined without proper CodeableConcept"
                    }
                }

                notNull(mrnIdentifier.value) {
                    "mrn value is required"
                }
            }

            val fhirStu3IdIdentifier = resource.identifier.find { it.system == CodeSystem.FHIR_STU3_ID.uri }
            notNull(fhirStu3IdIdentifier) {
                "fhir_stu3_id identifier is required"
            }

            ifNotNull(fhirStu3IdIdentifier) {
                fhirStu3IdIdentifier.type?.let { type ->
                    check(type == CodeableConcepts.FHIR_STU3_ID) {
                        "fhir_stu3_id identifier type defined without proper CodeableConcept"
                    }
                }

                notNull(fhirStu3IdIdentifier.value) {
                    "fhir_stu3_id value is required"
                }
            }

            check(resource.name.isNotEmpty()) {
                "At least one name must be provided"
            }
        }
    }

    override fun transformInternal(original: Patient, tenant: Tenant): Pair<Patient, Validation> {
        val validation = validation {
            notNull(original.id) { "no FHIR id" }
            notNull(original.gender) { "no gender" }
        }

        val birthDate = original.birthDate
        if (birthDate == null) {
            logger.warn { "patient ${original.id} is missing birth date" }
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

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta?.localize(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier() +
                getRoninIdentifiers(original, tenant),
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
        return Pair(transformed, validation)
    }

    fun getRoninIdentifiers(patient: Patient, tenant: Tenant): List<Identifier> {
        val roninIdentifiers = mutableListOf<Identifier>()
        patient.id?.let {
            roninIdentifiers.add(
                Identifier(
                    value = it.value,
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    type = CodeableConcepts.FHIR_STU3_ID
                )
            )
        }

        try {
            val existingMRN = identifierService.getMRNIdentifier(tenant, patient.identifier)
            roninIdentifiers.add(
                Identifier(
                    value = existingMRN.value,
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN
                )
            )
        } catch (e: Exception) {
            // We will handle this during validation.
        }
        return roninIdentifiers
    }
}
