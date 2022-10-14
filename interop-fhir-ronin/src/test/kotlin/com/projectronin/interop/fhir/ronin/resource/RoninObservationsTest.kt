package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyHeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyWeight
import com.projectronin.interop.fhir.ronin.resource.observation.USCoreVitalSignsValidator
import com.projectronin.interop.fhir.ronin.util.asCode
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
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "123"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = RoninBodyHeight.bodyHeightCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = USCoreVitalSignsValidator.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = 182.88,
                    unit = "cm",
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
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "123"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = RoninBodyWeight.bodyWeightCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = USCoreVitalSignsValidator.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = 68.04,
                    unit = "kg",
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can validate vital signs`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "123"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            code = CodeableConcept(text = "vital sign"),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = USCoreVitalSignsValidator.vitalSignsCode
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = 68.04,
                    unit = "kg",
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        RoninObservations.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `can validate laboratory results`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "123"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            code = CodeableConcept(text = "laboratory"),
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
            subject = Reference(reference = "Patient/1234"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = 68.04,
                    unit = "kg",
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
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
                        code = RoninBodyWeight.bodyWeightCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = USCoreVitalSignsValidator.vitalSignsCode
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent"),
            subject = Reference(reference = "Patient/1234"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val transformed = RoninObservations.transform(observation, tenant)
        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_WEIGHT.value))),
            transformed.meta
        )
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
                        code = RoninBodyHeight.bodyHeightCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = USCoreVitalSignsValidator.vitalSignsCode
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent"),
            subject = Reference(reference = "Patient/1234"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val transformed = RoninObservations.transform(observation, tenant)
        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_HEIGHT.value))),
            transformed.meta
        )
    }

    @Test
    fun `can transform vital signs`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(text = "vital sign"),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = USCoreVitalSignsValidator.vitalSignsCode
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent"),
            subject = Reference(reference = "Patient/1234"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val transformed = RoninObservations.transform(observation, tenant)
        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_VITAL_SIGNS.value))),
            transformed.meta
        )
    }

    @Test
    fun `can transform laboratory results`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(text = "laboratory"),
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
            dataAbsentReason = CodeableConcept(text = "dataAbsent"),
            subject = Reference(reference = "Patient/1234"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val transformed = RoninObservations.transform(observation, tenant)
        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value))),
            transformed.meta
        )
    }
}
