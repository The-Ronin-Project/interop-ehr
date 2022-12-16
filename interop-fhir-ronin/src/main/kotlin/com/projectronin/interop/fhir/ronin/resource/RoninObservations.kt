package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.resource.base.BaseProfile
import com.projectronin.interop.fhir.ronin.resource.base.MultipleProfileResource
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBloodPressure
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyHeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyTemperature
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyWeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninHeartRate
import com.projectronin.interop.fhir.ronin.resource.observation.RoninLaboratoryResult
import com.projectronin.interop.fhir.ronin.resource.observation.RoninObservation
import com.projectronin.interop.fhir.ronin.resource.observation.RoninPulseOximetry
import com.projectronin.interop.fhir.ronin.resource.observation.RoninRespiratoryRate

/**
 * Validator and Transformer for the group of active Ronin Observation profiles.
 */
object RoninObservations : MultipleProfileResource<Observation>() {
    override val potentialProfiles: List<BaseProfile<Observation>>
        get() = listOf(
            RoninLaboratoryResult,
            RoninBodyHeight,
            RoninBodyWeight,
            RoninBodyTemperature,
            RoninBloodPressure,
            RoninRespiratoryRate,
            RoninHeartRate,
            RoninPulseOximetry,
        )
    override val defaultProfile: BaseProfile<Observation>
        get() = RoninObservation
}
