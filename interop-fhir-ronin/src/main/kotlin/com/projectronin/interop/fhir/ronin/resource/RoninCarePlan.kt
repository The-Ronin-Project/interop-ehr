package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.validate.resource.R4CarePlanValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and Transformer for the Ronin Care Plan profile.
 */
@Component
class RoninCarePlan(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninProfile<CarePlan>(R4CarePlanValidator, RoninProfile.CARE_PLAN.value, normalizer, localizer) {
    override val rcdmVersion = RCDMVersion.V3_25_0
    override val profileVersion = 6

    private val epicCycleExtension = "http://open.epic.com/FHIR/StructureDefinition/extension/cycle"

    override fun validateRonin(element: CarePlan, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, validation)
            containedResourcePresent(element.contained, parentContext, validation)

            // check that subject reference has type and the extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(element.subject, LocationContext(CarePlan::subject), validation)
            }

            // status required, and the required value set, is validated in R4
            // intent required, and the required value set, is validated in R4
        }
        // status, intent, and subject required values and value sets inherit validation from R4
    }

    override fun transformInternal(
        normalized: CarePlan,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<CarePlan>?, Validation> {
        val categoryExtensions = normalized.category.map {
            Extension(
                url = RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, it)
            )
        }

        // Epic includes a Cycle extension that we need to normalize here.
        val normalizedActivities = normalized.activity.map { activity ->
            val cycleExtensions = activity.extension.filter { it.url?.value == epicCycleExtension }
            if (cycleExtensions.isEmpty()) {
                activity
            } else {
                activity.copy(
                    extension = activity.extension.map { extension ->
                        if (extension.url?.value == epicCycleExtension && extension.value?.type == DynamicValueType.REFERENCE) {
                            val reference = extension.value?.value as Reference
                            val normalizedReference = normalizer.normalize(reference, tenant)
                            extension.copy(
                                value = DynamicValue(DynamicValueType.REFERENCE, normalizedReference)
                            )
                        } else {
                            extension
                        }
                    }
                )
            }
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant),
            extension = normalized.extension + categoryExtensions,
            activity = normalizedActivities
        )

        return Pair(TransformResponse(transformed), Validation())
    }
}
