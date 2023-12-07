package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestIntent
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestStatus
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

class NormalizerTest {
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    private val extensions =
        listOf(
            Extension(
                id = "5678".asFHIR(),
                url = Uri("http://normalhost/extension"),
                value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/1234".asFHIR())),
            ),
        )

    private val normalizer = Normalizer()

    private fun normalizeCoding(coding: Coding): Coding? {
        val normalizeCodingMethod = Normalizer::class.functions.find { it.name == "normalizeCoding" }!!
        normalizeCodingMethod.isAccessible = true
        val normalized = normalizeCodingMethod.call(normalizer, coding, tenant) as? Coding
        normalizeCodingMethod.isAccessible = false
        return normalized
    }

    private fun normalizeIdentifier(identifier: Identifier): Identifier? {
        val normalizeIdentifierMethod = Normalizer::class.functions.find { it.name == "normalizeIdentifier" }!!
        normalizeIdentifierMethod.isAccessible = true
        val normalized = normalizeIdentifierMethod.call(normalizer, identifier, tenant) as? Identifier
        normalizeIdentifierMethod.isAccessible = false
        return normalized
    }

    private fun normalizeCodeableConcept(codeableConcept: CodeableConcept): CodeableConcept? {
        val normalizeCodeableConceptMethod =
            Normalizer::class.functions.find { it.name == "normalizeCodeableConcept" }!!
        normalizeCodeableConceptMethod.isAccessible = true
        val normalized =
            normalizeCodeableConceptMethod.call(normalizer, codeableConcept, tenant) as? CodeableConcept
        normalizeCodeableConceptMethod.isAccessible = false
        return normalized
    }

    @Test
    fun `normalize works for object with no unnormalized types supplied`() {
        val location = Location(id = Id("1234"))
        val normalizedLocation = normalizer.normalize(location, tenant)
        assertEquals(location, normalizedLocation)
    }

    @Test
    fun `normalize works for object with nested normalizable value with no normalization`() {
        val location =
            Location(
                id = Id("1234"),
                operationalStatus =
                    Coding(
                        id = "12345".asFHIR(),
                        extension = extensions,
                        system = Uri("non-normalizable-system"),
                        version = "version".asFHIR(),
                        code = Code("code"),
                        display = "Display".asFHIR(),
                        userSelected = FHIRBoolean.TRUE,
                    ),
            )
        val normalizedLocation = normalizer.normalize(location, tenant)
        assertEquals(location, normalizedLocation)
    }

    @Test
    fun `normalize works for object with nested normalizable value with normalization`() {
        val location =
            Location(
                id = Id("1234"),
                operationalStatus =
                    Coding(
                        id = "12345".asFHIR(),
                        extension = extensions,
                        system = Uri("urn:oid:2.16.840.1.113883.6.1"),
                        version = "version".asFHIR(),
                        code = Code("code"),
                        display = "Display".asFHIR(),
                        userSelected = FHIRBoolean.TRUE,
                    ),
            )
        val normalizedLocation = normalizer.normalize(location, tenant)

        val expectedLocation =
            Location(
                id = Id("1234"),
                operationalStatus =
                    Coding(
                        id = "12345".asFHIR(),
                        extension = extensions,
                        system = Uri("http://loinc.org"),
                        version = "version".asFHIR(),
                        code = Code("code"),
                        display = "Display".asFHIR(),
                        userSelected = FHIRBoolean.TRUE,
                    ),
            )
        assertEquals(expectedLocation, normalizedLocation)
    }

    @Test
    fun `normalizes coding - mapped system with urn`() {
        val coding =
            Coding(
                id = "12345".asFHIR(),
                extension = extensions,
                system = Uri("urn:oid:2.16.840.1.113883.6.1"),
                version = "version".asFHIR(),
                code = Code("code"),
                display = "Display".asFHIR(),
                userSelected = FHIRBoolean.TRUE,
            )
        val normalizedCoding = normalizeCoding(coding)
        assertNotEquals(coding, normalizedCoding)

        val expectedCoding =
            Coding(
                id = "12345".asFHIR(),
                extension = extensions,
                system = Uri("http://loinc.org"),
                version = "version".asFHIR(),
                code = Code("code"),
                display = "Display".asFHIR(),
                userSelected = FHIRBoolean.TRUE,
            )
        assertEquals(expectedCoding, normalizedCoding)
    }

