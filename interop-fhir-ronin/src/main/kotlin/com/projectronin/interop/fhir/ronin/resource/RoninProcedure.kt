package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.fhir.r4.validate.resource.R4ProcedureValidator
import com.projectronin.interop.fhir.r4.valueset.EventStatus
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RoninProcedure(
    normalizer: Normalizer,
    localizer: Localizer,
    protected val registryClient: NormalizationRegistryClient
) :
    USCoreBasedProfile<Procedure>(
        R4ProcedureValidator,
        RoninProfile.PROCEDURE.value,
        normalizer,
        localizer
    ) {
    override val rcdmVersion = RCDMVersion.V3_28_0
    override val profileVersion = 1

    private val requiredExtensionCodeError = FHIRError(
        code = "RONIN_PROC_001",
        description = "Tenant source procedure code extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Procedure::extension)
    )
    private val requiredExtensionCategoryError = FHIRError(
        code = "RONIN_PROC_002",
        description = "Tenant source procedure category extension is invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Procedure::extension)
    )

    override fun validateRonin(element: Procedure, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, this)

            // extension must include procedure code
            checkTrue(
                element.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_PROCEDURE_CODE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredExtensionCodeError,
                parentContext
            )

            // extension may include procedure category
            checkTrue(
                element.extension.filter { it.url == RoninExtension.TENANT_SOURCE_PROCEDURE_CATEGORY.uri }.size <= 1,
                requiredExtensionCategoryError,
                parentContext
            )
        }
    }

    private val requiredPerformedError = FHIRError(
        code = "USCORE_PROC_001",
        description = "Performed SHALL be present if the status is 'completed' or 'in-progress'",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Procedure::performed)
    )

    private val requiredCodeError = FHIRError(
        code = "USCORE_PROC_002",
        description = "Procedure code is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Procedure::code)
    )

    override fun validateUSCore(element: Procedure, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            if (element.status?.value == EventStatus.COMPLETED.code || element.status?.value == EventStatus.IN_PROGRESS.code) {
                checkNotNull(element.performed, requiredPerformedError, parentContext)
            }
            checkNotNull(element.code, requiredCodeError, parentContext)
            validateReference(element.subject, listOf(ResourceType.Patient), parentContext, validation)
        }
    }

    override fun conceptMap(
        normalized: Procedure,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Procedure, Validation> {
        val validation = Validation()

        // Procedure.code is a single CodeableConcept
        val mappedCodePair = normalized.code?.let { code ->
            val codePair = registryClient.getConceptMapping(
                tenant,
                "Procedure.code",
                code,
                normalized,
                forceCacheReloadTS
            )
            // validate the mapping we got, use code value to report issues
            validation.apply {
                checkNotNull(
                    codePair,
                    FailedConceptMapLookupError(
                        LocationContext(Procedure::code),
                        code.coding.mapNotNull { it.code?.value }
                            .joinToString(", "),
                        "any Procedure.code concept map for tenant '${tenant.mnemonic}'",
                        codePair?.metadata
                    ),
                    parentContext
                )
            }
            codePair
        }

        // Procedure.category is a single CodeableConcept
        val mappedCategoryPair = normalized.category?.let { category ->
            val categoryPair = registryClient.getConceptMapping(
                tenant,
                "Procedure.category",
                category,
                normalized,
                forceCacheReloadTS
            )
            // validate the mapping we got, use category value to report issues
            validation.apply {
                checkNotNull(
                    categoryPair,
                    FailedConceptMapLookupError(
                        LocationContext(Procedure::category),
                        category.coding.mapNotNull { it.code?.value }
                            .joinToString(", "),
                        "any Procedure.category concept map for tenant '${tenant.mnemonic}'",
                        categoryPair?.metadata
                    ),
                    parentContext
                )
            }
            categoryPair
        }

        val (finalCode, codeExtensions) = mappedCodePair?.let { Pair(it.codeableConcept, listOf(it.extension)) } ?: Pair(normalized.code, emptyList())
        val (finalCategory, categoryExtensions) = mappedCategoryPair?.let { Pair(it.codeableConcept, listOf(it.extension)) } ?: Pair(normalized.category, emptyList())

        val mappedProcedure = normalized.copy(
            code = finalCode,
            category = finalCategory,
            extension = normalized.extension + codeExtensions + categoryExtensions
        )

        return Pair(
            mappedProcedure,
            validation
        )
    }

    override fun transformInternal(
        normalized: Procedure,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<Procedure>?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(TransformResponse(transformed), Validation())
    }
}
