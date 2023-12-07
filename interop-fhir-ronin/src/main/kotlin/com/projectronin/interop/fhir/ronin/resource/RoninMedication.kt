package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and transformer for the Ronin Medication profile
 */
@Component
class RoninMedication(
    normalizer: Normalizer,
    localizer: Localizer,
) :
    USCoreBasedProfile<Medication>(
            R4MedicationValidator,
            RoninProfile.MEDICATION.value,
            normalizer,
            localizer,
        ) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val requiredCodeError = RequiredFieldError(Medication::code)
    private val requiredExtensionCodeError =
        FHIRError(
            code = "RONIN_MED_001",
            description = "Tenant source medication code extension is missing or invalid",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(Medication::extension),
        )

    override fun validateRonin(
        element: Medication,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)

            containedResourcePresent(element.contained, parentContext, validation)

            requireCodeableConcept("code", element.code, parentContext, this)
            checkTrue(
                element.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_MEDICATION_CODE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredExtensionCodeError,
                parentContext,
            )
            // code will be populated by from mapping
        }
    }

    override fun validateUSCore(
        element: Medication,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            checkNotNull(element.code, requiredCodeError, parentContext)

            // status value set is checked by R4MedicationValidator

            // ingredient.item is checked by R4MedicationValidator
        }
    }

    override fun conceptMap(
        normalized: Medication,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): Pair<Medication, Validation> {
        // TODO: apply concept maps to get Medication.code and extension
        val tenantSourceCodeExtension =
            getExtensionOrEmptyList(
                RoninExtension.TENANT_SOURCE_MEDICATION_CODE,
                normalized.code,
            )

        val mapped =
            normalized.copy(
                extension = normalized.extension + tenantSourceCodeExtension,
            )
        return Pair(mapped, Validation())
    }

    override fun transformInternal(
        normalized: Medication,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): Pair<TransformResponse<Medication>?, Validation> {
        val transformed =
            normalized.copy(
                meta = normalized.meta.transform(),
                identifier = normalized.getRoninIdentifiersForResource(tenant),
            )
        return Pair(TransformResponse(transformed), Validation())
    }
}
