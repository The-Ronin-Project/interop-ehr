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
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninObservationTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninObservation = RoninObservation(normalizer, localizer, mockk())
    private val vitalSignsCategory = Code("vital-signs")
    private val laboratoryCategory = Code("laboratory")

    @Test
    fun `qualifies when Observation code has the wrong system for vital signs`() {
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
                        system = Uri(value = "any system"),
                        code = Code("8302-2")
                    )
                )
            )
        )

        assertTrue(roninObservation.qualifies(observation))
    }

    @Test
    fun `qualifies when Observation code is totally wrong for vital signs`() {
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
                        system = Uri(value = "any system"),
                        code = Code("not-a-vital-sign")
                    )
                )
            )
        )

        assertTrue(roninObservation.qualifies(observation))
    }

    @Test
    fun `qualifies when Observation code is null`() {
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

        assertTrue(roninObservation.qualifies(observation))
    }

    @Test
    fun `validate fails with code coding being empty`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            coding = listOf()
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            code = CodeableConcept(
                coding = listOf()
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
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninObservation.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Observation.code",
            exception.message
        )
    }

    @Disabled("See INT-1882")
    @Test
    fun `validate fails with code coding display missing`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        system = Uri("some-system")
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
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninObservation.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate fails with code coding system missing`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "laboratory".asFHIR(),
                            coding = listOf(
                                Coding(
                                    code = Code("some-code"),
                                    display = "some-display".asFHIR()
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR()
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            display = "some-display".asFHIR(),
                            code = Code("laboratory")
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninObservation.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate fails with code coding code missing`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "laboratory".asFHIR(),
                            coding = listOf(
                                Coding(
                                    display = "some-display".asFHIR(),
                                    system = Uri("some-system")
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            display = "some-display".asFHIR(),
                            code = Code("laboratory")
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninObservation.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if more than 1 entry in coding list for code when Observation type is not known`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "laboratory".asFHIR(),
                            coding = listOf(
                                Coding(
                                    code = Code("some-code-1"),
                                    display = "some-display-1".asFHIR(),
                                    system = Uri("some-system-1")
                                ),
                                Coding(
                                    code = Code("some-code-2"),
                                    display = "some-display-2".asFHIR(),
                                    system = Uri("some-system-2")
                                ),
                                Coding(
                                    code = Code("some-code-3"),
                                    display = "some-display-3".asFHIR(),
                                    system = Uri("some-system-3")
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code-1"),
                        display = "some-display-1".asFHIR(),
                        system = Uri("some-system-1")
                    ),
                    Coding(
                        code = Code("some-code-2"),
                        display = "some-display-2".asFHIR(),
                        system = Uri("some-system-2")
                    ),
                    Coding(
                        code = Code("some-code-3"),
                        display = "some-display-3".asFHIR(),
                        system = Uri("some-system-3")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            code = Code("laboratory")
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        roninObservation.validate(observation).alertIfErrors()
    }

    @Test
    fun `qualifies when Observation category has the wrong system`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri(value = "not a good system at all"),
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
                text = "any-text".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            )
        )

        assertTrue(roninObservation.qualifies(observation))
    }

    @Test
    fun `qualifies when Observation category is totally wrong`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri(value = "not-a-good-system"),
                            code = Code(value = "not a good code")
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
                text = "any-text".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            )
        )

        assertTrue(roninObservation.qualifies(observation))
    }

    @Test
    fun `qualifies when Observation category is empty`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(CodeableConcept()),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                text = "any-text".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            )
        )

        assertTrue(roninObservation.qualifies(observation))
    }

    @Test
    fun `validate succeeds with no category`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "any-text".asFHIR(),
                            coding = listOf(
                                Coding(
                                    code = Code("some-code"),
                                    display = "some-display".asFHIR(),
                                    system = Uri("some-system")
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                text = "any-text".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            ),
            category = listOf(CodeableConcept(coding = listOf())),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        roninObservation.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails if no subject`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "any-text".asFHIR(),
                            coding = listOf(
                                Coding(
                                    code = Code("some-code"),
                                    display = "some-display".asFHIR(),
                                    system = Uri("some-system")
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                text = "any-text".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    text = "any-text".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("some-code"),
                            display = "some-display".asFHIR(),
                            system = Uri("some-system")
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = null,
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninObservation.validate(observation).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "any-text".asFHIR(),
                            coding = listOf(
                                Coding(
                                    code = Code("some-code"),
                                    display = "some-display".asFHIR(),
                                    system = Uri("some-system")
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                text = "any-text".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    text = "any-text".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("some-code"),
                            display = "some-display".asFHIR(),
                            system = Uri("some-system")
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Group/123".asFHIR(),
                type = Uri("Something", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val validation = roninObservation.validate(observation)

        println(validation.issues())
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Patient @ Observation.subject.reference",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if invalid effective type`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = CodeSystem.LOINC.uri,
                                    display = "Laboratory Result".asFHIR(),
                                    code = laboratoryCategory
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Laboratory Result".asFHIR(),
                        code = laboratoryCategory
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
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.BOOLEAN,
                value = true
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninObservation.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_DYN_VAL: effective can only be one of the following: DateTime, Period, Timing, Instant @ Observation.effective",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid author data type`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = CodeSystem.LOINC.uri,
                                    display = "Laboratory Result".asFHIR(),
                                    code = laboratoryCategory
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Laboratory Result".asFHIR(),
                        code = laboratoryCategory
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
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            note = listOf(
                Annotation(
                    text = Markdown("text"),
                    author = DynamicValue(
                        type = DynamicValueType.STRING,
                        value = "Dr Simmons"
                    )
                ),
                Annotation(
                    text = Markdown("more text"),
                    author = DynamicValue(
                        type = DynamicValueType.BOOLEAN,
                        value = true
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninObservation.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_DYN_VAL: author can only be one of the following: Reference, String @ Observation.note[1].author",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = CodeSystem.LOINC.uri,
                                    display = "Laboratory Result".asFHIR(),
                                    code = laboratoryCategory
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Laboratory Result".asFHIR(),
                        code = laboratoryCategory
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
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
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
            roninObservation.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: basedOn is a required element @ Observation.basedOn",
            exception.message
        )

        unmockkObject(R4ObservationValidator)
    }

    @Test
    fun `validate fails with subject and type but no data authority reference extension`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "any-text".asFHIR(),
                            coding = listOf(
                                Coding(
                                    code = Code("some-code"),
                                    display = "some-display".asFHIR(),
                                    system = Uri("some-system")
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                text = "any-text".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("any-code")
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient")
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninObservation.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ Observation.subject.type.extension",
            exception.message
        )
    }

    @Test
    fun `validate checks meta`() {
        val observation = Observation(
            id = Id("123"),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "any-text".asFHIR(),
                            coding = listOf(
                                Coding(
                                    code = Code("some-code"),
                                    display = "some-display".asFHIR(),
                                    system = Uri("some-system")
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                text = "any-text".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("any-code")
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninObservation.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Observation.meta",
            exception.message
        )
    }

    @Test
    fun `validate succeeds`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "any-text".asFHIR(),
                            coding = listOf(
                                Coding(
                                    code = Code("some-code"),
                                    display = "some-display".asFHIR(),
                                    system = Uri("some-system")
                                )
                            )
                        )
                    )
                )
            ),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                text = "any-text".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("any-code")
                        )
                    )
                )
            ),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        roninObservation.validate(observation).alertIfErrors()
    }
}
