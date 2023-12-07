package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.base.BaseProfile
import com.projectronin.interop.fhir.ronin.resource.base.MultipleProfileResource
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBloodPressure
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyHeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyMassIndex
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodySurfaceArea
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyTemperature
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyWeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninHeartRate
import com.projectronin.interop.fhir.ronin.resource.observation.RoninLaboratoryResult
import com.projectronin.interop.fhir.ronin.resource.observation.RoninObservation
import com.projectronin.interop.fhir.ronin.resource.observation.RoninPulseOximetry
import com.projectronin.interop.fhir.ronin.resource.observation.RoninRespiratoryRate
import com.projectronin.interop.fhir.ronin.resource.observation.RoninStagingRelated
import org.springframework.stereotype.Component

/**
 * Validator and Transformer for the group of active Ronin Observation profiles.
 * Any Observation that has a laboratory category code qualifies for the RoninLaboratoryResult class.
 * There is no generic vital-sign Observation class. Every vital-sign Observation class is specific.
 * If an Observation does not qualify as a known vital-sign or laboratory, it qualifies as the generic RoninObservation.
 */
@Component
class RoninObservations(
    normalizer: Normalizer,
    localizer: Localizer,
    roninBodyHeight: RoninBodyHeight,
    roninBodyMassIndex: RoninBodyMassIndex,
    roninBodySurfaceArea: RoninBodySurfaceArea,
    roninBodyWeight: RoninBodyWeight,
    roninBodyTemperature: RoninBodyTemperature,
    roninBloodPressure: RoninBloodPressure,
    roninRespiratoryRate: RoninRespiratoryRate,
    roninHeartRate: RoninHeartRate,
    roninPulseOximetry: RoninPulseOximetry,
    roninLaboratoryResult: RoninLaboratoryResult,
    roninStagingRelated: RoninStagingRelated,
    roninObservation: RoninObservation,
) : MultipleProfileResource<Observation>(normalizer, localizer) {
    override val potentialProfiles: List<BaseProfile<Observation>> =
        listOf(
            roninLaboratoryResult,
            roninBodyHeight,
            roninBodyMassIndex,
            roninBodySurfaceArea,
            roninBodyWeight,
            roninBodyTemperature,
            roninBloodPressure,
            roninRespiratoryRate,
            roninHeartRate,
            roninPulseOximetry,
            roninStagingRelated,
        )
    override val defaultProfile: BaseProfile<Observation>? = roninObservation
}
