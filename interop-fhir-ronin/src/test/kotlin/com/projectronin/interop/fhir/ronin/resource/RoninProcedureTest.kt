package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
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
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.fhir.r4.valueset.EventStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCodeableConcept
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Suppress("ktlint:standard:max-line-length")
class RoninProcedureTest {
    private lateinit var registryClient: NormalizationRegistryClient
    private lateinit var normalizer: Normalizer
    private lateinit var localizer: Localizer
    private lateinit var roninProcedure: RoninProcedure

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    private val unmappedCode1 =
        CodeableConcept(
            coding =
                listOf(
                    Coding(
                        system = Uri("UnmappedCodeSystem#1"),
                        code = Code(value = "UnmappedCode#1"),
                    ),
                ),
        )

    private val mappedCode1 =
        CodeableConcept(
            coding =
                listOf(
                    Coding(
                        system = Uri("Code"),
                        code = Code(value = "normalizedCode"),
                    ),
                ),
        )

    private val conceptMapMetadata =
        ConceptMapMetadata(
            registryEntryType = "concept-map",
            conceptMapName = "test-concept-map",
            conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
            version = "1",
        )

    private fun procedureExtensionCode() =
        Extension(
            url = RoninExtension.TENANT_SOURCE_PROCEDURE_CODE.uri,
            value =
                DynamicValue(
                    type = DynamicValueType.CODEABLE_CONCEPT,
                    value =
                        Coding(
                            system = Uri("system"),
                            code = Code(value = "code"),
                        ),
                ),
        )

