package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
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
    BaseRoninObservation(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_STAGING_RELATED.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_24_1
    override val profileVersion = 2

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

    private val requiredCompValueExtensionError = FHIRError(
        code = "RONIN_STAGING_OBS_003",
        description = "Tenant source observation component value extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Observation::component)
    )

    // category and category.coding must be present but are not fixed values
    // code.coding must exist in the valueSet
    override fun qualifies(resource: Observation): Boolean {
        return (
            (resource.code?.qualifiesForValueSet(qualifyingCodes().codes) == true) &&
                resource.category.isNotEmpty() && resource.category.any { category -> category.coding.isNotEmpty() }
            )
    }

    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateRonin(element, parentContext, validation)
        validation.apply {
            checkTrue(
                element.category.any { category -> category.coding.isNotEmpty() },
                FHIRError(
                    code = "RONIN_STAGING_OBS_002",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Coding is required",
                    location = LocationContext(Observation::category),
                    metadata = listOf(qualifyingCodes().metadata!!)
                ),
                parentContext
            )
        }
        // category non-null - validated by R4ObservationValidator
    }

    override fun validateSpecificObservation(
        element: Observation,
        parentContext: LocationContext,
        validation: Validation
    ) {
        super.validateSpecificObservation(element, parentContext, validation)

        validation.apply {
            element.code?.coding?.let {
                checkTrue(
                    it.size == 1,
                    FHIRError(
                        code = "RONIN_STAGING_OBS_001",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Coding list must contain exactly 1 entry",
                        location = LocationContext(Observation::code),
                        metadata = listOf(qualifyingCodes().metadata!!)
                    ),
                    parentContext
                )
            }

            element.component.forEach { component ->
                checkTrue(
                    (
                        component.value?.type == DynamicValueType.CODEABLE_CONCEPT &&
                            component.extension.any { it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.uri }
                        ) ||
                        (
                            component.value?.type != DynamicValueType.CODEABLE_CONCEPT &&
                                component.extension.isEmpty()
                            ),
                    requiredCompValueExtensionError,
                    parentContext
                )
            }
        }
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

    override fun conceptMap(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Observation, Validation> {
        // normalize Observation.code
        val (codeNormalized, codeValidation) = super.conceptMap(
            normalized,
            parentContext,
            tenant,
            forceCacheReloadTS
        )

        // normalize Observation.Component.value (DateTime or CodeableConcept)
        val validation = Validation()
        validation.merge(codeValidation)
        val componentNormalized = codeNormalized.component.mapNotNull { component ->
            component.value?.let { value ->
                when (value.type) {
                    DynamicValueType.CODEABLE_CONCEPT -> {
                        val concept = value.value as CodeableConcept
                        val mappedValue = registryClient.getConceptMapping(
                            tenant,
                            "Observation.Component.value",
                            concept,
                            forceCacheReloadTS
                        )
                        // validate mapping, use source value to report issues
                        validation.apply {
                            checkNotNull(
                                mappedValue,
                                FailedConceptMapLookupError(
                                    LocationContext(ObservationComponent::value),
                                    concept.coding.mapNotNull { it.code?.value }
                                        .joinToString(", "),
                                    "any Observation.Component.value concept map for tenant '${tenant.mnemonic}'",
                                    mappedValue?.metadata
                                ),
                                LocationContext(Observation::component)
                            )
                        }
                        mappedValue?.let {
                            component.copy(
                                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, mappedValue.codeableConcept),
                                extension = component.extension + mappedValue.extension
                            )
                        } ?: component
                    }

                    else -> component
                }
            }
        }

        return Pair(
            when {
                componentNormalized.isEmpty() -> codeNormalized
                else -> codeNormalized.copy(component = componentNormalized)
            },
            validation
        )
    }
}
