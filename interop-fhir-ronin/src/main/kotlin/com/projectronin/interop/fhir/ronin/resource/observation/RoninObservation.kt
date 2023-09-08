package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import org.springframework.stereotype.Component

/**
 * A broad capture of observations that do not fit in a more granular/specific Observation profile. As additional
 * profiles are created, observations may move from this broad profile to a more specific profile as appropriate.
 * [Ronin Model](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Observation.html)
 */
@Component
class RoninObservation(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninObservation(
        R4ObservationValidator,
        RoninProfile.OBSERVATION.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_26_1
    override val profileVersion = 4

    // RoninObservation is a catch all, so there are no explicit qualifying codes.
    override fun qualifyingCodes(): ValueSetList = ValueSetList(emptyList(), null)

    /**
     * Any Observation resource qualifies for [RoninObservation].
     */
    override fun qualifies(resource: Observation): Boolean {
        return true
    }

    override fun validateSpecificObservation(
        element: Observation,
        parentContext: LocationContext,
        validation: Validation
    ) {
    }
}
