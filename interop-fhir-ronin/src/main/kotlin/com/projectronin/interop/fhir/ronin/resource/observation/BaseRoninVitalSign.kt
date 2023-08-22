package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.error.RoninInvalidDynamicValueError
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.InvalidValueSetError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity

/**
 * Base class capable of handling common tasks associated to Vital Sign profiles
 */
abstract class BaseRoninVitalSign(
    extendedProfile: ProfileValidator<Observation>,
    profile: String,
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninObservation(
        extendedProfile,
        profile,
        normalizer,
        localizer,
        registryClient
    ) {

    override fun qualifyingCategories() =
        listOf(Coding(system = CodeSystem.OBSERVATION_CATEGORY.uri, code = Code("vital-signs")))

    // Quantity unit codes - subclasses may override to modify validation logic for quantity units like "cm" "kg"
    open val validQuantityCodes: List<String> = emptyList()

    // Reference checks - override BaseRoninObservation value lists as needed for BaseRoninVitalSign
    override val validSubjectValues = listOf("Patient")

    // Dynamic value checks - override BaseRoninObservation for BaseRoninVitalSign
    override val acceptedEffectiveTypes = listOf(
        DynamicValueType.DATE_TIME,
        DynamicValueType.PERIOD
    )

    private val singleObservationCodeError = FHIRError(
        code = "RONIN_VSOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding list must contain exactly 1 entry",
        location = LocationContext(Observation::code)
    )

    override fun validateSpecificObservation(
        element: Observation,
        parentContext: LocationContext,
        validation: Validation
    ) {
        super.validateSpecificObservation(element, parentContext, validation)

        validation.apply {
            element.code?.coding?.let {
                checkTrue(it.size == 1, singleObservationCodeError, parentContext)
            }
            element.effective?.let { data ->
                checkTrue(
                    acceptedEffectiveTypes.contains(data.type),
                    RoninInvalidDynamicValueError(
                        Observation::effective,
                        acceptedEffectiveTypes,
                        "Ronin Vital Sign"
                    ),
                    parentContext
                )
            }
        }

        validateVitalSign(element, parentContext, validation)
    }

    /**
     * Validates the [element] against Ronin rules for vital sign Observations.
     */
    open fun validateVitalSign(element: Observation, parentContext: LocationContext, validation: Validation) {
        validateVitalSignValue(element.value, validQuantityCodes, parentContext, validation)
    }

    private val requiredQuantityValueError = RequiredFieldError(LocationContext("Observation", "valueQuantity.value"))
    private val requiredQuantityUnitError = RequiredFieldError(LocationContext("Observation", "valueQuantity.unit"))
    private val requiredQuantityCodeError = RequiredFieldError(LocationContext("Observation", "valueQuantity.code"))

    private val invalidQuantitySystemError = FHIRError(
        code = "USCORE_VSOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Quantity system must be UCUM",
        location = LocationContext("Observation", "valueQuantity.system")
    )

    /**
     * Validates the dynamic [value] against Ronin rules for vital sign quantities.
     * Checks that the value includes a value and unit, and that the value system is UCUM.
     * Checks that the quantity's coded unit value is in the supplied [validUnitCodeList].
     */
    open fun validateVitalSignValue(
        value: DynamicValue<Any>?,
        validUnitCodeList: List<String>,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            value?.let {
                if (value.type == DynamicValueType.QUANTITY) {
                    val quantity = value.value as? Quantity
                    checkNotNull(quantity?.value, requiredQuantityValueError, parentContext)
                    checkNotNull(quantity?.unit, requiredQuantityUnitError, parentContext)

                    // The presence of a code requires a system, so we're bypassing the check here.
                    ifNotNull(quantity?.system) {
                        checkTrue(
                            quantity?.system == CodeSystem.UCUM.uri,
                            invalidQuantitySystemError,
                            parentContext
                        )
                    }

                    val quantityCode = quantity?.code
                    checkNotNull(quantityCode, requiredQuantityCodeError, parentContext)
                    ifNotNull(quantityCode) {
                        checkTrue(
                            validUnitCodeList.contains(quantityCode.value),
                            InvalidValueSetError(
                                LocationContext("Observation", "valueQuantity.code"),
                                quantityCode.value ?: ""
                            ),
                            parentContext
                        )
                    }
                }
            }
        }
    }
}
