package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.ObservationComponent
import com.projectronin.interop.fhir.r4.datatype.ObservationReferenceRange
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninBodyHeightTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `does not qualify when no code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = null
        )

        val qualified = RoninBodyHeight.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when no coding`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(text = "code".asFHIR())
        )

        val qualified = RoninBodyHeight.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when coding code not for body height`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(coding = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("1234"))))
        )

        val qualified = RoninBodyHeight.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when coding code is for body height but wrong system`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.UCUM.uri,
                        code = RoninBodyHeight.bodyHeightCode
                    )
                )
            )
        )

        val qualified = RoninBodyHeight.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `qualifies for profile`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = RoninBodyHeight.bodyHeightCode
                    )
                )
            )
        )

        val qualified = RoninBodyHeight.qualifies(observation)
        assertTrue(qualified)
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninBodyHeight.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Observation.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Observation.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails if bodySite set`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            bodySite = CodeableConcept(text = "knee".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninBodyHeight.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_HTOBS_001: bodySite not allowed for Body Height observation @ Observation.bodySite",
            exception.message
        )
    }

    @Test
    fun `validate fails if non-body height code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = Code("wrong-code")
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninBodyHeight.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_HTOBS_001: LOINC code ${RoninBodyHeight.bodyHeightCode.value} required for US Core Body Height profile @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no quantity value`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    unit = "cm".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("cm")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninBodyHeight.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.value is a required element @ Observation.valueQuantity.value",
            exception.message
        )
    }

    @Test
    fun `validate fails if no quantity unit`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(182.88),
                    system = CodeSystem.UCUM.uri,
                    code = Code("cm")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninBodyHeight.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.unit is a required element @ Observation.valueQuantity.unit",
            exception.message
        )
    }

    @Test
    fun `validate fails if quantity system is not UCUM`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
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
                    system = CodeSystem.LOINC.uri,
                    code = Code("cm")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninBodyHeight.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_HTOBS_002: Quantity system must be UCUM @ Observation.valueQuantity.system",
            exception.message
        )
    }

    @Test
    fun `validate fails if no quantity code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
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
                    system = CodeSystem.UCUM.uri
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninBodyHeight.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.code is a required element @ Observation.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if quantity code is of an invalid type`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
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
                    code = Code("invalid-code")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninBodyHeight.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: valueQuantity.code is outside of required value set @ Observation.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate checks US Core vital signs profile`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
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

        mockkObject(USCoreVitalSignsValidator)
        every {
            USCoreVitalSignsValidator.validate(
                observation,
                LocationContext(Observation::class)
            )
        } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Observation::basedOn),
                LocationContext(Observation::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            RoninBodyHeight.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: basedOn is a required element @ Observation.basedOn",
            exception.message
        )

        unmockkObject(USCoreVitalSignsValidator)
    }

    @Test
    fun `validate succeeds`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
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

        RoninBodyHeight.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `transform fails for observation with no ID`() {
        val observation = Observation(
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
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

        val transformed = RoninBodyHeight.transform(observation, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transforms observation with all attributes`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/observation"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            basedOn = listOf(Reference(display = "Based".asFHIR())),
            partOf = listOf(Reference(display = "Part".asFHIR())),
            status = ObservationStatus.AMENDED.asCode(),
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
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = RoninBodyHeight.bodyHeightCode
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            focus = listOf(Reference(display = "focus".asFHIR())),
            encounter = Reference(display = "encounter".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            issued = Instant("2022-01-01T00:00:00Z"),
            performer = listOf(Reference(display = "performer".asFHIR())),
            value = DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            interpretation = listOf(CodeableConcept(text = "interpretation".asFHIR())),
            bodySite = CodeableConcept(text = "bodySite".asFHIR()),
            method = CodeableConcept(text = "method".asFHIR()),
            specimen = Reference(display = "specimen".asFHIR()),
            device = Reference(display = "device".asFHIR()),
            referenceRange = listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())),
            hasMember = listOf(Reference(display = "member".asFHIR())),
            derivedFrom = listOf(Reference(display = "derivedFrom".asFHIR())),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(text = "code2".asFHIR()),
                    value = DynamicValue(
                        type = DynamicValueType.STRING,
                        "string"
                    )
                )
            ),
            note = listOf(Annotation(text = Markdown("text")))
        )

        val transformed = RoninBodyHeight.transform(observation, tenant)

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("test-123"), transformed.id)
        assertEquals(Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_HEIGHT.value))), transformed.meta)
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id".asFHIR()),
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(listOf(Reference(display = "Based".asFHIR())), transformed.basedOn)
        assertEquals(listOf(Reference(display = "Part".asFHIR())), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = USCoreVitalSignsValidator.vitalSignsCode
                        )
                    )
                )
            ),
            transformed.category
        )
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = RoninBodyHeight.bodyHeightCode
                    )
                )
            ),
            transformed.code
        )
        assertEquals(Reference(reference = "Patient/test-1234".asFHIR()), transformed.subject)
        assertEquals(listOf(Reference(display = "focus".asFHIR())), transformed.focus)
        assertEquals(Reference(display = "encounter".asFHIR()), transformed.encounter)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            transformed.effective
        )
        assertEquals(Instant("2022-01-01T00:00:00Z"), transformed.issued)
        assertEquals(listOf(Reference(display = "performer".asFHIR())), transformed.performer)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            transformed.value
        )
        assertNull(transformed.dataAbsentReason)
        assertEquals(listOf(CodeableConcept(text = "interpretation".asFHIR())), transformed.interpretation)
        assertNull(transformed.bodySite)
        assertEquals(CodeableConcept(text = "method".asFHIR()), transformed.method)
        assertEquals(Reference(display = "specimen".asFHIR()), transformed.specimen)
        assertEquals(Reference(display = "device".asFHIR()), transformed.device)
        assertEquals(listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())), transformed.referenceRange)
        assertEquals(listOf(Reference(display = "member".asFHIR())), transformed.hasMember)
        assertEquals(listOf(Reference(display = "derivedFrom".asFHIR())), transformed.derivedFrom)
        assertEquals(
            listOf(
                ObservationComponent(
                    code = CodeableConcept(text = "code2".asFHIR()),
                    value = DynamicValue(
                        type = DynamicValueType.STRING,
                        "string"
                    )
                )
            ),
            transformed.component
        )
        assertEquals(listOf(Annotation(text = Markdown("text"))), transformed.note)
    }

    @Test
    fun `transforms condition with only required attributes`() {
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
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val transformed = RoninBodyHeight.transform(observation, tenant)

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("test-123"), transformed.id)
        assertEquals(Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_HEIGHT.value))), transformed.meta)
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(listOf<Reference>(), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = USCoreVitalSignsValidator.vitalSignsCode
                        )
                    )
                )
            ),
            transformed.category
        )
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = RoninBodyHeight.bodyHeightCode
                    )
                )
            ),
            transformed.code
        )
        assertEquals(Reference(reference = "Patient/test-1234".asFHIR()), transformed.subject)
        assertEquals(listOf<Reference>(), transformed.focus)
        assertNull(transformed.encounter)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            transformed.effective
        )
        assertNull(transformed.issued)
        assertEquals(listOf<Reference>(), transformed.performer)
        assertNull(transformed.value)
        assertEquals(CodeableConcept(text = "dataAbsent".asFHIR()), transformed.dataAbsentReason)
        assertEquals(listOf<CodeableConcept>(), transformed.interpretation)
        assertNull(transformed.bodySite)
        assertNull(transformed.method)
        assertNull(transformed.specimen)
        assertNull(transformed.device)
        assertEquals(listOf<ObservationReferenceRange>(), transformed.referenceRange)
        assertEquals(listOf<Reference>(), transformed.hasMember)
        assertEquals(listOf<Reference>(), transformed.derivedFrom)
        assertEquals(listOf<ObservationComponent>(), transformed.component)
        assertEquals(listOf<Annotation>(), transformed.note)
    }
}
