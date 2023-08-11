package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.error.RoninInvalidDynamicValueError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.hasDayFormat
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RoninLaboratoryResult(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninProfileObservation(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_LABORATORY_RESULT.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_24_1
    override val profileVersion = 2

    override fun qualifyingCategories() =
        listOf(Coding(system = CodeSystem.OBSERVATION_CATEGORY.uri, code = Code("laboratory")))

    // Reference checks - override BaseRoninObservation value lists as needed for RoninLaboratoryResult
    override val validDerivedFromValues = listOf(
        "DocumentReference",
        "ImagingStudy",
        "Media",
        "MolecularSequence",
        "Observation",
        "QuestionnaireResponse"
    )
    override val validHasMemberValues = listOf("MolecularSequence", "Observation", "QuestionnaireResponse")
    override val validSubjectValues = listOf("Patient")
    override val validPartOfValues = listOf(
        "ImagingStudy",
        "Immunization",
        "MedicationAdministration",
        "MedicationDispense",
        "MedicationStatement",
        "Procedure"
    )
    override val validPerformerValues =
        listOf("Patient", "Practitioner", "PractitionerRole", "RelatedPerson", "Organization")

    // Dynamic value checks - override BaseRoninObservation for RoninLaboratoryResult
    override val acceptedEffectiveTypes = listOf(
        DynamicValueType.DATE_TIME,
        DynamicValueType.PERIOD
    )

    private val requiredCategoryError = RequiredFieldError(Observation::category)
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

    private val singleObservationCodeError = FHIRError(
        code = "RONIN_LABOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding list must contain exactly 1 entry",
        location = LocationContext(Observation::code)
    )

    private val invalidCodedValueSystemError = FHIRError(
        code = "RONIN_LABOBS_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Value code system must be SNOMED CT",
        location = LocationContext(Observation::value)
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

            if (element.value?.type == DynamicValueType.CODEABLE_CONCEPT) {
                val quantity = element.value!!.value as CodeableConcept

                // The presence of a code requires a system, so we're bypassing the check here.
                ifNotNull(quantity.coding) {
                    checkTrue(
                        quantity.coding.none { it.system?.value != CodeSystem.SNOMED_CT.uri.value },
                        invalidCodedValueSystemError,
                        parentContext
                    )
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
            val codingList = element.code?.coding
            ifNotNull(codingList) {
                checkTrue((codingList!!.size == 1), singleObservationCodeError, parentContext)
            }
            element.effective?.let { data ->
                checkTrue(
                    acceptedEffectiveTypes.contains(data.type),
                    RoninInvalidDynamicValueError(
                        Observation::effective,
                        acceptedEffectiveTypes,
                        "Ronin Laboratory Result"
                    ),
                    parentContext
                )
            }
        }
    }

    /**
     * Validates the Observation against rules from Ronin, USCore, and laboratory Observation type.
     */
    override fun validate(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validate(element, parentContext, validation) // Ronin, USCore
        validateObservation(element, parentContext, validation)
    }

    private val requiredIdError = RequiredFieldError(Observation::id)

    override fun transformInternal(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Observation?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        // TODO: specimen - requires url == http://projectronin.io/fhir/StructureDefinition/ronin-specimen

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(transformed, validation)
    }
}
