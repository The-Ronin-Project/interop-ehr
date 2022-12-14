package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

object RoninLaboratoryResult :
    BaseRoninObservation(R4ObservationValidator, RoninProfile.OBSERVATION_LABORATORY_RESULT.value) {
    internal val laboratoryCode = Code("laboratory")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninLaboratoryResult
    override val validDerivedFromValues = listOf("DocumentReference", "ImagingStudy", "Media", "MolecularSequence", "Observation", "QuestionnaireResponse")
    override val validHasMemberValues = listOf("MolecularSequence", "Observation", "QuestionnaireResponse")
    override val validSubjectValues = listOf("Patient")
    override val validPartOfValues = listOf("ImagingStudy", "Immunization", "MedicationAdministration", "MedicationDispense", "MedicationStatement", "Procedure")
    override val validPerformerValues = listOf("Patient", "Practitioner", "PractitionerRole", "RelatedPerson", "Organization")

    override fun qualifies(resource: Observation): Boolean {
        return resource.category.any { category ->
            category.coding.any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == laboratoryCode }
        }
    }

    private val requiredCategoryError = RequiredFieldError(Observation::category)
    private val requiredLaboratoryCodeError = FHIRError(
        code = "USCORE_LABOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding list for laboratory result category must have code \"${laboratoryCode.value}\" with system \"${CodeSystem.OBSERVATION_CATEGORY.uri.value}\"",
        location = LocationContext(Observation::category)
    )

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)
        validation.apply {
            checkNotNull(element.category, requiredCategoryError, parentContext)
            checkTrue(element.category.isNotEmpty(), requiredCategoryError, parentContext)
        }

        // category and code (basics), dataAbsentReason, effective, status - validated by R4ObservationValidator
    }

    private val singleLaboratoryCodeError = FHIRError(
        code = "RONIN_LABOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding list for laboratory result code is restricted to 1 entry",
        location = LocationContext(Observation::code)
    )

    override fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(
                element.category.flatMap { it.coding }
                    .any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == laboratoryCode },
                requiredLaboratoryCodeError,
                parentContext
            )
            val codingList = element.code?.coding
            ifNotNull(codingList) {
                checkTrue((codingList!!.size <= 1), singleLaboratoryCodeError, parentContext)
            }
        }
        requireCodeableConcept("code", element.code, parentContext, validation)
        requireCodeCoding("code", element.code?.coding, parentContext, validation)
    }

    private val requiredIdError = RequiredFieldError(Observation::id)

    override fun transformInternal(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Observation?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val tenantSourceCodeExtension = getExtensionOrEmptyList(
            RoninExtension.TENANT_SOURCE_OBSERVATION_CODE,
            normalized.code
        )

        // TODO: specimen - requires url == http://projectronin.io/fhir/StructureDefinition/ronin-specimen

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            extension = normalized.extension + tenantSourceCodeExtension,
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )
        return Pair(transformed, validation)
    }
}
