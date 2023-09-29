package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCodeableConcept
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.qualifiesForValueSet
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDateTime

/**
 * Base class capable of handling common tasks associated to Ronin Observation profiles.
 */
abstract class BaseRoninObservation(
    extendedProfile: ProfileValidator<Observation>,
    profile: String,
    normalizer: Normalizer,
    localizer: Localizer,
    protected val registryClient: NormalizationRegistryClient
) : USCoreBasedProfile<Observation>(extendedProfile, profile, normalizer, localizer) {

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    open fun qualifyingCategories(): List<Coding> = emptyList()

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    open fun qualifyingCodes(): ValueSetList = registryClient.getRequiredValueSet(
        "Observation.code",
        profile
    )

    override fun qualifies(resource: Observation): Boolean {
        return (
            resource.category.qualifiesForValueSet(qualifyingCategories()) &&
                resource.code.qualifiesForValueSet(qualifyingCodes().codes)
            )
    }

    // Reference checks - subclasses may override lists to modify validation logic for reference attributes
    open val validSubjectValues = listOf(ResourceType.Patient)

    // Dynamic value checks - subclasses may override lists
    open val acceptedEffectiveTypes = listOf(
        DynamicValueType.DATE_TIME,
        DynamicValueType.PERIOD,
        DynamicValueType.TIMING,
        DynamicValueType.INSTANT
    )

    // Dynamic value checks - same for all subclasses
    private val acceptedAuthorTypes = listOf(
        DynamicValueType.STRING,
        DynamicValueType.REFERENCE
    )

    private val requiredSubjectError = RequiredFieldError(Observation::subject)

    private val requiredCodeError = RequiredFieldError(Observation::code)

    private val singleObservationCodeError = FHIRError(
        code = "RONIN_OBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding list is restricted to 1 entry",
        location = LocationContext(Observation::code)
    )

    private val requiredExtensionCodeError = FHIRError(
        code = "RONIN_OBS_004",
        description = "Tenant source observation code extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Observation::extension)
    )

    private val requiredExtensionValueError = FHIRError(
        code = "RONIN_OBS_005",
        description = "Tenant source observation value extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Observation::extension)
    )

    private val requiredComponentExtensionCodeError = FHIRError(
        code = "RONIN_OBS_006",
        description = "Tenant source observation component code extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(ObservationComponent::extension)
    )

    private val requiredComponentExtensionValueError = FHIRError(
        code = "RONIN_OBS_007",
        description = "Tenant source observation component value extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(ObservationComponent::extension)
    )

    /**
     * Validates the [element] against RoninObservation rules. Validation logic for reference attributes may vary by
     * Observation type. This logic is controlled by overriding the open val variables like [validSubjectValues].
     */
    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, validation)

            containedResourcePresent(element.contained, parentContext, validation)

            requireCodeableConcept("code", element.code, parentContext, validation)
            requireCodeCoding("code", element.code?.coding, parentContext, validation)

            checkNotNull(element.subject, requiredSubjectError, parentContext)
            // check that subject reference has type and the extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(Observation::subject),
                    validation
                )
            }

            val qualifyingCategories = qualifyingCategories()
            if (qualifyingCategories.isNotEmpty()) {
                checkTrue(
                    element.category.qualifiesForValueSet(qualifyingCategories),
                    FHIRError(
                        code = "RONIN_OBS_002",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${qualifyingCategories().joinToString(", ") { "${it.system?.value}|${it.code?.value}" }}",
                        location = LocationContext(Observation::category)
                    ),
                    parentContext
                )
            }

            ifNotNull(element.code) {
                val qualifyingCodes = qualifyingCodes().codes
                if (qualifyingCodes.isNotEmpty()) {
                    checkTrue(
                        element.code.qualifiesForValueSet(qualifyingCodes),
                        FHIRError(
                            code = "RONIN_OBS_003",
                            severity = ValidationIssueSeverity.ERROR,
                            description = "Must match this system|code: ${qualifyingCodes().codes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }}",
                            location = LocationContext(Observation::code)
                        ),
                        parentContext
                    )
                }
            }

            checkTrue(
                element.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredExtensionCodeError,
                parentContext
            )
            if (element.value?.type == DynamicValueType.CODEABLE_CONCEPT) {
                checkTrue(
                    element.extension.any {
                        it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri &&
                            it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                    },
                    requiredExtensionValueError,
                    parentContext
                )
            }

            element.component.forEachIndexed { index, observationComponent ->
                val componentContext = parentContext.append(LocationContext("", "component[$index]"))
                checkTrue(
                    observationComponent.extension.any {
                        it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri &&
                            it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                    },
                    requiredComponentExtensionCodeError,
                    componentContext
                )
                if (observationComponent.value?.type == DynamicValueType.CODEABLE_CONCEPT) {
                    checkTrue(
                        observationComponent.extension.any {
                            it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.uri &&
                                it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                        },
                        requiredComponentExtensionValueError,
                        componentContext
                    )
                }
            }

            validateSpecificObservation(element, parentContext, validation)

            // category and code (basics), dataAbsentReason, effective, status - validated by R4ObservationValidator
        }
    }

    /**
     * Validates the [element] against USCore Observation rules.
     */
    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            validateReference(element.subject, validSubjectValues, LocationContext(Observation::subject), validation)
        }
    }

    /**
     * Validates a specific Observation against the profile. By default, this will check for a valid category and code, and the appropriate source code extension.
     */
    abstract fun validateSpecificObservation(
        element: Observation,
        parentContext: LocationContext,
        validation: Validation
    )

    override fun conceptMap(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Observation, Validation> {
        val validation = Validation()

        val newExtensions = mutableListOf<Extension>()

        val mapCodeResponse =
            mapCode(normalized.code, "Observation.code", parentContext, tenant, validation, forceCacheReloadTS)
        val mappedCode = if (mapCodeResponse == null) {
            normalized.code
        } else {
            newExtensions.add(mapCodeResponse.extension)
            mapCodeResponse.codeableConcept
        }

        val mapValueResponse =
            mapValue(normalized.value, "Observation.value", parentContext, tenant, validation, forceCacheReloadTS)
        val mappedValue = if (mapValueResponse == null) {
            normalized.value
        } else {
            newExtensions.add(mapValueResponse.extension)
            DynamicValue(DynamicValueType.CODEABLE_CONCEPT, mapValueResponse.codeableConcept)
        }

        // Now we need to do similar to above to each component.
        val mappedComponents = normalized.component.mapIndexed { index, normalizedComponent ->
            val componentContext = parentContext.append(LocationContext("", "component[$index]"))

            val newComponentExtensions = mutableListOf<Extension>()

            val mapComponentCodeResponse = mapCode(
                normalizedComponent.code,
                "Observation.component.code",
                componentContext,
                tenant,
                validation,
                forceCacheReloadTS
            )
            val mappedComponentCode = if (mapComponentCodeResponse == null) {
                normalizedComponent.code
            } else {
                newComponentExtensions.add(mapComponentCodeResponse.extension)
                mapComponentCodeResponse.codeableConcept
            }

            val mapComponentValueResponse =
                mapValue(
                    normalizedComponent.value,
                    "Observation.component.value",
                    componentContext,
                    tenant,
                    validation,
                    forceCacheReloadTS
                )
            val mappedComponentValue = if (mapComponentValueResponse == null) {
                normalizedComponent.value
            } else {
                newComponentExtensions.add(mapComponentValueResponse.extension)
                DynamicValue(DynamicValueType.CODEABLE_CONCEPT, mapComponentValueResponse.codeableConcept)
            }

            normalizedComponent.copy(
                code = mappedComponentCode,
                value = mappedComponentValue,
                extension = normalizedComponent.extension + newComponentExtensions
            )
        }

        val mappedObservation = normalized.copy(
            code = mappedCode,
            value = mappedValue,
            component = mappedComponents,
            extension = normalized.extension + newExtensions
        )
        return Pair(
            mappedObservation,
            validation
        )
    }

    private fun mapCode(
        normalizedCodeableConcept: CodeableConcept?,
        elementName: String,
        parentContext: LocationContext,
        tenant: Tenant,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): ConceptMapCodeableConcept? {
        return normalizedCodeableConcept?.let { code ->
            val observationCode = registryClient.getConceptMapping(
                tenant,
                elementName,
                code,
                forceCacheReloadTS
            )
            // validate the mapping we got, use code value to report issues
            validation.apply {
                checkNotNull(
                    observationCode,
                    FailedConceptMapLookupError(
                        LocationContext("", "code"),
                        code.coding.mapNotNull { it.code?.value }
                            .joinToString(", "),
                        "any $elementName concept map for tenant '${tenant.mnemonic}'",
                        observationCode?.metadata
                    ),
                    parentContext
                )
            }
            observationCode
        }
    }

    private fun mapValue(
        normalizedValue: DynamicValue<Any>?,
        elementName: String,
        parentContext: LocationContext,
        tenant: Tenant,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): ConceptMapCodeableConcept? {
        return normalizedValue?.let { dyanmicValue ->
            if (dyanmicValue.type == DynamicValueType.CODEABLE_CONCEPT) {
                val valueCodeableConcept = dyanmicValue.value as CodeableConcept

                val observationValue = registryClient.getConceptMapping(
                    tenant,
                    elementName,
                    valueCodeableConcept,
                    forceCacheReloadTS
                )
                // validate the mapping we got, use code value to report issues
                validation.apply {
                    checkNotNull(
                        observationValue,
                        FailedConceptMapLookupError(
                            LocationContext("", "value"),
                            valueCodeableConcept.coding.mapNotNull { it.code?.value }
                                .joinToString(", "),
                            "any $elementName concept map for tenant '${tenant.mnemonic}'",
                            observationValue?.metadata
                        ),
                        parentContext
                    )
                }
                observationValue
            } else {
                null
            }
        }
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

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(transformed, validation)
    }
}
