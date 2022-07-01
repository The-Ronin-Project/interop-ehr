package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
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
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.ehr.model.Observation as EHRObservation
import com.projectronin.interop.fhir.r4.resource.Observation as R4Observation

class R4ObservationTransformerTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val transformer = R4ObservationTransformer()

    @Test
    fun `transforms ok`() {
        val observation = R4Observation(
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
            dataAbsentReason = null,
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
        val mockObservation = mockk<EHRObservation> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns observation
        }
        val transformedObservation = transformer.transformObservation(mockObservation, tenant)
        assertNotNull(transformedObservation)
        assertEquals("${tenant.mnemonic}-${observation.id?.value}", transformedObservation?.id?.value)
        val tenantId = transformedObservation?.identifier?.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }
        assertNotNull(tenantId)
        assertEquals(tenant.mnemonic, tenantId?.value)
    }

    @Test
    fun `transforms ok with some nulls`() {
        val observation = R4Observation(
            id = Id("123"),
            meta = null,
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = null,
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
            subject = null,
            focus = listOf(Reference(display = "focus")),
            encounter = null,
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            issued = Instant("2022-01-01T00:00:00Z"),
            performer = listOf(Reference(display = "performer")),
            value = null,
            dataAbsentReason = CodeableConcept(text = "dataAbsent"),
            interpretation = listOf(CodeableConcept(text = "interpretation")),
            bodySite = null,
            method = null,
            specimen = null,
            device = null,
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
        val mockObservation = mockk<EHRObservation> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns observation
        }
        val transformedObservation = transformer.transformObservation(mockObservation, tenant)
        assertNotNull(transformedObservation)
    }

    @Test
    fun `transform fails without an ID`() {
        val observation = R4Observation(
            id = null,
            status = ObservationStatus.AMENDED,
            code = CodeableConcept(text = "code"),
        )
        val mockObservation = mockk<EHRObservation> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns observation
        }
        val transformedObservation = transformer.transformObservation(mockObservation, tenant)
        assertNull(transformedObservation)
    }

    @Test
    fun `transform fails when not R4`() {
        val observation = R4Observation(
            status = ObservationStatus.AMENDED,
            code = CodeableConcept(text = "code"),
        )
        val mockObservation = mockk<EHRObservation> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns observation
        }
        assertThrows<IllegalArgumentException> { transformer.transformObservation(mockObservation, tenant) }
    }

    @Test
    fun `transform bundle works`() {
        val observation = R4Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED,
            code = CodeableConcept(text = "code"),
        )
        val mockObservation = mockk<EHRObservation> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns observation
        }
        val mockBundle = mockk<Bundle<EHRObservation>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(mockObservation)
        }
        assertNotNull(transformer.transformObservations(mockBundle, tenant))
    }

    @Test
    fun `transform bundle fails when not r4`() {
        val mockBundle = mockk<Bundle<EHRObservation>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }
        assertThrows<IllegalArgumentException> { transformer.transformObservations(mockBundle, tenant) }
    }
}