    @Test
    fun `normalizes coding - mapped system`() {
        val coding =
            Coding(
                id = "12345".asFHIR(),
                extension = extensions,
                system = Uri("2.16.840.1.113883.6.1"),
                version = "version".asFHIR(),
                code = Code("code"),
                display = "Display".asFHIR(),
                userSelected = FHIRBoolean.TRUE,
            )
        val normalizedCoding = normalizeCoding(coding)
        assertNotEquals(coding, normalizedCoding)

        val expectedCoding =
            Coding(
                id = "12345".asFHIR(),
                extension = extensions,
                system = Uri("http://loinc.org"),
                version = "version".asFHIR(),
                code = Code("code"),
                display = "Display".asFHIR(),
                userSelected = FHIRBoolean.TRUE,
            )
        assertEquals(expectedCoding, normalizedCoding)
    }

    @Test
    fun `normalizes identifier - mapped system with urn`() {
        val identifier =
            Identifier(
                id = "12345".asFHIR(),
                extension = extensions,
                use = IdentifierUse.OFFICIAL.asCode(),
                type = CodeableConcept(text = "type".asFHIR()),
                system = Uri("urn:oid:2.16.840.1.113883.4.1"),
                value = "value".asFHIR(),
                period = Period(start = DateTime("2021")),
                assigner = Reference(display = "assigner".asFHIR()),
            )
        val normalizedIdentifier = normalizeIdentifier(identifier)
        assertNotEquals(extensions, normalizedIdentifier)

        val expectedIdentifier =
            Identifier(
                id = "12345".asFHIR(),
                extension = extensions,
                use = IdentifierUse.OFFICIAL.asCode(),
                type = CodeableConcept(text = "type".asFHIR()),
                system = Uri("http://hl7.org/fhir/sid/us-ssn"),
                value = "value".asFHIR(),
                period = Period(start = DateTime("2021")),
                assigner = Reference(display = "assigner".asFHIR()),
            )
        assertEquals(expectedIdentifier, normalizedIdentifier)
    }

    @Test
    fun `normalizes identifier - mapped system`() {
        val identifier =
            Identifier(
                id = "12345".asFHIR(),
                extension = extensions,
                use = IdentifierUse.OFFICIAL.asCode(),
                type = CodeableConcept(text = "type".asFHIR()),
                system = Uri("2.16.840.1.113883.4.1"),
                value = "value".asFHIR(),
                period = Period(start = DateTime("2021")),
                assigner = Reference(display = "assigner".asFHIR()),
            )
        val normalizedIdentifier = normalizeIdentifier(identifier)
        assertNotEquals(extensions, normalizedIdentifier)

        val expectedIdentifier =
            Identifier(
                id = "12345".asFHIR(),
                extension = extensions,
                use = IdentifierUse.OFFICIAL.asCode(),
                type = CodeableConcept(text = "type".asFHIR()),
                system = Uri("http://hl7.org/fhir/sid/us-ssn"),
                value = "value".asFHIR(),
                period = Period(start = DateTime("2021")),
                assigner = Reference(display = "assigner".asFHIR()),
            )
        assertEquals(expectedIdentifier, normalizedIdentifier)
    }

    @Test
    fun `normalize codeable concept text - text already set - no normalization`() {
        val codeableConcept = CodeableConcept(text = "Text Set".asFHIR())
        val normalizedCodeableConcept = normalizeCodeableConcept(codeableConcept)

        val expectedCodeableConcept = CodeableConcept(text = "Text Set".asFHIR())
        assertEquals(expectedCodeableConcept, normalizedCodeableConcept)
    }

