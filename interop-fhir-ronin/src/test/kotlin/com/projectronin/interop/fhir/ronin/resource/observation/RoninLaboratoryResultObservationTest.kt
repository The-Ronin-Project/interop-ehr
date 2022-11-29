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
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
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

class RoninLaboratoryResultObservationTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

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
            code = CodeableConcept(text = "lab".asFHIR())
        )

        val qualified = RoninLaboratoryResultObservation.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when no coding`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(CodeableConcept(coding = listOf())),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(text = "lab".asFHIR())
        )

        val qualified = RoninLaboratoryResultObservation.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when coding code not for laboratory`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("not-laboratory")
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(text = "lab".asFHIR())
        )

        val qualified = RoninLaboratoryResultObservation.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when coding code is for vital signs but wrong system`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
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
            code = CodeableConcept(text = "lab".asFHIR())
        )

        val qualified = RoninLaboratoryResultObservation.qualifies(observation)
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
            code = CodeableConcept(text = "lab".asFHIR())
        )

        val qualified = RoninLaboratoryResultObservation.qualifies(observation)
        assertTrue(qualified)
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            code = CodeableConcept(text = "lab".asFHIR()),
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
            subject = Reference(reference = "Patient/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninLaboratoryResultObservation.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Observation.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Observation.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails if no laboratory category`() {
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
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            code = CodeableConcept(text = "laboratory".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("not-laboratory")
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninLaboratoryResultObservation.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_LABOBS_001: A category code of \"laboratory\" is required @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails if no subject`() {
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
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            subject = null
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninLaboratoryResultObservation.validate(observation, null).alertIfErrors()
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
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            subject = Reference(reference = "Organization/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninLaboratoryResultObservation.validate(observation, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_LABOBS_002: Subject must represent a patient @ Observation.subject",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
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

        mockkObject(R4ObservationValidator)
        every { R4ObservationValidator.validate(observation, LocationContext(Observation::class)) } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Observation::basedOn),
                LocationContext(Observation::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            RoninLaboratoryResultObservation.validate(observation, null).alertIfErrors()
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

        RoninLaboratoryResultObservation.validate(observation, null).alertIfErrors()
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

        val transformed = RoninLaboratoryResultObservation.transform(observation, tenant)
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
                            code = Code("laboratory")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "laboratory".asFHIR()),
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

        val transformed = RoninLaboratoryResultObservation.transform(observation, tenant)

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("test-123"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value))),
            transformed.meta
        )
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
                            code = Code("laboratory")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertEquals(CodeableConcept(text = "laboratory".asFHIR()), transformed.code)
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
        assertEquals(CodeableConcept(text = "bodySite".asFHIR()), transformed.bodySite)
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
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val transformed = RoninLaboratoryResultObservation.transform(observation, tenant)

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("test-123"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value))),
            transformed.meta
        )
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
                            code = Code("laboratory")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertEquals(CodeableConcept(text = "laboratory".asFHIR()), transformed.code)
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
