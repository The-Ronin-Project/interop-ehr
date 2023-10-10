package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.qualifiesForValueSet
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDateTime

/**
 * Base class capable of handling common tasks associated to Ronin Condition profiles.
 */
abstract class BaseRoninCondition(
    extendedProfile: ProfileValidator<Condition>,
    profile: String,
    normalizer: Normalizer,
    localizer: Localizer,
    protected val registryClient: NormalizationRegistryClient,
    private val tenantsNotConditionMapped: String
) : USCoreBasedProfile<Condition>(extendedProfile, profile, normalizer, localizer) {

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    open fun qualifyingCategories(): List<Coding> = emptyList()

    override fun qualifies(resource: Condition): Boolean {
        return resource.category.qualifiesForValueSet(qualifyingCategories())
    }

    private val requiredCodeError = RequiredFieldError(Condition::code)

    private val requiredConditionCodeExtension = FHIRError(
        code = "RONIN_CND_001",
        description = "Tenant source condition code extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Condition::extension)
    )

    override fun validateRonin(element: Condition, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, validation)
            containedResourcePresent(element.contained, parentContext, validation)

            // check that subject reference has type and the extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(Condition::subject),
                    validation
                )
            }

            checkTrue(
                element.category.qualifiesForValueSet(qualifyingCategories()),
                FHIRError(
                    code = "RONIN_CND_001",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Must match this system|code: ${
                    qualifyingCategories().joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                    }",
                    location = LocationContext(Condition::category)
                ),
                parentContext
            )

            checkNotNull(element.code, requiredCodeError, parentContext)
            requireCodeableConcept("code", element.code, parentContext, validation)
            // code value set is not validated, as it is too large

            // clinicalStatus required value set is validated in R4
            // verificationStatus required value set is validated in R4

            checkTrue(
                element.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredConditionCodeExtension,
                parentContext
            )
        }
    }

    override fun validateUSCore(element: Condition, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            validateReference(
                element.subject,
                listOf(ResourceType.Patient),
                LocationContext(Condition::subject),
                validation
            )
        }
    }

    override fun conceptMap(
        normalized: Condition,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Condition, Validation> {
        val validation = Validation()
        if (tenant.mnemonic in tenantsNotConditionMapped) {
            val tenantSourceConditionCode = getExtensionOrEmptyList(
                RoninExtension.TENANT_SOURCE_CONDITION_CODE,
                normalized.code
            )

            val mapped = normalized.copy(
                extension = normalized.extension + tenantSourceConditionCode
            )
            return Pair(mapped, validation)
        }
        // Condition.code is a single CodeableConcept
        val mappedCode = normalized.code?.let { code ->
            val conditionCode = registryClient.getConceptMapping(
                tenant,
                "Condition.code",
                code,
                normalized,
                forceCacheReloadTS
            )
            // validate the mapping we got, use code value to report issues
            validation.apply {
                checkNotNull(
                    conditionCode,
                    FailedConceptMapLookupError(
                        LocationContext(Condition::code),
                        code.coding.mapNotNull { it.code?.value }
                            .joinToString(", "),
                        "any Condition.code concept map for tenant '${tenant.mnemonic}'",
                        conditionCode?.metadata
                    ),
                    parentContext
                )
            }
            conditionCode
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

    private val requiredIdError = RequiredFieldError(Condition::id)

    override fun transformInternal(
        normalized: Condition,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<Condition>?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(TransformResponse(transformed), validation)
    }
}