    @BeforeEach
    fun setup() {
        registryClient =
            mockk<NormalizationRegistryClient> {
                every {
                    getConceptMapping(
                        tenant,
                        "Procedure.code",
                        mappedCode1,
                        any<DocumentReference>(),
                    )
                } returns
                    ConceptMapCodeableConcept(
                        CodeableConcept(),
                        procedureExtensionCode(),
                        listOf(conceptMapMetadata),
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Procedure.code",
                        unmappedCode1,
                        any<DocumentReference>(),
                    )
                } returns null
            }
        normalizer =
            mockk {
                every { normalize(any(), tenant) } answers { firstArg() }
            }
        localizer =
            mockk {
                every { localize(any(), tenant) } answers { firstArg() }
            }
        roninProcedure = RoninProcedure(normalizer, localizer, registryClient)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninProcedure.qualifies(
                Procedure(
                    status = EventStatus.IN_PROGRESS.asCode(),
                    subject = Reference(reference = FHIRString("Patient")),
                ),
            ),
        )
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val procedure =
            Procedure(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PROCEDURE.value)), source = Uri("source")),
                status = EventStatus.ON_HOLD.asCode(),
                extension = listOf(procedureExtensionCode()),
                subject = Reference(reference = FHIRString("Patient/123123")),
                code = CodeableConcept(),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                roninProcedure.validate(procedure).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Procedure.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Procedure.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Procedure.identifier",
            exception.message,
        )
    }

    @Test
    fun `validate checks meta`() {
        val procedure =
            Procedure(
                id = Id("12345"),
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
                extension = listOf(procedureExtensionCode()),
                status = EventStatus.ON_HOLD.asCode(),
                subject = Reference(reference = FHIRString("Patient/123123")),
                code = CodeableConcept(),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                roninProcedure.validate(procedure).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Procedure.meta",
            exception.message,
        )
    }

    @Test
    fun `validate fails with missing extension code`() {
        val procedure =
            Procedure(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PROCEDURE.value)), source = Uri("source")),
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
                extension = emptyList(),
                status = EventStatus.ON_HOLD.asCode(),
                subject = Reference(reference = FHIRString("Patient/123123")),
                code = CodeableConcept(),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                roninProcedure.validate(procedure).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PROC_001: Tenant source procedure code extension is missing or invalid @ Procedure.extension",
            exception.message,
        )
    }

    @Test
    fun `validate fails with completed status and missing performed`() {
        val procedure =
            Procedure(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PROCEDURE.value)), source = Uri("source")),
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
                extension = listOf(procedureExtensionCode()),
                status = EventStatus.COMPLETED.asCode(),
                subject = Reference(reference = FHIRString("Patient/123123")),
                code = CodeableConcept(),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                roninProcedure.validate(procedure).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_PROC_001: Performed SHALL be present if the status is 'completed' or 'in-progress' @ Procedure.performed",
            exception.message,
        )
    }

    @Test
    fun `validate fails with missing code`() {
        val procedure =
            Procedure(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PROCEDURE.value)), source = Uri("source")),
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
                extension = listOf(procedureExtensionCode()),
                status = EventStatus.ON_HOLD.asCode(),
                subject = Reference(reference = FHIRString("Patient/123123")),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                roninProcedure.validate(procedure).alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_PROC_002: Procedure code is missing or invalid @ Procedure.code",
            exception.message,
        )
    }

    @Test
    fun `validate outputs warning with invalid subject`() {
        val procedure =
            Procedure(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PROCEDURE.value)), source = Uri("source")),
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
                extension = listOf(procedureExtensionCode()),
                status = EventStatus.ON_HOLD.asCode(),
                subject = Reference(reference = FHIRString("Appointment/123123")),
                code = CodeableConcept(),
            )

        val validation = roninProcedure.validate(procedure)

        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Patient @ Procedure.reference",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate does not output warning with valid subject`() {
        val procedure =
            Procedure(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PROCEDURE.value)), source = Uri("source")),
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
                extension = listOf(procedureExtensionCode()),
                status = EventStatus.ON_HOLD.asCode(),
                subject = Reference(reference = FHIRString("Patient/123123")),
                code = CodeableConcept(),
            )

        val validation = roninProcedure.validate(procedure)

        assertTrue(
            validation.issues().isEmpty(),
        )
    }

    @Test
    fun `validate succeeds`() {
        val procedure =
            Procedure(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PROCEDURE.value)), source = Uri("source")),
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
                extension = listOf(procedureExtensionCode()),
                status = EventStatus.ON_HOLD.asCode(),
                subject = Reference(reference = FHIRString("Patient/123123")),
                code = CodeableConcept(),
            )

        roninProcedure.validate(procedure).alertIfErrors()
    }

    @Test
    fun `transform succeeds`() {
        val procedure =
            Procedure(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PROCEDURE.value)), source = Uri("source")),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_MRN,
                            system = CodeSystem.RONIN_MRN.uri,
                            value = "6789".asFHIR(),
                        ),
                    ),
                status = EventStatus.ON_HOLD.asCode(),
                subject = Reference(reference = FHIRString("Patient/123123")),
                code = mappedCode1,
                category = CodeableConcept(),
            )

        val (transformed, validation) = roninProcedure.transform(procedure, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(Id("12345"), transformed.resource.id)
        assertEquals(4, transformed.resource.identifier.size)
        assertEquals(
            Meta(
                source = Uri("source"),
                profile = listOf(Canonical(RoninProfile.PROCEDURE.value)),
            ),
            transformed.resource.meta,
        )
        assertEquals(
            listOf(procedureExtensionCode()),
            transformed.resource.extension,
        )
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "6789".asFHIR(),
                ),
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
            transformed.resource.identifier,
        )
        assertEquals(procedure.subject, transformed.resource.subject)
        assertEquals(procedure.status, transformed.resource.status)
        assertEquals(CodeableConcept(), transformed.resource.code)
    }

    @Test
    fun `ensure missing mappings fail validation`() {
        val procedure =
            Procedure(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PROCEDURE.value)), source = Uri("source")),
                extension = listOf(procedureExtensionCode()),
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
                status = EventStatus.ON_HOLD.asCode(),
                subject = Reference(reference = FHIRString("Patient/123123")),
                code = unmappedCode1,
                category = CodeableConcept(),
            )

        val actualTransformResult = roninProcedure.transform(procedure, tenant)
        val actualException =
            assertThrows<IllegalArgumentException> {
                actualTransformResult.second.alertIfErrors()
            }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'UnmappedCode#1' has no target defined in any Procedure.code concept map for tenant 'test' @ Procedure.code",
            actualException.message,
        )
    }
}
