package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import org.springframework.stereotype.Component

@Component
class RoninBodyWeight(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient,
) :
    BaseRoninVitalSign(
            R4ObservationValidator,
            RoninProfile.OBSERVATION_BODY_WEIGHT.value,
            normalizer,
            localizer,
            registryClient,
        ) {
    override val rcdmVersion = RCDMVersion.V3_26_1
    override val profileVersion = 3

    // Quantity unit codes - [USCore Body Weight Units](http://hl7.org/fhir/R4/valueset-ucum-bodyweight.html)
    override val validQuantityCodes = listOf("kg", "[lb_av]", "g")

    private val noBodySiteError =
        FHIRError(
            code = "RONIN_WTOBS_001",
            severity = ValidationIssueSeverity.ERROR,
            description = "bodySite not allowed for Body Weight observation",
            location = LocationContext(Observation::bodySite),
        )

    override fun validateVitalSign(
        element: Observation,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            validateVitalSignValue(element.value, parentContext, this)

            checkTrue(element.bodySite == null, noBodySiteError, parentContext)
        }
    }

    override fun getTransformedBodySite(bodySite: CodeableConcept?): CodeableConcept? {
        return null
    }
}
