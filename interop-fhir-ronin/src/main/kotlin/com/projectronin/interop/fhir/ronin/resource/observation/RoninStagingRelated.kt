package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.qualifiesForValueSet
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
class RoninStagingRelated(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninProfileObservation(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_STAGING_RELATED.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 1

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    override val qualifyingCodes: List<Coding> by lazy {
        registryClient.getRequiredValueSet(
            "Observation.code",
            profile
        )
    }

    override val validDerivedFromValues = listOf(
        "DocumentReference",
        "ImagingStudy",
        "Media",
        "MolecularSequence",
        "Observation",
        "QuestionnaireResponse"
    )

    override val validPartOfValues = listOf(
        "ImagingStudy",
        "Immunization",
        "MedicationAdministration",
        "MedicationDispense",
        "MedicationStatement",
        "Procedure"
    )

    override val validPerformerValues =
        listOf("Patient", "Practitioner", "PractitionerRole", "RelatedPerson", "Organization", "CareTeam")

    private val requiredIdError = RequiredFieldError(Observation::id)
    private val singleObservationCodeError = FHIRError(
        code = "RONIN_STAGING_OBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding list must contain exactly 1 entry",
        location = LocationContext(Observation::code)
    )

    private val requiredObservationCategoryCoding = FHIRError(
        code = "RONIN_STAGING_OBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding is required",
        location = LocationContext(Observation::category)
    )

    // category and category.coding must be present but are not fixed values
    // code.coding must exist in the valueSet
    override fun qualifies(resource: Observation): Boolean {
        return (
            resource.code.qualifiesForValueSet(qualifyingCodes) &&
                resource.category.isNotEmpty() && resource.category.any { category -> category.coding.isNotEmpty() }
            )
    }

    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateRonin(element, parentContext, validation)
        validation.apply {
            checkTrue(
                element.category.any { category -> category.coding.isNotEmpty() },
                requiredObservationCategoryCoding,
                parentContext
            )
        }
        // category non-null - validated by R4ObservationValidator
    }

    override fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            element.code?.coding?.let {
                checkTrue(it.size == 1, singleObservationCodeError, parentContext)
            }
            checkTrue(
                element.code.qualifiesForValueSet(qualifyingCodes),
                FHIRError(
                    code = "RONIN_OBS_003",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Must match this system|code: ${qualifyingCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }}",
                    location = LocationContext(Observation::code)
                ),
                parentContext
            )
        }
    }

    override fun validate(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validate(element, parentContext, validation) // Ronin, USCore
        validateObservation(element, parentContext, validation)
    }

    override fun transformInternal(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Observation?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(transformed, validation)
    }
}
