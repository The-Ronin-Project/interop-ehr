package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
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
class RoninObservation(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninObservation(R4ObservationValidator, RoninProfile.OBSERVATION.value, normalizer, localizer) {
    /**
     * Any Observation resource qualifies for [RoninObservation].
     */
    override fun qualifies(resource: Observation): Boolean {
        return true
    }

    override fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {}
}
