package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
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
import org.springframework.stereotype.Component

/**
 * Validator and Transformer for the group of active Ronin Observation profiles.
 */
@Component
class RoninObservations(
    normalizer: Normalizer,
    localizer: Localizer,
    roninBodyHeight: RoninBodyHeight,
    roninBodyWeight: RoninBodyWeight,
    roninBodyTemperature: RoninBodyTemperature,
    roninBloodPressure: RoninBloodPressure,
    roninRespiratoryRate: RoninRespiratoryRate,
    roninHeartRate: RoninHeartRate,
    roninPulseOximetry: RoninPulseOximetry,
    roninLaboratoryResult: RoninLaboratoryResult,
    roninObservation: RoninObservation
) : MultipleProfileResource<Observation>(normalizer, localizer) {
    override val potentialProfiles: List<BaseProfile<Observation>> =
        listOf(
            roninLaboratoryResult,
            roninBodyHeight,
            roninBodyWeight,
            roninBodyTemperature,
            roninBloodPressure,
            roninRespiratoryRate,
            roninHeartRate,
            roninPulseOximetry
        )
    override val defaultProfile: BaseProfile<Observation>? = roninObservation
}
