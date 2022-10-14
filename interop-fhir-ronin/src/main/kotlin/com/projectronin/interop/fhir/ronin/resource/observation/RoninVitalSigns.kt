package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

object RoninVitalSigns :
    USCoreBasedProfile<Observation>(USCoreVitalSignsValidator, RoninProfile.OBSERVATION_VITAL_SIGNS.value) {
    private val specificVitalSignsCodes = listOf(RoninBodyHeight.bodyHeightCode, RoninBodyWeight.bodyWeightCode)

    override fun qualifies(resource: Observation): Boolean {
        val specificVitalSigns =
            resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && specificVitalSignsCodes.contains(it.code) }
                ?: false
        // If we found a specific vital sign, then we should already be qualifying it against that type, and thus will not fall back to this generic profile.
        if (specificVitalSigns) {
            return false
        }

        return resource.category.any { category ->
            category.coding.any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == USCoreVitalSignsValidator.vitalSignsCode }
        }
    }

    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
        }
    }

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        // This is validated by USCoreVitalSigns
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
            bodySite = original.bodySite?.localize(tenant),
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
