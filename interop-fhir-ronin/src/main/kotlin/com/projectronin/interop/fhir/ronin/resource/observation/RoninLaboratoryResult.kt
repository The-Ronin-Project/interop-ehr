package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.hasDayFormat
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
        description = "Laboratory result must use code \"${laboratoryCode.value}\" in system \"${CodeSystem.OBSERVATION_CATEGORY.uri.value}\"",
        location = LocationContext(Observation::category)
    )
    private val invalidQuantitySystemError = FHIRError(
        code = "USCORE_LABOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Quantity system must be UCUM",
        location = LocationContext("Observation", "valueQuantity.system")
    )
    private val noChildValueOrDataAbsentReasonError = FHIRError(
        code = "USCORE_LABOBS_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "If there is no component or hasMember element then either a value[x] or a data absent reason must be present",
        location = LocationContext(Observation::class)
    )
    private val invalidDateTimeError = FHIRError(
        code = "USCORE_LABOBS_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "Datetime must be at least to day",
        location = LocationContext(Observation::effective)
    )
    private val invalidCodeSystemError = FHIRError(
        code = "RONIN_LABOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Code system must be LOINC",
        location = LocationContext(Observation::code)
    )

    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateRonin(element, parentContext, validation)
        validation.apply {
            if (element.code != null) {
                element.code?.coding?.let { coding ->
                    checkTrue(
                        coding.all { it.system == CodeSystem.LOINC.uri },
                        invalidCodeSystemError,
                        parentContext
                    )
                }
            }
        }
    }

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)
        validation.apply {
            checkNotNull(element.category, requiredCategoryError, parentContext)
            checkTrue(element.category.isNotEmpty(), requiredCategoryError, parentContext)

            if (element.value?.type == DynamicValueType.QUANTITY) {
                val quantity = element.value!!.value as Quantity

                // The presence of a code requires a system, so we're bypassing the check here.
                ifNotNull(quantity.system) {
                    checkTrue(quantity.system == CodeSystem.UCUM.uri, invalidQuantitySystemError, parentContext)
                }
            }

            if (element.component.isEmpty() && element.hasMember.isEmpty()) {
                checkTrue(
                    (element.value != null || element.dataAbsentReason != null),
                    noChildValueOrDataAbsentReasonError,
                    parentContext
                )
            }

            element.effective?.let {
                if (it.type == DynamicValueType.DATE_TIME) {
                    val dateTime = it.value as? String
                    checkTrue(dateTime.hasDayFormat(), invalidDateTimeError, parentContext)
                }
            }
        }

        // category, code, dataAbsentReason, effective (basics) - and status - validated by R4ObservationValidator
    }

    override fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateObservation(element, parentContext, validation)
        validation.apply {
            checkTrue(
                element.category.flatMap { it.coding }
                    .any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == laboratoryCode },
                requiredLaboratoryCodeError,
                parentContext
            )
        }
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
