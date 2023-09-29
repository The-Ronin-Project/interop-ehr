package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
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
class RoninBodyWeight(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninVitalSign(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_BODY_WEIGHT.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_26_1
    override val profileVersion = 3

    // Quantity unit codes - [USCore Body Weight Units](http://hl7.org/fhir/R4/valueset-ucum-bodyweight.html)
    override val validQuantityCodes = listOf("kg", "[lb_av]", "g")

    private val noBodySiteError = FHIRError(
        code = "RONIN_WTOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "bodySite not allowed for Body Weight observation",
        location = LocationContext(Observation::bodySite)
    )

    override fun validateVitalSign(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            validateVitalSignValue(element.value, parentContext, this)

            checkTrue(element.bodySite == null, noBodySiteError, parentContext)
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
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant),
            bodySite = null // bodySite should not be supplied for Body Weight
        )
        return Pair(transformed, validation)
    }
}
