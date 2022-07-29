package com.projectronin.interop.fhir.ronin.resource

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
import com.projectronin.interop.fhir.r4.datatype.ObservationComponent
import com.projectronin.interop.fhir.r4.datatype.ObservationReferenceRange
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OncologyObservationTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `validate fails if no tenant identifier provided`() {
        val observation = Observation(
            identifier = listOf(Identifier(value = "id")),
            status = ObservationStatus.FINAL,
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://acme-rehab.org"),
                        code = Code("alcohol-type"),
                        display = "Type of alcohol consumed"
                    )
                ),
                text = "Type of alcohol consumed"
            ),
            subject = Reference(
                reference = "Patient/example"
            )
        )
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyObservation.validate(observation)
            }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `validate succeeds for valid observation`() {
        val observation = Observation(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            status = ObservationStatus.FINAL,
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://acme-rehab.org"),
                        code = Code("alcohol-type"),
                        display = "Type of alcohol consumed"
                    )
                ),
                text = "Type of alcohol consumed"
            ),
            subject = Reference(
                reference = "Patient/example"
            )
        )

        OncologyObservation.validate(observation)
    }

    @Test
    fun `transform succeeds`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/observation"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
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
            identifier = listOf(Identifier(value = "id")),
            basedOn = listOf(Reference(display = "Based")),
            partOf = listOf(Reference(display = "Part")),
            status = ObservationStatus.AMENDED,
            category = listOf(CodeableConcept(text = "category")),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "subject"),
            focus = listOf(Reference(display = "focus")),
            encounter = Reference(display = "encounter"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            issued = Instant("2022-01-01T00:00:00Z"),
            performer = listOf(Reference(display = "performer")),
            value = DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            interpretation = listOf(CodeableConcept(text = "interpretation")),
            bodySite = CodeableConcept(text = "bodySite"),
            method = CodeableConcept(text = "method"),
            specimen = Reference(display = "specimen"),
            device = Reference(display = "device"),
            referenceRange = listOf(ObservationReferenceRange(text = "referenceRange")),
            hasMember = listOf(Reference(display = "member")),
            derivedFrom = listOf(Reference(display = "derivedFrom")),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(text = "code2"),
                    value = DynamicValue(
                        type = DynamicValueType.STRING,
                        "string"
                    )
                )
            ),
            note = listOf(Annotation(text = Markdown("text")))
        )

        val transformed = OncologyObservation.transform(observation, tenant)

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("test-123"), transformed.id)
        assertEquals(Meta(profile = listOf(Canonical("https://www.hl7.org/fhir/observation"))), transformed.meta)
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), transformed.text)
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
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertEquals(listOf(Reference(display = "Based")), transformed.basedOn)
        assertEquals(listOf(Reference(display = "Part")), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED, transformed.status)
        assertEquals(listOf(CodeableConcept(text = "category")), transformed.category)
        assertEquals(CodeableConcept(text = "code"), transformed.code)
        assertEquals(Reference(display = "subject"), transformed.subject)
        assertEquals(listOf(Reference(display = "focus")), transformed.focus)
        assertEquals(Reference(display = "encounter"), transformed.encounter)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            transformed.effective
        )
        assertEquals(Instant("2022-01-01T00:00:00Z"), transformed.issued)
        assertEquals(listOf(Reference(display = "performer")), transformed.performer)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            transformed.value
        )
        assertNull(transformed.dataAbsentReason)
        assertEquals(listOf(CodeableConcept(text = "interpretation")), transformed.interpretation)
        assertEquals(CodeableConcept(text = "bodySite"), transformed.bodySite)
        assertEquals(CodeableConcept(text = "method"), transformed.method)
        assertEquals(Reference(display = "specimen"), transformed.specimen)
        assertEquals(Reference(display = "device"), transformed.device)
        assertEquals(listOf(ObservationReferenceRange(text = "referenceRange")), transformed.referenceRange)
        assertEquals(listOf(Reference(display = "member")), transformed.hasMember)
        assertEquals(listOf(Reference(display = "derivedFrom")), transformed.derivedFrom)
        assertEquals(
            listOf(
                ObservationComponent(
                    code = CodeableConcept(text = "code2"),
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
    fun `transform succeeds with some nulls`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED,
            code = CodeableConcept(text = "code"),
            dataAbsentReason = CodeableConcept(text = "dataAbsent")
        )

        val transformed = OncologyObservation.transform(observation, tenant)

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("test-123"), transformed.id)
        assertNull(transformed.meta)
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(listOf<Reference>(), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED, transformed.status)
        assertEquals(listOf<CodeableConcept>(), transformed.category)
        assertEquals(CodeableConcept(text = "code"), transformed.code)
        assertNull(transformed.subject)
        assertEquals(listOf<Reference>(), transformed.focus)
        assertNull(transformed.encounter)
        assertNull(transformed.effective)
        assertNull(transformed.issued)
        assertEquals(listOf<Reference>(), transformed.performer)
        assertNull(transformed.value)
        assertEquals(CodeableConcept(text = "dataAbsent"), transformed.dataAbsentReason)
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
    fun `transform fails without an ID`() {
        val observation = Observation(
            id = null,
            status = ObservationStatus.AMENDED,
            code = CodeableConcept(text = "code"),
            dataAbsentReason = CodeableConcept(text = "dataAbsent")
        )
        val transformed = OncologyObservation.transform(observation, tenant)
        assertNull(transformed)
    }
}
