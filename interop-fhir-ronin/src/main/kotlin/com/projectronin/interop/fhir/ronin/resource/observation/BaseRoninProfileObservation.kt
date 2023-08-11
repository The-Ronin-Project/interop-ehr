package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDateTime

/**
 * Base class defining common functions for specific Ronin Observation profiles.
 */
abstract class BaseRoninProfileObservation(
    extendedProfile: ProfileValidator<Observation>,
    profile: String,
    normalizer: Normalizer,
    localizer: Localizer,
    protected val registryClient: NormalizationRegistryClient
) :
    BaseRoninObservation(
        extendedProfile,
        profile,
        normalizer,
        localizer
    ) {

    override fun qualifyingCodes(): ValueSetList = registryClient.getRequiredValueSet(
        "Observation.code",
        profile
    )

    private val requiredExtensionCodeError = FHIRError(
        code = "RONIN_OBS_004",
        description = "Tenant source observation code extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Observation::extension)
    )

    override fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateObservation(element, parentContext, validation)

        validation.apply {
            checkTrue(
                element.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredExtensionCodeError,
                parentContext
            )
        }
    }

    override fun mapInternal(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Observation, Validation> {
        val validation = Validation()

        // Observation.code is a single CodeableConcept
        val mappedCode = normalized.code?.let { code ->
            val observationCode = registryClient.getConceptMapping(
                tenant,
                "Observation.code",
                code,
                forceCacheReloadTS
            )
            // validate the mapping we got, use code value to report issues
            validation.apply {
                checkNotNull(
                    observationCode,
                    FailedConceptMapLookupError(
                        LocationContext(Observation::code),
                        code.coding.mapNotNull { it.code?.value }
                            .joinToString(", "),
                        "any Observation.code concept map for tenant '${tenant.mnemonic}'",
                        observationCode?.metadata
                    ),
                    parentContext
                )
            }
            observationCode
        }

        return Pair(
            mappedCode?.let {
                normalized.copy(
                    code = it.codeableConcept,
                    extension = normalized.extension + it.extension
                )
            } ?: normalized,
            validation
        )
    }
}
