package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.resource.base.BaseProfile
import com.projectronin.interop.fhir.ronin.resource.base.MultipleProfileResource
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyHeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyWeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninLaboratoryResult
import com.projectronin.interop.fhir.ronin.resource.observation.RoninObservation

/**
 * Validator and Transformer for the group of active Ronin Observation profiles.
 */
object RoninObservations : MultipleProfileResource<Observation>() {
    override val potentialProfiles: List<BaseProfile<Observation>>
        get() = listOf(RoninLaboratoryResult, RoninBodyHeight, RoninBodyWeight)
    override val defaultProfile: BaseProfile<Observation>
        get() = RoninObservation
}
