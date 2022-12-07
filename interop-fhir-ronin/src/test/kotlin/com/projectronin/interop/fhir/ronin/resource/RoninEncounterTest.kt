package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Duration
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.EncounterClassHistory
import com.projectronin.interop.fhir.r4.datatype.EncounterDiagnosis
import com.projectronin.interop.fhir.r4.datatype.EncounterHospitalization
import com.projectronin.interop.fhir.r4.datatype.EncounterLocation
import com.projectronin.interop.fhir.r4.datatype.EncounterParticipant
import com.projectronin.interop.fhir.r4.datatype.EncounterStatusHistory
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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.valueset.EncounterLocationStatus
import com.projectronin.interop.fhir.r4.valueset.EncounterStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
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
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `always qualifies`() {
        assertTrue(
            RoninEncounter.qualifies(
                Encounter(
                    status = EncounterStatus.CANCELLED.asCode(),
                    `class` = Coding(code = Code("OBSENC")),
                )
            )
        )
    }

    @Test
    fun `validate - checks ronin identifiers - fails if required attributes missing`() {
        val encounter = Encounter(
            id = Id("12345"),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(reference = "Patient/example".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninEncounter.validate(encounter, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Encounter.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Encounter.identifier",
            exception.message
        )
    }

    @Test
    fun `validate - checks R4 profile - fails if required attributes missing`() {
        val encounter = Encounter(
            id = Id("12345"),
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
                )
            ),
            status = null,
            `class` = null,
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(reference = "Patient/example".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninEncounter.validate(encounter, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: status is a required element @ Encounter.status\n" +
                "ERROR REQ_FIELD: class is a required element @ Encounter.class",
            exception.message
        )
    }

    @Test
    fun `validate - checks R4 profile - fails if status does not use required valueset`() {
        val encounter = Encounter(
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
                )
            ),
            status = Code("x"),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(reference = "Patient/example".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninEncounter.validate(encounter, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'x' is outside of required value set @ Encounter.status",
            exception.message
        )
    }

    @Test
    fun `validate - checks USCORE profile - fails if subject and type null`() {
        val encounter = Encounter(
            id = Id("12345"),
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
                )
            ),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninEncounter.validate(encounter, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: type is a required element @ Encounter.type\n" +
                "ERROR REQ_FIELD: subject is a required element @ Encounter.subject",
            exception.message
        )
    }

    @Test
    fun `validate - checks USCORE profile - fails if type empty list`() {
        val encounter = Encounter(
            id = Id("12345"),
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
                )
            ),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            subject = Reference(reference = "Patient/example".asFHIR()),
            type = emptyList()
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninEncounter.validate(encounter, null).alertIfErrors()
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
                Identifier(system = Uri("http://system.without.value.org")),
                Identifier(value = "identifierWithoutSystem".asFHIR())
            ),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(reference = "Patient/example".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninEncounter.validate(encounter, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: value is a required element @ Encounter.identifier[2].value\n" +
                "ERROR REQ_FIELD: system is a required element @ Encounter.identifier[3].system",
            exception.message
        )
    }

    @Test
    fun `validate - succeeds`() {
        val encounter = Encounter(
            id = Id("12345"),
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
                )
            ),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(reference = "Patient/example".asFHIR())
        )

        RoninEncounter.validate(encounter, null).alertIfErrors()
    }

    @Test
    fun `transforms encounter with all attributes`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/encounter.html"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
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
            subject = Reference(
                reference = "Patient/f001".asFHIR(),
                display = "P. van de Heuvel".asFHIR()
            ),
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

        val transformed = RoninEncounter.transform(encounter, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Encounter", transformed.resourceType)
        assertEquals(Id(value = "test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value))),
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
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
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
                    text = "Patient-initiated encounter".asFHIR(),
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
                text = "Non-urgent ear, nose and throat admission".asFHIR(),
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
                display = "P. van de Heuvel".asFHIR()
            ),
            transformed.subject
        )
        assertEquals(listOf<Reference>(), transformed.episodeOfCare)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(
            listOf(
                EncounterParticipant(
                    individual = Reference(
                        reference = "Practitioner/test-f001".asFHIR(),
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
                    text = "Retropharyngeal abscess".asFHIR(),
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
                    reference = "Condition/test-f001".asFHIR(),
                    display = "Test Condition".asFHIR()
                )
            ),
            transformed.reasonReference
        )
        assertEquals(
            listOf(
                EncounterDiagnosis(
                    condition = Reference(reference = "Condition/test-stroke".asFHIR()),
                    use = CodeableConcept(
                        text = "Admission diagnosis".asFHIR(),
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
                    condition = Reference(reference = "Condition/test-f201".asFHIR()),
                    use = CodeableConcept(
                        text = "Discharge diagnosis".asFHIR(),
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
        assertEquals(listOf(Reference(reference = "Account/test-f001".asFHIR())), transformed.account)
        assertEquals(
            EncounterHospitalization(
                preAdmissionIdentifier = Identifier(
                    use = Code("official"),
                    system = Uri("http://www.bmc.nl/zorgportal/identifiers/pre-admissions"),
                    value = "93042".asFHIR()
                ),
                origin = null,
                admitSource = CodeableConcept(
                    text = "Referral by physician".asFHIR(),
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
                        text = "vegetarian".asFHIR(),
                        coding = listOf(
                            Coding(
                                system = Uri("https://www.hl7.org/fhir/R4/valueset-encounter-diet.html"),
                                code = Code("vegetarian"),
                                display = "vegetarian".asFHIR()
                            )
                        )
                    ),
                    CodeableConcept(
                        text = "kosher".asFHIR(),
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
                destination = Reference(reference = "Location/test-place".asFHIR()),
                dischargeDisposition = CodeableConcept(
                    text = "Discharge to home".asFHIR(),
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
                    location = Reference(reference = "Location/test-f001".asFHIR()),
                    status = EncounterLocationStatus.RESERVED.asCode(),
                    physicalType = CodeableConcept(
                        text = "Area".asFHIR(),
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
                reference = "Organization/test-f001".asFHIR(),
                display = "Community Hospital".asFHIR()
            ),
            transformed.serviceProvider
        )
        assertEquals(Reference(reference = "Encounter/test-super".asFHIR()), transformed.partOf)
    }

    @Test
    fun `transform encounter with only required attributes`() {
        val encounter = Encounter(
            id = Id("12345"),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(reference = "Patient/example".asFHIR())
        )

        val transformed = RoninEncounter.transform(encounter, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Encounter", transformed.resourceType)
        assertEquals(Id(value = "test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value))),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
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
        assertEquals(Reference(reference = "Patient/test-example".asFHIR()), transformed.subject)
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
            `class` = Coding(code = Code("OBSENC")),
        )

        val transformed = RoninEncounter.transform(encounter, tenant)

        assertNull(transformed)
    }
}