    @Test
    fun `normalize codeable concept text - no text - pull from single coding`() {
        val codeableConcept = CodeableConcept(coding = listOf(Coding(display = "Coding Display".asFHIR())))
        val normalizedCodeableConcept = normalizeCodeableConcept(codeableConcept)

        val expectedCodeableConcept =
            CodeableConcept(
                text = "Coding Display".asFHIR(),
                coding = listOf(Coding(display = "Coding Display".asFHIR())),
            )
        assertEquals(expectedCodeableConcept, normalizedCodeableConcept)
    }

    @Test
    fun `normalize codeable concept text - no text value - pull from single coding`() {
        val codeableConcept =
            CodeableConcept(text = FHIRString(null), coding = listOf(Coding(display = "Coding Display".asFHIR())))
        val normalizedCodeableConcept = normalizeCodeableConcept(codeableConcept)

        val expectedCodeableConcept =
            CodeableConcept(
                text = "Coding Display".asFHIR(),
                coding = listOf(Coding(display = "Coding Display".asFHIR())),
            )
        assertEquals(expectedCodeableConcept, normalizedCodeableConcept)
    }

    @Test
    fun `normalize codeable concept text - no text - pull from single coding user selected`() {
        val codeableConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(display = "Coding Display".asFHIR()),
                        Coding(display = "Coding Display 2".asFHIR()),
                        Coding(display = "User Selected Display".asFHIR(), userSelected = FHIRBoolean.TRUE),
                    ),
            )
        val normalizedCodeableConcept = normalizeCodeableConcept(codeableConcept)

        val expectedCodeableConcept =
            CodeableConcept(
                text = "User Selected Display".asFHIR(),
                coding =
                    listOf(
                        Coding(display = "Coding Display".asFHIR()),
                        Coding(display = "Coding Display 2".asFHIR()),
                        Coding(display = "User Selected Display".asFHIR(), userSelected = FHIRBoolean.TRUE),
                    ),
            )
        assertEquals(expectedCodeableConcept, normalizedCodeableConcept)
    }

    @Test
    fun `normalize codeable concept text - no text - single coding user selected with no display`() {
        val codeableConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(display = "Coding Display".asFHIR()),
                        Coding(display = "Coding Display 2".asFHIR()),
                        Coding(userSelected = FHIRBoolean.TRUE),
                    ),
            )
        val normalizedCodeableConcept = normalizeCodeableConcept(codeableConcept)

        assertEquals(codeableConcept, normalizedCodeableConcept)
    }

    @Test
    fun `normalize codeable concept text - no text - single coding user selected with display with null value`() {
        val codeableConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(display = "Coding Display".asFHIR()),
                        Coding(display = "Coding Display 2".asFHIR()),
                        Coding(display = FHIRString(null), userSelected = FHIRBoolean.TRUE),
                    ),
            )
        val normalizedCodeableConcept = normalizeCodeableConcept(codeableConcept)

        assertEquals(codeableConcept, normalizedCodeableConcept)
    }

    @Test
    fun `normalize codeable concept text - no text - single coding user selected with display with empty value`() {
        val codeableConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(display = "Coding Display".asFHIR()),
                        Coding(display = "Coding Display 2".asFHIR()),
                        Coding(display = FHIRString(""), userSelected = FHIRBoolean.TRUE),
                    ),
            )
        val normalizedCodeableConcept = normalizeCodeableConcept(codeableConcept)

        assertEquals(codeableConcept, normalizedCodeableConcept)
    }

    @Test
    fun `normalize codeable concept text - no text, multiple codings - no normalization`() {
        val codeableConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(display = "Coding Display".asFHIR()),
                        Coding(display = "Coding Display 2".asFHIR()),
                    ),
            )
        val normalizedCodeableConcept = normalizeCodeableConcept(codeableConcept)

        val expectedCodeableConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(display = "Coding Display".asFHIR()),
                        Coding(display = "Coding Display 2".asFHIR()),
                    ),
            )
        assertEquals(expectedCodeableConcept, normalizedCodeableConcept)
    }

    @Test
    fun `normalize codeable concept text - no text, multiple user selected codings - no normalization`() {
        val codeableConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(display = "Coding Display".asFHIR()),
                        Coding(display = "Coding Display 2".asFHIR()),
                        Coding(display = "User Selected Display 1".asFHIR(), userSelected = FHIRBoolean.TRUE),
                        Coding(display = "User Selected Display 2".asFHIR(), userSelected = FHIRBoolean.TRUE),
                    ),
            )
        val normalizedCodeableConcept = normalizeCodeableConcept(codeableConcept)

        val expectedCodeableConcept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(display = "Coding Display".asFHIR()),
                        Coding(display = "Coding Display 2".asFHIR()),
                        Coding(display = "User Selected Display 1".asFHIR(), userSelected = FHIRBoolean.TRUE),
                        Coding(display = "User Selected Display 2".asFHIR(), userSelected = FHIRBoolean.TRUE),
                    ),
            )
        assertEquals(expectedCodeableConcept, normalizedCodeableConcept)
    }

    @Test
    fun `normalize extension with no value`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("something here"),
                        ),
                    ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension.size, 0)
    }

    @Test
    fun `normalize extension with no url`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                    ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(0, normalizedExtension.extension.size)
    }

    @Test
    fun `normalize extension with non-ronin url and value`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("some-extension-url"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                    ),
            )
        val expectedExtension =
            listOf(
                Extension(
                    url = Uri("some-extension-url"),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODING,
                            value =
                                Coding(
                                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                    code = Code(value = "abc"),
                                ),
                        ),
                ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension, expectedExtension)
    }

    @Test
    fun `normalize extension with non-ronin url and extension`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("some-extension-url"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("some-extension-url-2"),
                                        value =
                                            DynamicValue(
                                                type = DynamicValueType.CODING,
                                                value =
                                                    Coding(
                                                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                                        code = Code(value = "abc"),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )
        val expectedExtension =
            listOf(
                Extension(
                    url = Uri("some-extension-url"),
                    extension =
                        listOf(
                            Extension(
                                url = Uri("some-extension-url-2"),
                                value =
                                    DynamicValue(
                                        type = DynamicValueType.CODING,
                                        value =
                                            Coding(
                                                system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                                code = Code(value = "abc"),
                                            ),
                                    ),
                            ),
                        ),
                ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension, expectedExtension)
    }

    @Test
    fun `normalize extension will filter sub-extensions`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("some-extension-url"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("some-extension-url-2"),
                                        value =
                                            DynamicValue(
                                                type = DynamicValueType.CODING,
                                                value =
                                                    Coding(
                                                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                                        code = Code(value = "abc"),
                                                    ),
                                            ),
                                    ),
                                    Extension(
                                        url = Uri("some-extension-url-3"),
                                    ),
                                ),
                        ),
                    ),
            )
        val expectedExtension =
            listOf(
                Extension(
                    url = Uri("some-extension-url"),
                    extension =
                        listOf(
                            Extension(
                                url = Uri("some-extension-url-2"),
                                value =
                                    DynamicValue(
                                        type = DynamicValueType.CODING,
                                        value =
                                            Coding(
                                                system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                                code = Code(value = "abc"),
                                            ),
                                    ),
                            ),
                        ),
                ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension, expectedExtension)
    }

    @Test
    fun `normalize extension with ronin url and value`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                    ),
            )
        val expectedExtension =
            listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODING,
                            value =
                                Coding(
                                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                    code = Code(value = "abc"),
                                ),
                        ),
                ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension, expectedExtension)
    }

    @Test
    fun `normalize extension within resource that has ronin url and value`() {
        val patient =
            Patient(
                id = Id("12345"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                    ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension, patient)
    }

    @Test
    fun `normalize extension within resource that has ronin url`() {
        val patient =
            Patient(
                id = Id("12345"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                        ),
                    ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension, patient)
    }

    @Test
    fun `normalize extension within resource without value is dropped`() {
        val requestGroup =
            RequestGroup(
                id = Id("1234"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("this-should-be-removed-1"),
                        ),
                    ),
                status = Code("active"),
                intent = Code("order"),
                basedOn =
                    listOf(
                        Reference(
                            type = Uri("Patient"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("this-should-be-removed-2"),
                                    ),
                                ),
                        ),
                    ),
            )

        val expectedRequestGroup =
            RequestGroup(
                id = Id("1234"),
                status = Code("active"),
                intent = Code("order"),
                basedOn =
                    listOf(
                        Reference(
                            type = Uri("Patient"),
                            extension = emptyList(),
                        ),
                    ),
            )

        val normalizedExtension = normalizer.normalize(requestGroup, tenant)
        assertEquals(expectedRequestGroup, normalizedExtension)
    }

    @Test
    fun `normalize extension within multiple urls and values`() {
        val requestGroup =
            RequestGroup(
                id = Id("1234"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("this-should-be-removed"),
                        ),
                        Extension(
                            url = Uri("this-should-be-kept"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                        Extension(
                            url = Uri("this-should-be-kept"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            code = Code(value = "def"),
                                        ),
                                ),
                        ),
                        Extension(
                            url = Uri("this-should-be-removed"),
                        ),
                    ),
                status = Code("active"),
                intent = Code("order"),
                basedOn =
                    listOf(
                        Reference(
                            type = Uri("Patient"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("this-is-an-extension-url-should-be-removed"),
                                    ),
                                ),
                        ),
                    ),
            )

        val normalized = normalizer.normalize(requestGroup, tenant)
        assertEquals(normalized.extension.size, 2)
        assertEquals(normalized.basedOn[0].extension.size, 0)
    }

    @Test
    fun `normalize extension within multiple urls and values - keeps basedOn`() {
        val requestGroup =
            RequestGroup(
                id = Id("1234"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("this-should-be-removed"),
                        ),
                        Extension(
                            url = Uri("this-should-be-kept"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                        Extension(
                            url = Uri("this-should-be-kept"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            code = Code(value = "def"),
                                        ),
                                ),
                        ),
                        Extension(
                            url = Uri("this-should-be-removed"),
                        ),
                    ),
                status = Code("active"),
                intent = Code("order"),
                basedOn =
                    listOf(
                        Reference(
                            type = Uri("Patient"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("this-is-an-extension-url-should-be-removed"),
                                    ),
                                    Extension(
                                        url = Uri("this-should-be-kept"),
                                        value =
                                            DynamicValue(
                                                type = DynamicValueType.CODING,
                                                value =
                                                    Coding(
                                                        code = Code(value = "def"),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val normalized = normalizer.normalize(requestGroup, tenant)
        assertEquals(normalized.extension.size, 2)
        assertEquals(normalized.basedOn[0].extension.size, 1)
    }

    @Test
    fun `normalize maintains contained resources`() {
        val medication =
            Medication(
                id = Id("67890"),
                code = CodeableConcept(text = FHIRString("Medication")),
            )
        val medicationRequest =
            MedicationRequest(
                id = Id("12345"),
                contained = listOf(medication),
                intent = MedicationRequestIntent.ORDER.asCode(),
                status = MedicationRequestStatus.ACTIVE.asCode(),
                medication = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = FHIRString("#67890"))),
                subject = Reference(reference = FHIRString("Patient/13579")),
            )
        val normalized = normalizer.normalize(medicationRequest, tenant)
        assertEquals(medicationRequest, normalized)
    }

    @Test
    fun `normalize maintains telecom`() {
        val patient =
            Patient(
                id = Id("12345"),
                telecom =
                    listOf(
                        ContactPoint(
                            system = ContactPointSystem.EMAIL.asCode(),
                            value = "bob@bobster.com".asFHIR(),
                        ),
                        ContactPoint(
                            system = ContactPointSystem.PHONE.asCode(),
                            value = "5551234567".asFHIR(),
                        ),
                    ),
            )
        val normalized = normalizer.normalize(patient, tenant)
        assertEquals(patient, normalized)
    }
}
