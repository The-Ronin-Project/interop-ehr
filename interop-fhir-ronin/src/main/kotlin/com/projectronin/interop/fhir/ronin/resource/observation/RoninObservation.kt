package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

/**
 * A broad capture of observations that do not fit in a more granular/specific Observation profile. As additional
 * profiles are created, observations may move from this broad profile to a more specific profile as appropriate.
 * [Ronin Model](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Observation.html)
 */
object RoninObservation : BaseRoninObservation(R4ObservationValidator, RoninProfile.OBSERVATION.value) {

    /**
     * Any Observation resource qualifies for [RoninObservation].
     */
    override fun qualifies(resource: Observation): Boolean {
        return true
    }
}
