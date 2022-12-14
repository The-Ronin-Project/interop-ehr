package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.InvalidValueSetError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

object RoninBodyWeight : BaseRoninVitalSign(R4ObservationValidator, RoninProfile.OBSERVATION_BODY_WEIGHT.value) {
    internal val bodyWeightCode = Code("29463-7")

    override fun qualifies(resource: Observation): Boolean {
        return resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == bodyWeightCode } ?: false
    }

    private val noBodySiteError = FHIRError(
        code = "RONIN_WTOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "bodySite not allowed for Body Weight observation",
        location = LocationContext(Observation::bodySite)
    )

    override fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(element.bodySite == null, noBodySiteError, parentContext)
        }
    }

    private val noBodyWeightCodeError = FHIRError(
        code = "USCORE_WTOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${bodyWeightCode.value} required for US Core Body Weight profile",
        location = LocationContext(Observation::code)
    )

    private val requiredQuantityValueError = RequiredFieldError(LocationContext("Observation", "valueQuantity.value"))
    private val requiredQuantityUnitError = RequiredFieldError(LocationContext("Observation", "valueQuantity.unit"))
    private val requiredQuantityCodeError = RequiredFieldError(LocationContext("Observation", "valueQuantity.code"))

    private val invalidQuantitySystemError = FHIRError(
        code = "USCORE_WTOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Quantity system must be UCUM",
        location = LocationContext("Observation", "valueQuantity.system")
    )

    /**
     * [USCore Body Weight Units](http://hl7.org/fhir/R4/valueset-ucum-bodyweight.html)
     */
    private val validQuantityCodes = listOf("kg", "[lb_av]", "g")

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)
        validation.apply {
            val code = element.code
            if (code != null) {
                checkTrue(
                    code.coding.any { it.system == CodeSystem.LOINC.uri && it.code == bodyWeightCode },
                    noBodyWeightCodeError,
                    parentContext
                )
            }

            if (element.value?.type == DynamicValueType.QUANTITY) {
                val quantity = element.value!!.value as Quantity
                checkNotNull(quantity.value, requiredQuantityValueError, parentContext)
                checkNotNull(quantity.unit, requiredQuantityUnitError, parentContext)

                // The presence of a code requires a system, so we're bypassing the check here.
                ifNotNull(quantity.system) {
                    checkTrue(quantity.system == CodeSystem.UCUM.uri, invalidQuantitySystemError, parentContext)
                }

                val quantityCode = quantity.code
                checkNotNull(quantityCode, requiredQuantityCodeError, parentContext)
                ifNotNull(quantityCode) {
                    checkTrue(
                        validQuantityCodes.contains(quantityCode.value),
                        InvalidValueSetError(LocationContext("Observation", "valueQuantity.code"), quantityCode.value ?: ""),
                        parentContext
                    )
                }
            }
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

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            bodySite = null // bodySite should not be supplied for Body Weight
        )
        return Pair(transformed, validation)
    }
}
