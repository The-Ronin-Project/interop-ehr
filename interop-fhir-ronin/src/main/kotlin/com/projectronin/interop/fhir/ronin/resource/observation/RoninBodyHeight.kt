package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.InvalidValueSetError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

const val RONIN_BODY_HEIGHT_PROFILE =
    "http://projectronin.io/fhir/ronin.common-fhir-model.uscore-r4/StructureDefinition/ronin-bodyHeight"

object RoninBodyHeight :
    USCoreBasedProfile<Observation>(USCoreVitalSignsValidator, RONIN_BODY_HEIGHT_PROFILE) {
    internal val bodyHeightCode = Code("8302-2")

    override fun qualifies(resource: Observation): Boolean {
        return resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == bodyHeightCode } ?: false
    }

    private val noBodySiteError = FHIRError(
        code = "RONIN_HTOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "bodySite not allowed for Body Height observation",
        location = LocationContext(Observation::bodySite)
    )

    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)

            checkTrue(element.bodySite == null, noBodySiteError, parentContext)
        }
    }

    private val noBodyHeightCodeError = FHIRError(
        code = "USCORE_HTOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${bodyHeightCode.value} required for US Core Body Height profile",
        location = LocationContext(Observation::code)
    )

    private val requiredQuantityValueError = RequiredFieldError(LocationContext("Observation", "valueQuantity.value"))
    private val requiredQuantityUnitError = RequiredFieldError(LocationContext("Observation", "valueQuantity.unit"))
    private val requiredQuantityCodeError = RequiredFieldError(LocationContext("Observation", "valueQuantity.code"))

    private val invalidQuantitySystemError = FHIRError(
        code = "USCORE_HTOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Quantity system must be UCUM",
        location = LocationContext("Observation", "valueQuantity.system")
    )

    private val validQuantityCodes = listOf("cm", "[in_i]")
    private val invalidQuantityCodeError = InvalidValueSetError(LocationContext("Observation", "valueQuantity.code"))

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            val code = element.code
            if (code != null) {
                checkTrue(
                    code.coding.any { it.system == CodeSystem.LOINC.uri && it.code == bodyHeightCode },
                    noBodyHeightCodeError,
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
                    checkTrue(validQuantityCodes.contains(quantityCode.value), invalidQuantityCodeError, parentContext)
                }
            }
        }
    }

    private val requiredIdError = RequiredFieldError(Observation::id)

    override fun transformInternal(
        original: Observation,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Observation?, Validation> {
        val validation = validation {
            checkNotNull(original.id, requiredIdError, parentContext)
        }

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta.transform(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + original.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            basedOn = original.basedOn.map { it.localize(tenant) },
            partOf = original.partOf.map { it.localize(tenant) },
            category = original.category.map { it.localize(tenant) },
            code = original.code?.localize(tenant),
            subject = original.subject?.localize(tenant),
            focus = original.focus.map { it.localize(tenant) },
            encounter = original.encounter?.localize(tenant),
            performer = original.performer.map { it.localize(tenant) },
            dataAbsentReason = original.dataAbsentReason?.localize(tenant),
            interpretation = original.interpretation.map { it.localize(tenant) },
            bodySite = null, // bodySite should not be supplied for Body Height
            method = original.method?.localize(tenant),
            specimen = original.specimen?.localize(tenant),
            device = original.device?.localize(tenant),
            referenceRange = original.referenceRange.map { it.localize(tenant) },
            hasMember = original.hasMember.map { it.localize(tenant) },
            derivedFrom = original.derivedFrom.map { it.localize(tenant) },
            component = original.component.map { it.localize(tenant) },
            note = original.note.map { it.localize(tenant) }
        )
        return Pair(transformed, validation)
    }
}
