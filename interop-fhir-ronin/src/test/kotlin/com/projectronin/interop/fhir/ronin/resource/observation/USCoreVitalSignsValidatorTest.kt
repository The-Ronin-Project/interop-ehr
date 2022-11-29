package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class USCoreVitalSignsValidatorTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `validate fails if no vital signs category`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(text = "laboratory".asFHIR()),
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
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            USCoreVitalSignsValidator.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_VSOBS_001: A category code of \"${USCoreVitalSignsValidator.vitalSignsCode.value}\" is required @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails if no subject`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(text = "laboratory".asFHIR()),
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
            subject = null,
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            USCoreVitalSignsValidator.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ Observation.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails if subject is not a Patient`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(text = "laboratory".asFHIR()),
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
            subject = Reference(reference = "Organization/123".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            USCoreVitalSignsValidator.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_VSOBS_002: Subject must represent a patient @ Observation.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails if no effective`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(text = "laboratory".asFHIR()),
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
            subject = Reference(reference = "Patient/123".asFHIR()),
            effective = null
        )

        val exception = assertThrows<IllegalArgumentException> {
            USCoreVitalSignsValidator.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: effective is a required element @ Observation.effective",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid effective type`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(text = "laboratory".asFHIR()),
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
            subject = Reference(reference = "Patient/123".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.INSTANT,
                value = Instant("2017-01-01T00:00:00Z")
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            USCoreVitalSignsValidator.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_DYN_VAL: effective can only be one of the following: DateTime, Period @ Observation.effective",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(text = "laboratory".asFHIR()),
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

        mockkObject(R4ObservationValidator)
        every { R4ObservationValidator.validate(observation, LocationContext(Observation::class)) } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Observation::basedOn),
                LocationContext(Observation::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            USCoreVitalSignsValidator.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: basedOn is a required element @ Observation.basedOn",
            exception.message
        )

        unmockkObject(R4ObservationValidator)
    }

    @Test
    fun `validate succeeds`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(text = "laboratory".asFHIR()),
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

        USCoreVitalSignsValidator.validate(observation, null).alertIfErrors()
    }
}
