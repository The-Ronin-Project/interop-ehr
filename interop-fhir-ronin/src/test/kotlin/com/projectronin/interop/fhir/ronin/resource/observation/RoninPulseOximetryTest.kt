package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
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
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.resource.ObservationReferenceRange
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninPulseOximetryTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val pulseOxCode = Code("59408-5")
    private val pulseOximetryCoding = listOf(
        Coding(system = CodeSystem.LOINC.uri, code = pulseOxCode)
    )
    private val normRegistryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet("Observation.coding.code", RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)
        } returns pulseOximetryCoding
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninPulseOximetry = RoninPulseOximetry(normalizer, localizer, normRegistryClient)
    private val vitalSignsCategory = Code("vital-signs")
    private val flowRateCode = Code("3151-8")
    private val concentrationCode = Code("3150-0")

    @Test
    fun `does not qualify when no category`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(text = "vital sign".asFHIR())
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
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
                            code = vitalSignsCategory
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

        assertFalse(roninPulseOximetry.qualifies(observation))
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
                            code = vitalSignsCategory
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

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when coding is present, but no entries match either pulse oximetry or oxygen saturation`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                        code = Code("8867-4")
                    ),
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = vitalSignsCategory
                    )
                )
            )
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when wrong system for code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                        system = Uri("faulty"),
                        code = pulseOxCode
                    )
                )
            )
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when coding code present for oxygen saturation, but not pulse oximetry`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                        code = Code("13245")
                    )
                )
            )
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when both coding codes are present, but wrong system for pulse oximetry`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                        system = Uri("faulty"),
                        code = pulseOxCode
                    )
                )
            )
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
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
                            code = vitalSignsCategory
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
                        code = pulseOxCode
                    )
                )
            )
        )

        assertTrue(roninPulseOximetry.qualifies(observation))
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Observation.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Observation.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails if non-pulse oximetry code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = Code("random")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_003: Must match this system|code: http://loinc.org|59408-5 @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no components and no data absent reason for pulse oximetry`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PXOBS_004: Must match this system|code: http://loinc.org|3151-8 @ Observation.component.component\n" +
                "ERROR RONIN_PXOBS_005: Must match this system|code: http://loinc.org|3150-0 @ Observation.component.component",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if no components and data absent reason is provided`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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

        roninPulseOximetry.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `validate fails if flowRate quantity and flowRate data absent reason are both provided`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR R4_OBSCOM_001: dataAbsentReason SHALL only be present if value[x] is not present @ Observation.component[0]",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if flowRate quantity has data absent reason instead of value`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        roninPulseOximetry.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `validate fails if components include conflicting flowRate quantity values`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_PXOBS_005: Only 1 entry is allowed for pulse oximetry flow rate @ Observation.component.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no flowRate quantity value`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.value is a required element @ Observation.valueQuantity.value",
            exception.message
        )
    }

    @Test
    fun `validate fails if no flowRate quantity unit`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.unit is a required element @ Observation.valueQuantity.unit",
            exception.message
        )
    }

    @Test
    fun `validate fails if flowRate quantity system is not UCUM`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = Uri("some-system"),
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_VSOBS_002: Quantity system must be UCUM @ Observation.valueQuantity.system",
            exception.message
        )
    }

    @Test
    fun `validate fails if no flowRate quantity code`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.code is a required element @ Observation.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if flowRate quantity code is outside the required value set`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("invalid-code")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'invalid-code' is outside of required value set @ Observation.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if concentration quantity and concentration data absent reason are both provided`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR R4_OBSCOM_001: dataAbsentReason SHALL only be present if value[x] is not present @ Observation.component[1]",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if concentration quantity has data absent reason instead of value`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        roninPulseOximetry.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `validate fails if components include conflicting concentration quantity values`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_PXOBS_006: Only 1 entry is allowed for pulse oximetry oxygen concentration @ Observation.component.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no concentration quantity value`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.value is a required element @ Observation.valueQuantity.value",
            exception.message
        )
    }

    @Test
    fun `validate fails if no concentration quantity unit`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.unit is a required element @ Observation.valueQuantity.unit",
            exception.message
        )
    }

    @Test
    fun `validate fails if concentration quantity system is not UCUM`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = Uri("some-system"),
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_VSOBS_002: Quantity system must be UCUM @ Observation.valueQuantity.system",
            exception.message
        )
    }

    @Test
    fun `validate fails if no concentration quantity code`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.code is a required element @ Observation.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if concentration quantity code is outside the required value set`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("invalid-code")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'invalid-code' is outside of required value set @ Observation.valueQuantity.code",
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("bad-code")
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: http://terminology.hl7.org/CodeSystem/observation-category|vital-signs @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate succeeds`() {
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        roninPulseOximetry.validate(observation, null).alertIfErrors()
    }

    @Test
    fun `transform fails for observation with no ID`() {
        val observation = Observation(
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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

        val (transformed, _) = roninPulseOximetry.transform(observation, tenant)
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
            basedOn = listOf(Reference(reference = "CarePlan/1234".asFHIR())),
            partOf = listOf(Reference(reference = "MedicationStatement/1234".asFHIR())),
            status = ObservationStatus.AMENDED.asCode(),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                ),
                text = "Pulse Oximetry".asFHIR()
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            focus = listOf(Reference(display = "focus".asFHIR())),
            encounter = Reference(reference = "Encounter/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            issued = Instant("2022-01-01T00:00:00Z"),
            performer = listOf(Reference(reference = "Organization/1234".asFHIR())),
            value = DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            interpretation = listOf(CodeableConcept(text = "interpretation".asFHIR())),
            bodySite = CodeableConcept(text = "bodySite".asFHIR()),
            method = CodeableConcept(text = "method".asFHIR()),
            specimen = Reference(reference = "Specimen/1234".asFHIR()),
            device = Reference(reference = "DeviceMetric/1234".asFHIR()),
            referenceRange = listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())),
            hasMember = listOf(Reference(reference = "Observation/5678".asFHIR())),
            derivedFrom = listOf(Reference(reference = "DocumentReference/1234".asFHIR())),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Flow Rate".asFHIR(),
                                code = flowRateCode
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = concentrationCode
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            ),
            note = listOf(
                Annotation(
                    text = Markdown("text"),
                    author = DynamicValue(type = DynamicValueType.REFERENCE, value = "Practitioner/0001")
                )
            )
        )

        val (transformed, validation) = roninPulseOximetry.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("123"), transformed.id)
        assertEquals(Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value))), transformed.meta)
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
            transformed.identifier
        )
        assertEquals(listOf(Reference(reference = "CarePlan/1234".asFHIR())), transformed.basedOn)
        assertEquals(listOf(Reference(reference = "MedicationStatement/1234".asFHIR())), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                ),
                text = "Pulse Oximetry".asFHIR()
            ),
            transformed.code
        )
        assertEquals(Reference(reference = "Patient/1234".asFHIR()), transformed.subject)
        assertEquals(listOf(Reference(display = "focus".asFHIR())), transformed.focus)
        assertEquals(Reference(reference = "Encounter/1234".asFHIR()), transformed.encounter)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            transformed.effective
        )
        assertEquals(Instant("2022-01-01T00:00:00Z"), transformed.issued)
        assertEquals(listOf(Reference(reference = "Organization/1234".asFHIR())), transformed.performer)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            transformed.value
        )
        assertNull(transformed.dataAbsentReason)
        assertEquals(listOf(CodeableConcept(text = "interpretation".asFHIR())), transformed.interpretation)
        assertEquals(CodeableConcept(text = "bodySite".asFHIR()), transformed.bodySite)
        assertEquals(CodeableConcept(text = "method".asFHIR()), transformed.method)
        assertEquals(Reference(reference = "Specimen/1234".asFHIR()), transformed.specimen)
        assertEquals(Reference(reference = "DeviceMetric/1234".asFHIR()), transformed.device)
        assertEquals(listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())), transformed.referenceRange)
        assertEquals(listOf(Reference(reference = "Observation/5678".asFHIR())), transformed.hasMember)
        assertEquals(listOf(Reference(reference = "DocumentReference/1234".asFHIR())), transformed.derivedFrom)
        assertEquals(
            ObservationComponent(
                code = CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.LOINC.uri,
                            display = "Flow Rate".asFHIR(),
                            code = flowRateCode
                        )
                    ),
                    text = "Flow Rate".asFHIR()
                ),
                value = DynamicValue(
                    DynamicValueType.QUANTITY,
                    Quantity(
                        value = Decimal(value = 110.0),
                        unit = "L/min".asFHIR(),
                        system = CodeSystem.UCUM.uri,
                        code = Code("L/min")
                    )
                )
            ),
            transformed.component[0]
        )
        assertEquals(
            ObservationComponent(
                code = CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.LOINC.uri,
                            display = "Concentration".asFHIR(),
                            code = concentrationCode
                        )
                    ),
                    text = "Concentration".asFHIR()
                ),
                value = DynamicValue(
                    DynamicValueType.QUANTITY,
                    Quantity(
                        value = Decimal(value = 70.0),
                        unit = "%".asFHIR(),
                        system = CodeSystem.UCUM.uri,
                        code = Code("%")
                    )
                )
            ),
            transformed.component[1]
        )
        assertEquals(
            listOf(
                Annotation(
                    text = Markdown("text"),
                    author = DynamicValue(type = DynamicValueType.REFERENCE, value = "Practitioner/0001")
                )
            ),
            transformed.note
        )
    }

    @Test
    fun `transforms observation with only required attributes`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                ),
                text = "Pulse Oximetry".asFHIR()
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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

        val (transformed, validation) = roninPulseOximetry.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("123"), transformed.id)
        assertEquals(Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value))), transformed.meta)
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
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
                            code = vitalSignsCategory
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
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                ),
                text = "Pulse Oximetry".asFHIR()
            ),
            transformed.code
        )
        assertEquals(Reference(reference = "Patient/1234".asFHIR()), transformed.subject)
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

    @Test
    fun `transform inherits R4 validation`() {
        val observation = Observation(
            id = Id("123"),
            status = Code("bad-status"),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
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

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            val (transformed, validation) = roninPulseOximetry.transform(observation, tenant)
            assertNull(transformed)
            validation.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'bad-status' is outside of required value set @ Observation.status",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid derivedFrom reference resource type`() {
        val observation = Observation(
            id = Id("123"),
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
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                ),
                text = "Pulse Oximetry".asFHIR()
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/123".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            derivedFrom = listOf(Reference(reference = "".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not one of DocumentReference, " +
                "ImagingStudy, Media, MolecularSequence, Observation, QuestionnaireResponse @ Observation.derivedFrom[0]",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid hasMember reference resource type`() {
        val observation = Observation(
            id = Id("123"),
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
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                ),
                text = "Pulse Oximetry".asFHIR()
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/123".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            hasMember = listOf(Reference(reference = "".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not one of " +
                "MolecularSequence, Observation, QuestionnaireResponse @ Observation.hasMember[0]",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid partOf reference resource type`() {
        val observation = Observation(
            id = Id("123"),
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
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Pulse Oximetry".asFHIR(),
                        code = pulseOxCode
                    )
                ),
                text = "Pulse Oximetry".asFHIR()
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategory
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/123".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            partOf = listOf(Reference(reference = "".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not one of ImagingStudy, Immunization, " +
                "MedicationAdministration, MedicationDispense, MedicationStatement, Procedure @ Observation.partOf[0]",
            exception.message
        )
    }
}
