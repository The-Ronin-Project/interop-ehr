package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
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
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
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
    private val extension1 = Extension(
        url = Uri("http://example.com/extension"),
        value = DynamicValue(DynamicValueType.STRING, FHIRString("value"))
    )
    private val normalizer = mockk<Normalizer>()
    private val localizer = mockk<Localizer>()
    private val bodyHeight = mockk<RoninBodyHeight>()
    private val bodyHeightCode = Code("8302-2")

    private val bodyMassIndex = mockk<RoninBodyMassIndex>()
    private val bodySurfaceArea = mockk<RoninBodySurfaceArea>()
    private val bodyWeight = mockk<RoninBodyWeight>()
    private val bodyTemperature = mockk<RoninBodyTemperature>()
    private val bloodPressure = mockk<RoninBloodPressure>()
    private val respiratoryRate = mockk<RoninRespiratoryRate>()
    private val heartRate = mockk<RoninHeartRate>()
    private val pulseOximetry = mockk<RoninPulseOximetry>()
    private val laboratoryResult = mockk<RoninLaboratoryResult>()
    private val stagingRelated = mockk<RoninStagingRelated>()
    private val default = mockk<RoninObservation>() {
        every { qualifies(any()) } returns true
    }
    private val roninObservations =
        RoninObservations(
            normalizer,
            localizer,
            bodyHeight,
            bodyMassIndex,
            bodySurfaceArea,
            bodyWeight,
            bodyTemperature,
            bloodPressure,
            respiratoryRate,
            heartRate,
            pulseOximetry,
            laboratoryResult,
            stagingRelated,
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
                                code = bodyHeightCode
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
        every { bodyMassIndex.qualifies(observation) } returns false
        every { bodySurfaceArea.qualifies(observation) } returns false
        every { bodyWeight.qualifies(observation) } returns false
        every { bodyTemperature.qualifies(observation) } returns false
        every { bloodPressure.qualifies(observation) } returns false
        every { respiratoryRate.qualifies(observation) } returns false
        every { heartRate.qualifies(observation) } returns false
        every { pulseOximetry.qualifies(observation) } returns false
        every { laboratoryResult.qualifies(observation) } returns false
        every { stagingRelated.qualifies(observation) } returns false

        roninObservations.validate(observation).alertIfErrors()
    }

    @Test
    fun `can transform to profile`() {
        val original = mockk<Observation> {
            every { id } returns Id("1234")
        }
        every { normalizer.normalize(original, tenant) } returns original

        val mappedObservation = mockk<Observation> {
            every { id } returns Id("1234")
            every { extension } returns listOf(extension1)
        }

        val roninObservation = mockk<Observation> {
            every { id } returns Id("test-1234")
            every { extension } returns listOf(extension1)
        }
        every { localizer.localize(roninObservation, tenant) } returns roninObservation

        every { bodyHeight.qualifies(original) } returns false
        every { bodyWeight.qualifies(original) } returns true
        every { bodyWeight.conceptMap(original, LocationContext(Observation::class), tenant) } returns Pair(
            mappedObservation,
            Validation()
        )

        every { bodyMassIndex.qualifies(original) } returns false
        every { bodySurfaceArea.qualifies(original) } returns false
        every { bodyTemperature.qualifies(original) } returns false
        every { bloodPressure.qualifies(original) } returns false
        every { respiratoryRate.qualifies(original) } returns false
        every { heartRate.qualifies(original) } returns false
        every { pulseOximetry.qualifies(original) } returns false
        every { laboratoryResult.qualifies(original) } returns false
        every { stagingRelated.qualifies(original) } returns false

        every { bodyHeight.qualifies(mappedObservation) } returns false
        every { bodyWeight.qualifies(mappedObservation) } returns true
        every {
            bodyWeight.transformInternal(
                mappedObservation,
                LocationContext(Observation::class),
                tenant
            )
        } returns Pair(
            TransformResponse(roninObservation),
            Validation()
        )

        every { bodyMassIndex.qualifies(mappedObservation) } returns false
        every { bodySurfaceArea.qualifies(mappedObservation) } returns false
        every { bodyTemperature.qualifies(mappedObservation) } returns false
        every { bloodPressure.qualifies(mappedObservation) } returns false
        every { respiratoryRate.qualifies(mappedObservation) } returns false
        every { heartRate.qualifies(mappedObservation) } returns false
        every { pulseOximetry.qualifies(mappedObservation) } returns false
        every { laboratoryResult.qualifies(mappedObservation) } returns false
        every { stagingRelated.qualifies(mappedObservation) } returns false

        every { bodyMassIndex.qualifies(roninObservation) } returns false
        every { bodySurfaceArea.qualifies(roninObservation) } returns false
        every { bodyHeight.qualifies(roninObservation) } returns false
        every { bodyWeight.qualifies(roninObservation) } returns true
        every { bodyWeight.validate(roninObservation, LocationContext(Observation::class)) } returns Validation()
        every { bodyTemperature.qualifies(roninObservation) } returns false
        every { bloodPressure.qualifies(roninObservation) } returns false
        every { respiratoryRate.qualifies(roninObservation) } returns false
        every { heartRate.qualifies(roninObservation) } returns false
        every { pulseOximetry.qualifies(roninObservation) } returns false
        every { laboratoryResult.qualifies(roninObservation) } returns false
        every { stagingRelated.qualifies(roninObservation) } returns false

        val (transformResponse, validation) = roninObservations.transform(original, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(roninObservation, transformed)
    }
}
