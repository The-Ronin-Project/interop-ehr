package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.ObservationComponent
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBloodPressure
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyHeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyTemperature
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyWeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninHeartRate
import com.projectronin.interop.fhir.ronin.resource.observation.RoninLaboratoryResult
import com.projectronin.interop.fhir.ronin.resource.observation.RoninObservation
import com.projectronin.interop.fhir.ronin.resource.observation.RoninPulseOximetry
import com.projectronin.interop.fhir.ronin.resource.observation.RoninRespiratoryRate
import com.projectronin.interop.fhir.ronin.util.unlocalize
import com.projectronin.interop.fhir.util.asCode
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

    // Observation vital signs vs. NOT vital signs - Observation vital signs start here

    @Test
    fun `always qualifies`() {
        assertTrue(
            RoninObservations.qualifies(
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
    fun `can validate body height`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Body Height".asFHIR(),
                        code = RoninBodyHeight.bodyHeightCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("vital-signs")
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(182.88),
                    unit = "cm".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("cm")
                )
            )
        )

        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can validate body weight`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Body Weight".asFHIR(),
                        code = RoninBodyWeight.bodyWeightCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("vital-signs")
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can validate body temperature`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Body Temp".asFHIR(),
                        code = RoninBodyTemperature.bodyTemperatureCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninBodyTemperature.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(37.0),
                    unit = "Cel".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("Cel")
                )
            )
        )

        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can validate blood pressure`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Blood Pressure".asFHIR(),
                        code = RoninBloodPressure.bloodPressureCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninBloodPressure.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Systolic".asFHIR(),
                                code = RoninBloodPressure.systolicCode
                            )
                        ),
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Diastolic".asFHIR(),
                                code = RoninBloodPressure.diastolicCode
                            )
                        ),
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can validate heart rate`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Heart Rate".asFHIR(),
                        code = RoninHeartRate.heartRateCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninRespiratoryRate.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(40.0),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            )
        )

        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can validate respiratory rate`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Respiratory Rate".asFHIR(),
                        code = RoninRespiratoryRate.respiratoryRateCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninRespiratoryRate.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(40.0),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            )
        )

        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can validate pulse oximetry`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninPulseOximetry.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Pulse Oximetry".asFHIR(),
                        code = RoninPulseOximetry.pulseOxCode
                    ),
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "O2 Saturation".asFHIR(),
                        code = RoninPulseOximetry.O2SatCode
                    )
                )
            )
        )

        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can transform body weight`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Body Weight".asFHIR(),
                        code = RoninBodyWeight.bodyWeightCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("vital-signs")
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_WEIGHT.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }

    @Test
    fun `can transform body height`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Body Height".asFHIR(),
                        code = RoninBodyHeight.bodyHeightCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("vital-signs")
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_HEIGHT.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }

    @Test
    fun `can transform blood pressure`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Blood Pressure".asFHIR(),
                        code = RoninBloodPressure.bloodPressureCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninBloodPressure.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Systolic".asFHIR(),
                                code = RoninBloodPressure.systolicCode
                            )
                        ),
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Diastolic".asFHIR(),
                                code = RoninBloodPressure.diastolicCode
                            )
                        ),
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }

    @Test
    fun `can transform body temperature`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Body Temp".asFHIR(),
                        code = RoninBodyTemperature.bodyTemperatureCode
                    )
                ),
                text = "Body Temp".asFHIR()
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninBodyTemperature.vitalSignsCode
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_TEMPERATURE.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }

    @Test
    fun `can transform respiratory rate`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Respiratory Rate".asFHIR(),
                        code = RoninRespiratoryRate.respiratoryRateCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninRespiratoryRate.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(40.0),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_RESPIRATORY_RATE.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }

    @Test
    fun `can transform heart rate`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Heart Rate".asFHIR(),
                        code = RoninHeartRate.heartRateCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninRespiratoryRate.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(40.0),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_HEART_RATE.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }

    @Test
    fun `can transform pulse oximetry`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = RoninPulseOximetry.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Pulse Oximetry".asFHIR(),
                        code = RoninPulseOximetry.pulseOxCode
                    ),
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "O2 Saturation".asFHIR(),
                        code = RoninPulseOximetry.O2SatCode
                    )
                )
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }

    // Observation vital signs vs. NOT vital signs - Observation NOT vital signs start here

    @Test
    fun `validates a not-yet-implemented vital-signs Observation as a non-specific Observation`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("answer"),
                        code = Code("42"),
                        display = "No idea what this means".asFHIR()
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("vital-signs")
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        assertTrue(RoninObservation.qualifies(observation))
        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `validates not-yet-implemented NOT-vital-signs Observation as a non-specific Observation`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.SNOMED_CT.uri,
                        code = Code("160695008"),
                        display = "Transport too expensive".asFHIR()
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("social-history"),
                            display = "Social History".asFHIR()
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        assertTrue(RoninObservation.qualifies(observation))
        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can validate laboratory results`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("laboratory")
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        assertTrue(RoninLaboratoryResult.qualifies(observation))
        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `transforms not-yet-implemented, vital-signs Observation to a non-specific Observation`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("answer"),
                        code = Code("42"),
                        display = "No idea what this means".asFHIR()
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("vital-signs")
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }

    @Test
    fun `transforms not-yet-implemented, NOT-vital-signs Observation to a non-specific Observation`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.SNOMED_CT.uri,
                        code = Code("160695008"),
                        display = "Transport too expensive".asFHIR()
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("social-history"),
                            display = "Social History".asFHIR()
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }

    @Test
    fun `can transform laboratory results`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("laboratory")
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformed, validation) = RoninObservations.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value))),
            transformed.meta
        )
        assertEquals(observation.id?.value, transformed.id?.unlocalize(tenant)?.value)
    }
}
