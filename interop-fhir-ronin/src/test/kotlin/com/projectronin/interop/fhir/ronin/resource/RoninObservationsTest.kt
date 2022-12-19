package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBloodPressure
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyHeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyTemperature
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyWeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninHeartRate
import com.projectronin.interop.fhir.ronin.resource.observation.RoninLaboratoryResult
import com.projectronin.interop.fhir.ronin.resource.observation.RoninObservation
import com.projectronin.interop.fhir.ronin.resource.observation.RoninPulseOximetry
import com.projectronin.interop.fhir.ronin.resource.observation.RoninRespiratoryRate
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninObservationsTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val normalizer = mockk<Normalizer>()
    private val localizer = mockk<Localizer>()
    private val bodyHeight = mockk<RoninBodyHeight>()
    private val bodyWeight = mockk<RoninBodyWeight>()
    private val bodyTemperature = mockk<RoninBodyTemperature>()
    private val bloodPressure = mockk<RoninBloodPressure>()
    private val respiratoryRate = mockk<RoninRespiratoryRate>()
    private val heartRate = mockk<RoninHeartRate>()
    private val pulseOximetry = mockk<RoninPulseOximetry>()
    private val laboratoryResult = mockk<RoninLaboratoryResult>()
    private val default = mockk<RoninObservation>() {
        every { qualifies(any()) } returns true
    }
    private val roninObservations =
        RoninObservations(
            normalizer,
            localizer,
            bodyHeight,
            bodyWeight,
            bodyTemperature,
            bloodPressure,
            respiratoryRate,
            heartRate,
            pulseOximetry,
            laboratoryResult,
            default
        )

    // Observation vital signs vs. NOT vital signs - Observation vital signs start here

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninObservations.qualifies(
                Observation(
                    status = ObservationStatus.AMENDED.asCode(),
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                code = RoninBodyHeight.bodyHeightCode
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `can validate against a profile`() {
        val observation = mockk<Observation>()

        every { bodyHeight.qualifies(observation) } returns true
        every { bodyHeight.validate(observation, LocationContext(Observation::class)) } returns Validation()
        every { bodyWeight.qualifies(observation) } returns false
        every { bodyTemperature.qualifies(observation) } returns false
        every { bloodPressure.qualifies(observation) } returns false
        every { respiratoryRate.qualifies(observation) } returns false
        every { heartRate.qualifies(observation) } returns false
        every { pulseOximetry.qualifies(observation) } returns false
        every { laboratoryResult.qualifies(observation) } returns false

        roninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can transform to profile`() {
        val original = mockk<Observation> {
            every { id } returns Id("1234")
        }
        every { normalizer.normalize(original, tenant) } returns original

        val roninObservation = mockk<Observation> {
            every { id } returns Id("1234")
        }
        every { localizer.localize(roninObservation, tenant) } returns roninObservation

        every { bodyHeight.qualifies(original) } returns false
        every { bodyWeight.qualifies(original) } returns true
        every { bodyWeight.transformInternal(original, LocationContext(Observation::class), tenant) } returns Pair(
            roninObservation,
            Validation()
        )
        every { bodyTemperature.qualifies(original) } returns false
        every { bloodPressure.qualifies(original) } returns false
        every { respiratoryRate.qualifies(original) } returns false
        every { heartRate.qualifies(original) } returns false
        every { pulseOximetry.qualifies(original) } returns false
        every { laboratoryResult.qualifies(original) } returns false
        every { bodyHeight.qualifies(roninObservation) } returns false
        every { bodyWeight.qualifies(roninObservation) } returns true
        every { bodyWeight.validate(roninObservation, LocationContext(Observation::class)) } returns Validation()
        every { bodyTemperature.qualifies(roninObservation) } returns false
        every { bloodPressure.qualifies(roninObservation) } returns false
        every { respiratoryRate.qualifies(roninObservation) } returns false
        every { heartRate.qualifies(roninObservation) } returns false
        every { pulseOximetry.qualifies(roninObservation) } returns false
        every { laboratoryResult.qualifies(roninObservation) } returns false

        val (transformed, validation) = roninObservations.transform(original, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(roninObservation, transformed)
    }
}
