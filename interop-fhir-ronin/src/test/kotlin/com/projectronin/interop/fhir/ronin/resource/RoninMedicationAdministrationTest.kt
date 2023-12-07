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
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.SimpleQuantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.resource.MedicationAdministrationDosage
import com.projectronin.interop.fhir.r4.resource.MedicationAdministrationPerformer
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationAdministrationValidator
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationStatementValidator
import com.projectronin.interop.fhir.r4.valueset.MedicationAdministrationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCoding
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.extractor.MedicationExtractor
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Suppress("ktlint:standard:max-line-length")
class RoninMedicationAdministrationTest {
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }
    private val conceptMapMetadata =
        ConceptMapMetadata(
            registryEntryType = "concept-map",
            conceptMapName = "test-concept-map",
            conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
            version = "1",
        )

    private val normalizer =
        mockk<Normalizer> {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
    private val localizer =
        mockk<Localizer> {
            every { localize(any(), tenant) } answers { firstArg() }
        }
    private val medicationExtractor =
        mockk<MedicationExtractor> {
            every { extractMedication(any(), any(), any()) } returns null
        }
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val roninMedicationAdministration =
        RoninMedicationAdministration(normalizer, localizer, medicationExtractor, registryClient)

    @Test
    fun `validates Ronin Identifiers`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                    ),
            )
        mockkObject(R4MedicationAdministrationValidator)
        every {
            R4MedicationAdministrationValidator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        } returns validation { }

        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ MedicationAdministration.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ MedicationAdministration.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ MedicationAdministration.identifier",
            exception.message,
        )

        unmockkObject(R4MedicationAdministrationValidator)
    }

    @Test
    fun `validate succeeds with required attributes`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                    ),
                status = Code("in-progress"),
                category =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(id = "something".asFHIR()),
                            ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication/something".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )
        roninMedicationAdministration.validate(medAdmin).alertIfErrors()
    }

    @Test
    fun `validate passed with more than one extension`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                        Extension(
                            url = Uri("something"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                        Extension(
                            url = Uri(null),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.REFERENCE,
                                    value = Code(null),
                                ),
                        ),
                    ),
                status = Code("in-progress"),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication/something".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = emptyList()),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )
        roninMedicationAdministration.validate(medAdmin).alertIfErrors()
    }

    @Test
    fun `validation fails with more than one category`() {
        val medAdmin =
            MedicationAdministration(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
                contained = listOf(Location(id = Id("67890"))),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                    ),
                category =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(id = "something".asFHIR()),
                                Coding(id = "something-else".asFHIR()),
                            ),
                    ),
                status = Code("in-progress"),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MEDADMIN_001: More than one category cannot be present if category is not null @ MedicationAdministration.category",
            exception.message,
        )
    }

    @Test
    fun `validation fails with no medication datatype extension url`() {
        val medAdmin =
            MedicationAdministration(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
                contained = listOf(Location(id = Id("67890"))),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                category =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(id = "something".asFHIR()),
                            ),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(value = "incorrect-url"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                    ),
                status = Code("in-progress"),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MEDDTEXT_001: Extension must contain original Medication Datatype @ MedicationAdministration.extension",
            exception.message,
        )
    }

    @Test
    fun `validates R4 profile`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                    ),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication/something".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
            )

        mockkObject(R4MedicationAdministrationValidator)
        every {
            R4MedicationAdministrationValidator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        } returns
            validation {
                checkNotNull(
                    null,
                    RequiredFieldError(MedicationAdministration::status),
                    LocationContext(MedicationAdministration::class),
                )
                checkNotNull(
                    null,
                    RequiredFieldError(MedicationAdministration::effective),
                    LocationContext(MedicationAdministration::class),
                )
                checkNotNull(
                    null,
                    RequiredFieldError(MedicationAdministration::medication),
                    LocationContext(MedicationAdministration::class),
                )
                checkNotNull(
                    null,
                    RequiredFieldError(MedicationAdministration::subject),
                    LocationContext(MedicationAdministration::class),
                )
            }

        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: status is a required element @ MedicationAdministration.status\n" +
                "ERROR REQ_FIELD: effective is a required element @ MedicationAdministration.effective\n" +
                "ERROR REQ_FIELD: medication is a required element @ MedicationAdministration.medication\n" +
                "ERROR REQ_FIELD: subject is a required element @ MedicationAdministration.subject",
            exception.message,
        )

        unmockkObject(R4MedicationStatementValidator)
    }

    @Test
    fun `transform succeeds with all required attributes`() {
        val medAdmin =
            MedicationAdministration(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
                status = Code("in-progress"),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication/something".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "MedicationAdministration.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
                    code = Code(value = "in-progress"),
                ),
                MedicationAdministrationStatus::class,
                RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value,
                medAdmin,
            )
        } returns
            ConceptMapCoding(
                statusCoding("in-progress"),
                statusCodingExtension("in-progress"),
                listOf(conceptMapMetadata),
            )

        val (transformResponse, validation) = roninMedicationAdministration.transform(medAdmin, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_ADMINISTRATION.value,
            transformed.meta!!.profile[0].value,
        )
        assertEquals(medAdmin.implicitRules, transformed.implicitRules)
        assertEquals(medAdmin.language, transformed.language)
        assertEquals(medAdmin.text, transformed.text)
        assertEquals(
            listOf(
                statusCodingExtension("in-progress"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODE,
                            value = Code("literal reference"),
                        ),
                ),
            ),
            transformed.extension,
        )
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR(),
                ),
            ),
            transformed.identifier,
        )
        assertEquals(medAdmin.instantiates, transformed.instantiates)
        assertEquals(medAdmin.partOf, transformed.partOf)
        assertEquals(medAdmin.status, transformed.status)
        assertEquals(medAdmin.statusReason, transformed.statusReason)
        assertEquals(medAdmin.category, transformed.category)
        assertEquals(medAdmin.medication, transformed.medication)
        assertEquals(medAdmin.subject, transformed.subject)
        assertEquals(medAdmin.context, transformed.context)
        assertEquals(medAdmin.supportingInformation, transformed.supportingInformation)
        assertEquals(medAdmin.effective, transformed.effective)
        assertEquals(medAdmin.performer, transformed.performer)
        assertEquals(medAdmin.note, transformed.note)
        assertEquals(medAdmin.dosage, transformed.dosage)
        assertEquals(medAdmin.eventHistory, transformed.eventHistory)
    }

    @Test
    fun `transform succeeds and returns correct extension with all required attributes`() {
        val medAdmin =
            MedicationAdministration(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
                status = Code("in-progress"),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication/something".asFHIR(),
                                identifier = Identifier(id = "12345678".asFHIR()),
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "MedicationAdministration.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
                    code = Code(value = "in-progress"),
                ),
                MedicationAdministrationStatus::class,
                RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value,
                medAdmin,
            )
        } returns
            ConceptMapCoding(
                statusCoding("in-progress"),
                statusCodingExtension("in-progress"),
                listOf(conceptMapMetadata),
            )

        val (transformResponse, validation) = roninMedicationAdministration.transform(medAdmin, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_ADMINISTRATION.value,
            transformed.meta!!.profile[0].value,
        )
        assertEquals(medAdmin.implicitRules, transformed.implicitRules)
        assertEquals(medAdmin.language, transformed.language)
        assertEquals(medAdmin.text, transformed.text)
        assertEquals(
            listOf(
                statusCodingExtension("in-progress"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODE,
                            value = Code("logical reference"),
                        ),
                ),
            ),
            transformed.extension,
        )
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR(),
                ),
            ),
            transformed.identifier,
        )
        assertEquals(medAdmin.instantiates, transformed.instantiates)
        assertEquals(medAdmin.partOf, transformed.partOf)
        assertEquals(medAdmin.status, transformed.status)
        assertEquals(medAdmin.statusReason, transformed.statusReason)
        assertEquals(medAdmin.category, transformed.category)
        assertEquals(medAdmin.medication, transformed.medication)
        assertEquals(medAdmin.subject, transformed.subject)
        assertEquals(medAdmin.context, transformed.context)
        assertEquals(medAdmin.supportingInformation, transformed.supportingInformation)
        assertEquals(medAdmin.effective, transformed.effective)
        assertEquals(medAdmin.performer, transformed.performer)
        assertEquals(medAdmin.note, transformed.note)
        assertEquals(medAdmin.dosage, transformed.dosage)
        assertEquals(medAdmin.eventHistory, transformed.eventHistory)
    }

    @Test
    fun `transform succeeds with all attributes`() {
        val medAdmin =
            MedicationAdministration(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
                contained = listOf(Location(id = Id("67890"))),
                identifier = listOf(Identifier(value = "id".asFHIR())),
                instantiates = listOf(Uri("something-here")),
                partOf = listOf(Reference(display = "partOf".asFHIR())),
                status = Code("in-progress"),
                statusReason = listOf(CodeableConcept(text = "statusReason".asFHIR())),
                category = CodeableConcept(coding = listOf(Coding(code = Code("code"))), text = "category".asFHIR()),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "#something".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
                context = Reference(reference = "Encounter/12345678".asFHIR(), display = "context".asFHIR()),
                supportingInformation = listOf(Reference(display = "supportingInformation".asFHIR())),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                performer =
                    listOf(
                        MedicationAdministrationPerformer(
                            actor = Reference(reference = "Patient".asFHIR()),
                            id = "12345678".asFHIR(),
                        ),
                    ),
                note = listOf(Annotation(text = Markdown("annotation"))),
                dosage =
                    MedicationAdministrationDosage(
                        rate =
                            DynamicValue(
                                type = DynamicValueType.QUANTITY,
                                SimpleQuantity(value = Decimal(1)),
                            ),
                    ),
                eventHistory = listOf(Reference(display = "eventHistory".asFHIR())),
            )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "MedicationAdministration.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
                    code = Code(value = "in-progress"),
                ),
                MedicationAdministrationStatus::class,
                RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value,
                medAdmin,
            )
        } returns
            ConceptMapCoding(
                statusCoding("in-progress"),
                statusCodingExtension("in-progress"),
                listOf(conceptMapMetadata),
            )

        val (transformResponse, validation) = roninMedicationAdministration.transform(medAdmin, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_ADMINISTRATION.value,
            transformed.meta!!.profile[0].value,
        )
        assertEquals(medAdmin.implicitRules, transformed.implicitRules)
        assertEquals(medAdmin.language, transformed.language)
        assertEquals(medAdmin.text, transformed.text)
        assertEquals(transformed.contained, listOf(Location(id = Id("67890"))))
        assertEquals(
            listOf(
                statusCodingExtension("in-progress"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODE,
                            value = Code("contained reference"),
                        ),
                ),
            ),
            transformed.extension,
        )
        assertEquals(4, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR(),
                ),
            ),
            transformed.identifier,
        )
        assertEquals(transformed.instantiates, listOf(Uri("something-here")))
        assertEquals(transformed.partOf, listOf(Reference(display = "partOf".asFHIR())))
        assertEquals(transformed.status, Code("in-progress"))
        assertEquals(transformed.statusReason, listOf(CodeableConcept(text = "statusReason".asFHIR())))
        assertEquals(
            transformed.category,
            CodeableConcept(coding = listOf(Coding(code = Code("code"))), text = "category".asFHIR()),
        )
        assertEquals(
            transformed.medication,
            DynamicValue(
                DynamicValueType.REFERENCE,
                value =
                    Reference(
                        reference = "#something".asFHIR(),
                        type = Uri("Medication", extension = dataAuthorityExtension),
                    ),
            ),
        )
        assertEquals(
            transformed.subject,
            Reference(
                reference = "Patient/123".asFHIR(),
                type =
                    Uri(
                        "Patient",
                        extension = dataAuthorityExtension,
                    ),
            ),
        )
        assertEquals(
            transformed.context,
            Reference(reference = "Encounter/12345678".asFHIR(), display = "context".asFHIR()),
        )
        assertEquals(transformed.supportingInformation, listOf(Reference(display = "supportingInformation".asFHIR())))
        assertEquals(transformed.effective, DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"))
        assertEquals(
            transformed.performer,
            listOf(
                MedicationAdministrationPerformer(
                    actor = Reference(reference = "Patient".asFHIR()),
                    id = "12345678".asFHIR(),
                ),
            ),
        )
        assertEquals(transformed.note, listOf(Annotation(text = Markdown("annotation"))))
        assertEquals(
            transformed.dosage,
            MedicationAdministrationDosage(
                rate =
                    DynamicValue(
                        type = DynamicValueType.QUANTITY,
                        SimpleQuantity(value = Decimal(1)),
                    ),
            ),
        )
        assertEquals(transformed.eventHistory, listOf(Reference(display = "eventHistory".asFHIR())))
    }

    @Test
    fun `validate fails with wrong medication datatype extension value`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                identifier =
                    listOf(
                        Identifier(value = "id".asFHIR()),
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("blah"),
                                ),
                        ),
                    ),
                status = MedicationAdministrationStatus.IN_PROGRESS.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/test-1234")),
                    ),
                subject =
                    Reference(
                        display = "subject".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MEDDTEXT_002: Medication Datatype extension value is invalid @ MedicationAdministration.extension",
            exception.message,
        )
    }

    @Test
    fun `validate fails with wrong medication datatype extension type`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                identifier =
                    listOf(
                        Identifier(value = "id".asFHIR()),
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.STRING,
                                    value = Code("codeable concept"),
                                ),
                        ),
                        Extension(
                            url = Uri(value = null),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.STRING,
                                    value = Code(null),
                                ),
                        ),
                    ),
                status = MedicationAdministrationStatus.IN_PROGRESS.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/test-1234")),
                    ),
                subject =
                    Reference(
                        display = "subject".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MEDDTEXT_003: Medication Datatype extension type is invalid @ MedicationAdministration.extension",
            exception.message,
        )
    }

    @Test
    fun `transform fails with missing attributes`() {
        val medAdmin = MedicationAdministration()
        val (transformResponse, _) = roninMedicationAdministration.transform(medAdmin, tenant)
        assertNull(transformResponse)
    }

    @Test
    fun `validate fails missing subject and status`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                identifier =
                    listOf(
                        Identifier(value = "id".asFHIR()),
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("codeable concept"),
                                ),
                        ),
                    ),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/test-1234")),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: status is a required element @ MedicationAdministration.status\n" +
                "ERROR REQ_FIELD: subject is a required element @ MedicationAdministration.subject",
            exception.message,
        )
    }

    @Test
    fun `validate fails missing medication`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                identifier =
                    listOf(
                        Identifier(value = "id".asFHIR()),
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        statusCodingExtension("mapped"),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("codeable concept"),
                                ),
                        ),
                    ),
                status = MedicationAdministrationStatus.IN_PROGRESS.asCode(),
                subject =
                    Reference(
                        display = "subject".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: medication is a required element @ MedicationAdministration.medication",
            exception.message,
        )
    }

    @Test
    fun `transform succeeds with extracted medications`() {
        val containedMedication =
            Medication(
                id = Id("67890"),
                code = CodeableConcept(text = "medication".asFHIR()),
            )
        val originalMedicationDynamicValue =
            DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "#67890".asFHIR()))
        val medAdmin =
            MedicationAdministration(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                        source = Uri("source"),
                    ),
                contained = listOf(containedMedication),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
                status = Code("in-progress"),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication = originalMedicationDynamicValue,
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "MedicationAdministration.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
                    code = Code(value = "in-progress"),
                ),
                MedicationAdministrationStatus::class,
                RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value,
                medAdmin,
            )
        } returns
            ConceptMapCoding(
                statusCoding("in-progress"),
                statusCodingExtension("in-progress"),
                listOf(conceptMapMetadata),
            )

        val updatedMedicationDynamicValue =
            DynamicValue(
                DynamicValueType.REFERENCE,
                Reference(reference = "Medication/contained-12345-67890".asFHIR()),
            )
        val theExtractedMedication =
            Medication(
                id = Id("contained-12345-67890"),
                code = CodeableConcept(text = "medication".asFHIR()),
            )
        every {
            medicationExtractor.extractMedication(
                originalMedicationDynamicValue,
                listOf(containedMedication),
                any(),
            )
        } returns
            mockk {
                every { updatedMedication } returns updatedMedicationDynamicValue
                every { updatedContained } returns emptyList()
                every { extractedMedication } returns theExtractedMedication
            }

        val (transformResponse, validation) = roninMedicationAdministration.transform(medAdmin, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(listOf(theExtractedMedication), transformResponse.embeddedResources)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_ADMINISTRATION.value,
            transformed.meta!!.profile[0].value,
        )
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(medAdmin.implicitRules, transformed.implicitRules)
        assertEquals(medAdmin.language, transformed.language)
        assertEquals(medAdmin.text, transformed.text)
        assertEquals(
            listOf(
                statusCodingExtension("in-progress"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODE,
                            value = Code("contained reference"),
                        ),
                ),
            ),
            transformed.extension,
        )
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR(),
                ),
            ),
            transformed.identifier,
        )
        assertEquals(medAdmin.instantiates, transformed.instantiates)
        assertEquals(medAdmin.partOf, transformed.partOf)
        assertEquals(medAdmin.status, transformed.status)
        assertEquals(medAdmin.statusReason, transformed.statusReason)
        assertEquals(medAdmin.category, transformed.category)
        assertEquals(updatedMedicationDynamicValue, transformed.medication)
        assertEquals(medAdmin.subject, transformed.subject)
        assertEquals(medAdmin.context, transformed.context)
        assertEquals(medAdmin.supportingInformation, transformed.supportingInformation)
        assertEquals(medAdmin.effective, transformed.effective)
        assertEquals(medAdmin.performer, transformed.performer)
        assertEquals(medAdmin.note, transformed.note)
        assertEquals(medAdmin.dosage, transformed.dosage)
        assertEquals(medAdmin.eventHistory, transformed.eventHistory)
    }

    @Test
    fun `transform fails if no status value`() {
        val medAdmin =
            MedicationAdministration(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
                status = null,
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication/something".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )

        val (transformResponse, validation) = roninMedicationAdministration.transform(medAdmin, tenant)
        assertNull(transformResponse)

        val exception =
            assertThrows<IllegalArgumentException> {
                validation.alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MEDADMIN_003: Tenant source medication administration status extension is missing or invalid @ MedicationAdministration.extension\n" +
                "ERROR REQ_FIELD: status is a required element @ MedicationAdministration.status",
            exception.message,
        )
    }

    @Test
    fun `transform fails if status cannot be mapped`() {
        val medAdmin =
            MedicationAdministration(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
                status = Code("unmapped"),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication/something".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "MedicationAdministration.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
                    code = Code(value = "unmapped"),
                ),
                MedicationAdministrationStatus::class,
                RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value,
                medAdmin,
            )
        } returns null

        val (transformResponse, validation) = roninMedicationAdministration.transform(medAdmin, tenant)
        assertNull(transformResponse)

        val exception =
            assertThrows<IllegalArgumentException> {
                validation.alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'unmapped' has no target defined in http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus @ MedicationAdministration.status\n" +
                "ERROR RONIN_MEDADMIN_003: Tenant source medication administration status extension is missing or invalid @ MedicationAdministration.extension\n" +
                "ERROR INV_VALUE_SET: 'unmapped' is outside of required value set @ MedicationAdministration.status",
            exception.message,
        )
    }

    @Test
    fun `validate fails for wrong URL in status source extension`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://example.org/other-extension"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code(value = "mapped"),
                                ),
                        ),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                    ),
                status = Code("in-progress"),
                category =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(id = "something".asFHIR()),
                            ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication/something".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MEDADMIN_003: Tenant source medication administration status extension is missing or invalid @ MedicationAdministration.extension",
            exception.message,
        )
    }

    @Test
    fun `validate fails for wrong data type in status source extension`() {
        val medAdmin =
            MedicationAdministration(
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                        source = Uri("source"),
                    ),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                extension =
                    listOf(
                        Extension(
                            url =
                                Uri(
                                    "http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceMedicationAdministrationStatus",
                                ),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.BOOLEAN,
                                    value = FHIRBoolean.FALSE,
                                ),
                        ),
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                    ),
                status = Code("in-progress"),
                category =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(id = "something".asFHIR()),
                            ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
                medication =
                    DynamicValue(
                        DynamicValueType.REFERENCE,
                        value =
                            Reference(
                                reference = "Medication/something".asFHIR(),
                                identifier = null,
                                type = Uri("Medication", extension = dataAuthorityExtension),
                            ),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type =
                            Uri(
                                "Patient",
                                extension = dataAuthorityExtension,
                            ),
                    ),
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                roninMedicationAdministration.validate(medAdmin).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MEDADMIN_003: Tenant source medication administration status extension is missing or invalid @ MedicationAdministration.extension",
            exception.message,
        )
    }

    private fun statusCoding(value: String) =
        Coding(
            system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
            code = Code(value = value),
        )

    private fun statusCodingExtension(value: String) =
        Extension(
            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceMedicationAdministrationStatus"),
            value =
                DynamicValue(
                    type = DynamicValueType.CODING,
                    value =
                        Coding(
                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
                            code = Code(value = value),
                        ),
                ),
        )
}
