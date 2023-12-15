package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.validate.resource.R4CarePlanValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and Transformer for the Ronin Care Plan profile.
 */
@Component
class RoninCarePlan(
    protected val registryClient: NormalizationRegistryClient,
    normalizer: Normalizer,
    localizer: Localizer,
) :
    BaseRoninProfile<CarePlan>(
            R4CarePlanValidator,
            RoninProfile.CARE_PLAN.value,
            normalizer,
            localizer,
        ) {
    override val rcdmVersion = RCDMVersion.V3_27_0
    override val profileVersion = 7

    private val epicCycleExtension = "http://open.epic.com/FHIR/StructureDefinition/extension/cycle"
    private val categoryListError =
        FHIRError(
            code = "RONIN_CAREPLAN_001",
            description = "CarePlan category list size must match the tenantSourceCarePlanCategory extension list size",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext("", ""),
        )

    override fun validateRonin(
        element: CarePlan,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, validation)
            containedResourcePresent(element.contained, parentContext, validation)

            // check if categories exist
            if (element.category.isNotEmpty()) {
                // check that careplan category list size and the tenantSourceCarePlanCategory extension list size are equal
                val categoryExtensionList =
                    element.extension.filter { it.url == RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri }
                checkTrue(
                    categoryExtensionList.size == element.category.size,
                    categoryListError,
                    parentContext,
                )
            }

            // check that subject reference extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(element.subject, LocationContext(CarePlan::subject), validation)
            }

            // status required, and the required value set, is validated in R4
            // intent required, and the required value set, is validated in R4
        }
        // status, intent, and subject required values and value sets inherit validation from R4
    }

    override fun conceptMap(
        normalized: CarePlan,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): Pair<CarePlan, Validation> {
        val validation = Validation()
        // mapOf -> keys are mapped categories, values are the tenant-source-extensions of the mapped categories
        val mappedCategoryAndExtension = mutableMapOf<CodeableConcept, Extension>()

        // CarePlan.category is a list of CodeableConcepts, go through each one and validate it, then add to map
        normalized.category.forEach { category ->
            val categoryConcept =
                registryClient.getConceptMapping(
                    tenant,
                    "CarePlan.category",
                    category,
                    normalized,
                    forceCacheReloadTS,
                )
            validation.apply {
                checkNotNull(
                    categoryConcept,
                    FailedConceptMapLookupError(
                        LocationContext(CarePlan::category),
                        category.coding.mapNotNull { it.code?.value }
                            .joinToString(", "),
                        "any CarePlan.category concept map for tenant '${tenant.mnemonic}'",
                        categoryConcept?.metadata,
                    ),
                    parentContext,
                )
            }
            if (categoryConcept != null) {
                mappedCategoryAndExtension.put(categoryConcept.codeableConcept, categoryConcept.extension)
            }
        }
        val mappedCarePlan =
            normalized.copy(
                // category = codeable-concepts and extension = normalized + tenant-source-extension-category
                category = mappedCategoryAndExtension.keys.toList(),
                extension = normalized.extension + mappedCategoryAndExtension.values,
            )
        return Pair(mappedCarePlan, validation)
    }

    override fun transformInternal(
        normalized: CarePlan,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): Pair<TransformResponse<CarePlan>?, Validation> {
        // Epic includes a Cycle extension that we need to normalize here.
        val normalizedActivities =
            normalized.activity.map { activity ->
                val cycleExtensions = activity.extension.filter { it.url?.value == epicCycleExtension }
                if (cycleExtensions.isEmpty()) {
                    activity
                } else {
                    activity.copy(
                        extension =
                            activity.extension.map { extension ->
                                if (extension.url?.value == epicCycleExtension && extension.value?.type == DynamicValueType.REFERENCE) {
                                    val reference = extension.value?.value as Reference
                                    val normalizedReference = normalizer.normalize(reference, tenant)
                                    extension.copy(
                                        value = DynamicValue(DynamicValueType.REFERENCE, normalizedReference),
                                    )
                                } else {
                                    extension
                                }
                            },
                    )
                }
            }

        val transformed =
            normalized.copy(
                meta = normalized.meta.transform(),
                identifier = normalized.getRoninIdentifiersForResource(tenant),
                extension = normalized.extension,
                activity = normalizedActivities,
            )

        return Pair(TransformResponse(transformed), Validation())
    }
}
