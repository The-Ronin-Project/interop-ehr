package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Duration
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.resource.EncounterClassHistory
import com.projectronin.interop.fhir.r4.resource.EncounterDiagnosis
import com.projectronin.interop.fhir.r4.resource.EncounterHospitalization
import com.projectronin.interop.fhir.r4.resource.EncounterLocation
import com.projectronin.interop.fhir.r4.resource.EncounterParticipant
import com.projectronin.interop.fhir.r4.resource.EncounterStatusHistory
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.EncounterLocationStatus
import com.projectronin.interop.fhir.r4.valueset.EncounterStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.ronin.util.localizeReferenceTest
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninEncounterTest {
    // using to double-check transformation for reference
    private val mockReference = Reference(
        reference = "Patient/f001".asFHIR(),
        display = "P. van de Heuvel".asFHIR()
    )
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val encounterCoding = Coding(
        system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
        code = Code("AMB"),
        display = "ambulatory".asFHIR()
    )
    private val encounterClassExtension = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterClass"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = encounterCoding
        )
    )
    private val roninEncounter = RoninEncounter(normalizer, localizer)

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninEncounter.qualifies(
                Encounter(
                    status = EncounterStatus.CANCELLED.asCode(),
                    `class` = Coding(code = Code("OBSENC"))
                )
            )
        )
    }

    @Test
    fun `validate - checks ronin identifiers - fails if required attributes missing`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(encounterClassExtension),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )

        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Encounter.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Encounter.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Encounter.identifier",
            exception.message
        )
    }

    @Test
    fun `validate - checks R4 profile - fails if required attributes missing`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(encounterClassExtension),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = null,
            `class` = null,
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )

        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: class is a required element @ Encounter.class\n" +
                "ERROR REQ_FIELD: status is a required element @ Encounter.status",
            exception.message
        )
    }

    @Test
    fun `validate - checks R4 profile - fails if status does not use required valueset`() {
        val encounter = Encounter(
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(encounterClassExtension),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = Code("x"),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )

        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'x' is outside of required value set @ Encounter.status",
            exception.message
        )
    }

    @Test
    fun `validate - checks USCORE profile - fails if subject null`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(encounterClassExtension),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code")))))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ Encounter.subject",
            exception.message
        )
    }

    @Test
    fun `validate - checks USCORE profile - fails if type empty list`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(encounterClassExtension),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            type = emptyList()
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: type is a required element @ Encounter.type",
            exception.message
        )
    }

    @Test
    fun `validate - checks USCORE profile - fails if system or value not populated on identifier`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(encounterClassExtension),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
                ),
                Identifier(system = Uri("http://system.without.value.org")),
                Identifier(value = "identifierWithoutSystem".asFHIR())
            ),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )

        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: value is a required element @ Encounter.identifier[3].value\n" +
                "ERROR REQ_FIELD: system is a required element @ Encounter.identifier[4].system",
            exception.message
        )
    }

    @Test
    fun `validate fails with subject but no type`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(encounterClassExtension),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(reference = "Patient/1234".asFHIR())

        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ Encounter.subject.type",
            exception.message
        )
    }

    @Test
    fun `validate checks meta`() {
        val encounter = Encounter(
            id = Id("12345"),
            extension = listOf(encounterClassExtension),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )

        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Encounter.meta",
            exception.message
        )
    }

    @Test
    fun `validate - succeeds`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(encounterClassExtension),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )

        )

        roninEncounter.validate(encounter).alertIfErrors()
    }

    @Test
    fun `transforms encounter with all attributes`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/encounter.html")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(Location(id = Id("67890"))),
            extension = listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id".asFHIR(), system = Uri("urn:oid:1:2:3"))),
            status = EncounterStatus.CANCELLED.asCode(),
            statusHistory = listOf(
                EncounterStatusHistory(
                    status = EncounterStatus.PLANNED.asCode(),
                    period = Period(
                        start = DateTime(value = "2021-11-16"),
                        end = DateTime(value = "2021-11-17T08:00:00Z")
                    )
                ),
                EncounterStatusHistory(
                    status = EncounterStatus.ARRIVED.asCode(),
                    period = Period(
                        start = DateTime(value = "2021-11-17T08:00:00Z"),
                        end = DateTime(value = "2021-11-17T09:00:00Z")
                    )
                ),
                EncounterStatusHistory(
                    status = EncounterStatus.IN_PROGRESS.asCode(),
                    period = Period(
                        start = DateTime(value = "2021-11-17T09:00:00Z"),
                        end = DateTime(value = "2021-11-17T10:00:00Z")
                    )
                ),
                EncounterStatusHistory(
                    status = EncounterStatus.FINISHED.asCode(),
                    period = Period(
                        start = DateTime(value = "2021-11-17T10:00:00Z")
                    )
                )
            ),
            `class` = Coding(
                system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                code = Code("AMB"),
                display = "ambulatory".asFHIR()
            ),
            classHistory = listOf(
                EncounterClassHistory(
                    `class` = Code("AMB"),
                    period = Period(
                        start = DateTime(value = "2021-11-16")
                    )
                )
            ),
            type = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("270427003"),
                            display = "Patient-initiated encounter".asFHIR()
                        )
                    )
                )
            ),
            serviceType = null,
            priority = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.SNOMED_CT.uri,
                        code = Code("103391001"),
                        display = "Non-urgent ear, nose and throat admission".asFHIR()
                    )
                )
            ),
            subject = localizeReferenceTest(mockReference), // check that it transforms
            episodeOfCare = emptyList(),
            basedOn = emptyList(),
            participant = listOf(
                EncounterParticipant(
                    individual = Reference(
                        reference = "Practitioner/f001".asFHIR(),
                        display = "E.M. van den Broek".asFHIR()
                    )
                )
            ),
            appointment = emptyList(),
            period = null,
            length = Duration(
                value = Decimal(90.0),
                unit = "min".asFHIR(),
                system = CodeSystem.UCUM.uri,
                code = Code("min")
            ),
            reasonCode = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("18099001"),
                            display = "Retropharyngeal abscess".asFHIR()
                        )
                    )
                )
            ),
            reasonReference = listOf(
                Reference(
                    reference = "Condition/f001".asFHIR(),
                    display = "Test Condition".asFHIR()
                )
            ),
            diagnosis = listOf(
                EncounterDiagnosis(
                    condition = Reference(reference = "Condition/stroke".asFHIR()),
                    use = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                code = Code("AD"),
                                display = "Admission diagnosis".asFHIR()
                            )
                        )
                    ),
                    rank = PositiveInt(1)
                ),
                EncounterDiagnosis(
                    condition = Reference(reference = "Condition/f201".asFHIR()),
                    use = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                code = Code("DD"),
                                display = "Discharge diagnosis".asFHIR()
                            )
                        )
                    )
                )
            ),
            account = listOf(Reference(reference = "Account/f001".asFHIR())),
            hospitalization = EncounterHospitalization(
                preAdmissionIdentifier = Identifier(
                    use = Code("official"),
                    system = Uri("http://www.bmc.nl/zorgportal/identifiers/pre-admissions"),
                    value = "93042".asFHIR()
                ),
                origin = null,
                admitSource = CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("305956004"),
                            display = "Referral by physician".asFHIR()
                        )
                    )
                ),
                reAdmission = null,
                dietPreference = listOf(
                    CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("https://www.hl7.org/fhir/R4/valueset-encounter-diet.html"),
                                code = Code("vegetarian"),
                                display = "vegetarian".asFHIR()
                            )
                        )
                    ),
                    CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("https://www.hl7.org/fhir/R4/valueset-encounter-diet.html"),
                                code = Code("kosher"),
                                display = "kosher".asFHIR()
                            )
                        )
                    )
                ),
                specialCourtesy = emptyList(),
                specialArrangement = emptyList(),
                destination = Reference(reference = "Location/place".asFHIR()),
                dischargeDisposition = CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("306689006"),
                            display = "Discharge to home".asFHIR()
                        )
                    )
                )
            ),
            location = listOf(
                EncounterLocation(
                    location = Reference(reference = "Location/f001".asFHIR()),
                    status = EncounterLocationStatus.RESERVED.asCode(),
                    physicalType = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://terminology.hl7.org/CodeSystem/location-physical-type"),
                                code = Code("area"),
                                display = "Area".asFHIR()
                            )
                        )
                    )
                )
            ),
            serviceProvider = Reference(
                reference = "Organization/f001".asFHIR(),
                display = "Community Hospital".asFHIR()
            ),
            partOf = Reference(reference = "Encounter/super".asFHIR())
        )

        val (transformResponse, validation) = roninEncounter.transform(encounter, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Encounter", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained
        )
        // TODO: Re-add type and class extensions once outstanding questions resolved
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                ),
                encounterClassExtension
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
                Identifier(system = Uri("urn:oid:1:2:3"), value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            transformed.identifier
        )
        assertEquals(EncounterStatus.CANCELLED.asCode(), transformed.status)
        assertEquals(
            listOf(
                EncounterStatusHistory(
                    status = EncounterStatus.PLANNED.asCode(),
                    period = Period(
                        start = DateTime(value = "2021-11-16"),
                        end = DateTime(value = "2021-11-17T08:00:00Z")
                    )
                ),
                EncounterStatusHistory(
                    status = EncounterStatus.ARRIVED.asCode(),
                    period = Period(
                        start = DateTime(value = "2021-11-17T08:00:00Z"),
                        end = DateTime(value = "2021-11-17T09:00:00Z")
                    )
                ),
                EncounterStatusHistory(
                    status = EncounterStatus.IN_PROGRESS.asCode(),
                    period = Period(
                        start = DateTime(value = "2021-11-17T09:00:00Z"),
                        end = DateTime(value = "2021-11-17T10:00:00Z")
                    )
                ),
                EncounterStatusHistory(
                    status = EncounterStatus.FINISHED.asCode(),
                    period = Period(
                        start = DateTime(value = "2021-11-17T10:00:00Z")
                    )
                )
            ),
            transformed.statusHistory
        )
        assertEquals(
            Coding(
                system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                code = Code("AMB"),
                display = "ambulatory".asFHIR()
            ),
            transformed.`class`
        )
        assertEquals(
            listOf(
                EncounterClassHistory(
                    `class` = Code("AMB"),
                    period = Period(
                        start = DateTime(value = "2021-11-16")
                    )
                )
            ),
            transformed.classHistory
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("270427003"),
                            display = "Patient-initiated encounter".asFHIR()
                        )
                    )
                )
            ),
            transformed.type
        )
        assertNull(transformed.serviceType)
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.SNOMED_CT.uri,
                        code = Code("103391001"),
                        display = "Non-urgent ear, nose and throat admission".asFHIR()
                    )
                )
            ),
            transformed.priority
        )
        assertEquals(
            Reference(
                reference = "Patient/test-f001".asFHIR(),
                display = "P. van de Heuvel".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(listOf<Reference>(), transformed.episodeOfCare)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(
            listOf(
                EncounterParticipant(
                    individual = Reference(
                        reference = "Practitioner/f001".asFHIR(),
                        display = "E.M. van den Broek".asFHIR()
                    )
                )
            ),
            transformed.participant
        )
        assertEquals(listOf<Reference>(), transformed.appointment)
        assertNull(transformed.period)
        assertEquals(
            Duration(
                value = Decimal(90.0),
                unit = "min".asFHIR(),
                system = CodeSystem.UCUM.uri,
                code = Code("min")
            ),
            transformed.length
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("18099001"),
                            display = "Retropharyngeal abscess".asFHIR()
                        )
                    )
                )
            ),
            transformed.reasonCode
        )
        assertEquals(
            listOf(
                Reference(
                    reference = "Condition/f001".asFHIR(),
                    display = "Test Condition".asFHIR()
                )
            ),
            transformed.reasonReference
        )
        assertEquals(
            listOf(
                EncounterDiagnosis(
                    condition = Reference(reference = "Condition/stroke".asFHIR()),
                    use = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                code = Code("AD"),
                                display = "Admission diagnosis".asFHIR()
                            )
                        )
                    ),
                    rank = PositiveInt(1)
                ),
                EncounterDiagnosis(
                    condition = Reference(reference = "Condition/f201".asFHIR()),
                    use = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                code = Code("DD"),
                                display = "Discharge diagnosis".asFHIR()
                            )
                        )
                    )
                )
            ),
            transformed.diagnosis
        )
        assertEquals(listOf(Reference(reference = "Account/f001".asFHIR())), transformed.account)
        assertEquals(
            EncounterHospitalization(
                preAdmissionIdentifier = Identifier(
                    use = Code("official"),
                    system = Uri("http://www.bmc.nl/zorgportal/identifiers/pre-admissions"),
                    value = "93042".asFHIR()
                ),
                origin = null,
                admitSource = CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("305956004"),
                            display = "Referral by physician".asFHIR()
                        )
                    )
                ),
                reAdmission = null,
                dietPreference = listOf(
                    CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("https://www.hl7.org/fhir/R4/valueset-encounter-diet.html"),
                                code = Code("vegetarian"),
                                display = "vegetarian".asFHIR()
                            )
                        )
                    ),
                    CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("https://www.hl7.org/fhir/R4/valueset-encounter-diet.html"),
                                code = Code("kosher"),
                                display = "kosher".asFHIR()
                            )
                        )
                    )
                ),
                specialCourtesy = emptyList(),
                specialArrangement = emptyList(),
                destination = Reference(reference = "Location/place".asFHIR()),
                dischargeDisposition = CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("306689006"),
                            display = "Discharge to home".asFHIR()
                        )
                    )
                )
            ),
            transformed.hospitalization
        )
        assertEquals(
            listOf(
                EncounterLocation(
                    location = Reference(reference = "Location/f001".asFHIR()),
                    status = EncounterLocationStatus.RESERVED.asCode(),
                    physicalType = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://terminology.hl7.org/CodeSystem/location-physical-type"),
                                code = Code("area"),
                                display = "Area".asFHIR()
                            )
                        )
                    )
                )
            ),
            transformed.location
        )
        assertEquals(
            Reference(
                reference = "Organization/f001".asFHIR(),
                display = "Community Hospital".asFHIR()
            ),
            transformed.serviceProvider
        )
        assertEquals(Reference(reference = "Encounter/super".asFHIR()), transformed.partOf)
    }

    @Test
    fun `transform encounter adds class extension with only required attributes`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(source = Uri("fake-source-fake-url")),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val (transformResponse, validation) = roninEncounter.transform(encounter, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterClass"),
                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("OBSENC")))
                )
            ),
            transformed.extension
        )
    }

    @Test
    fun `transform encounter with only required attributes`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val (transformResponse, validation) = roninEncounter.transform(encounter, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Encounter", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            transformed.identifier
        )
        assertEquals(EncounterStatus.CANCELLED.asCode(), transformed.status)
        assertEquals(listOf<CodeableConcept>(), transformed.statusHistory)
        assertEquals(Coding(code = Code("OBSENC")), transformed.`class`)
        assertEquals(listOf<CodeableConcept>(), transformed.classHistory)
        assertEquals(listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))), transformed.type)
        assertNull(transformed.serviceType)
        assertNull(transformed.priority)
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(listOf<Reference>(), transformed.episodeOfCare)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(listOf<EncounterParticipant>(), transformed.participant)
        assertEquals(listOf<Reference>(), transformed.appointment)
        assertNull(transformed.period)
        assertNull(transformed.length)
        assertEquals(listOf<CodeableConcept>(), transformed.reasonCode)
        assertEquals(listOf<Reference>(), transformed.reasonReference)
        assertEquals(listOf<EncounterDiagnosis>(), transformed.diagnosis)
        assertEquals(listOf<Reference>(), transformed.account)
        assertNull(transformed.hospitalization)
        assertEquals(listOf<EncounterLocation>(), transformed.location)
        assertNull(transformed.serviceProvider)
        assertNull(transformed.partOf)
    }

    @Test
    fun `transform fails for encounter with missing id`() {
        val encounter = Encounter(
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC"))
        )

        val (transformResponse, _) = roninEncounter.transform(encounter, tenant)

        assertNull(transformResponse)
    }

    @Test
    fun `validate - fails if missing required source code extension`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_ENC_001: Tenant source encounter class extension is missing or invalid @ Encounter.extension",
            exception.message
        )
    }

    @Test
    fun `validate - fails if source code extension has wrong url`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_ENC_001: Tenant source encounter class extension is missing or invalid @ Encounter.extension",
            exception.message
        )
    }

    @Test
    fun `validate - fails if source code extension has wrong datatype`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninEncounter.validate(encounter).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_ENC_001: Tenant source encounter class extension is missing or invalid @ Encounter.extension",
            exception.message
        )
    }
}
