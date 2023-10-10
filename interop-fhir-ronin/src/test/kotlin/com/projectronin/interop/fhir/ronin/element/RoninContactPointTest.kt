package com.projectronin.interop.fhir.ronin.element

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.CareTeam
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCoding
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninContactPointTest {
    private lateinit var registryClient: NormalizationRegistryClient
    private lateinit var roninContactPoint: RoninContactPoint

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    // contact point attributes to mix to set up validation test cases
    private val emailValue = "name@site.com".asFHIR()
    private val emailSystemValue = ContactPointSystem.EMAIL.code
    private val emailSystemExtensionUri = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri
    private val emailSystemExtensionValue = DynamicValue(
        type = DynamicValueType.CODING,
        value = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", emailSystemValue)
    )
    private val emailSystem = Code(
        value = emailSystemValue,
        extension = listOf(Extension(url = emailSystemExtensionUri, value = emailSystemExtensionValue))
    )
    private val emailUseValue = ContactPointUse.HOME.code
    private val emailUseExtensionUri = RoninExtension.TENANT_SOURCE_TELECOM_USE.uri
    private val emailUseExtensionValue = DynamicValue(
        type = DynamicValueType.CODING,
        value = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.use", emailUseValue)
    )
    private val emailUse = Code(
        value = emailUseValue,
        extension = listOf(Extension(url = emailUseExtensionUri, value = emailUseExtensionValue))
    )
    private val conceptMapMetadata = ConceptMapMetadata(
        registryEntryType = "concept-map",
        conceptMapName = "test-concept-map",
        conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
        version = "1"
    )
    private val valueSetMetadata = ValueSetMetadata(
        registryEntryType = "value_set",
        valueSetName = "test-value-set",
        valueSetUuid = "03d51d53-1a31-49a9-af74-573b456efca5",
        version = "2"
    )

    @BeforeEach
    fun setup() {
        registryClient = mockk()
        roninContactPoint = RoninContactPoint(registryClient)
    }

    @Test
    fun `validateUSCore fails for missing system`() {
        val telecom =
            listOf(ContactPoint(system = null, value = emailValue))

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateUSCore(telecom, LocationContext(CareTeam::class), Validation()).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: system is a required element @ CareTeam.telecom[0].system",
            exception.message
        )
    }

    @Test
    fun `validateUSCore fails for missing value`() {
        val telecom =
            listOf(ContactPoint(system = Code(value = emailSystemValue), value = null))

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateUSCore(telecom, LocationContext(CareTeam::class), Validation()).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: value is a required element @ CareTeam.telecom[0].value",
            exception.message
        )
    }

    @Test
    fun `validateUSCore succeeds`() {
        val telecom =
            listOf(ContactPoint(system = Code(value = emailSystemValue), value = emailValue))

        roninContactPoint.validateUSCore(telecom, LocationContext(CareTeam::class), Validation()).alertIfErrors()
    }

    @Test
    fun `validate fails for telecom system missing source extension`() {
        val telecom =
            listOf(ContactPoint(system = Code(value = emailSystemValue), use = emailUse, value = emailValue))

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateRonin(telecom, LocationContext(CareTeam::class), Validation()).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CNTCTPT_001: Tenant source telecom system extension is required @ CareTeam.telecom[0].system",
            exception.message
        )
    }

    @Test
    fun `validate fails for telecom system with wrong URL in source extension`() {
        val telecom = listOf(
            ContactPoint(
                system = Code(
                    value = emailSystemValue,
                    extension = listOf(
                        Extension(
                            url = Uri("emailSystemExtension"),
                            value = emailSystemExtensionValue
                        )
                    )
                ),
                use = emailUse,
                value = emailValue
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateRonin(telecom, LocationContext(Organization::class), Validation()).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CNTCTPT_002: Tenant source telecom system extension is defined without proper URL @ Organization.telecom[0].system",
            exception.message
        )
    }

    @Test
    fun `transform filters out and returns warning when no value`() {
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("abc"), value = null))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertEquals(0, transformResult.first.size)

        val validation = transformResult.second
        assertFalse(validation.hasErrors())
        assertTrue(validation.hasIssues())

        val issues = validation.issues()
        assertEquals("RONIN_CNTCTPT_006", issues.first().code)

        verify { registryClient wasNot Called }
    }

    @Test
    fun `transform filters out and returns warning when no system`() {
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = null, value = "8675309".asFHIR()))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertEquals(0, transformResult.first.size)

        val validation = transformResult.second
        assertFalse(validation.hasErrors())
        assertTrue(validation.hasIssues())

        val issues = validation.issues()
        assertEquals("RONIN_CNTCTPT_005", issues.first().code)

        verify { registryClient wasNot Called }
    }

    @Test
    fun `transform succeeds for telecom system - when concept map returns a good value`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "abc")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("phone"), systemExtension("abc"), listOf(conceptMapMetadata))
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("abc"), value = "8675309".asFHIR()))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertEquals(
            listOf(
                ContactPoint(
                    value = "8675309".asFHIR(),
                    system = Code(
                        value = "phone",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.system"
                                            )
                                        ),
                                        code = Code(value = "abc")
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            transformResult.first
        )
        transformResult.second.alertIfErrors()
    }

    @Test
    fun `transform for telecom system - when concept map has no match - and source code is not in enum`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "xyz")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("xyz"), value = "8675309".asFHIR()))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertNotNull(transformResult.first)
        assertEquals(
            Code(value = "xyz"),
            transformResult.first.first().system
        )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'xyz' has no target defined in " +
                "http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem @ Patient.telecom[0].system",
            exception.message
        )
    }

    @Test
    fun `transform for telecom system - when concept map has a match - and the match is not in enum`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "xyz")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("postal"), systemExtension("xyz"), listOf(conceptMapMetadata))
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("xyz"), value = "8675309".asFHIR()))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertNotNull(transformResult.first)
        assertEquals(
            Code(value = "postal", extension = listOf(systemExtension("xyz"))),
            transformResult.first.first().system
        )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_CONMAP_VALUE_SET: http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem " +
                "mapped 'xyz' to 'postal' which is outside of required value set @ Patient.telecom[0].system",
            exception.message
        )
    }

    @Test
    fun `transform for telecom system - when concept map has no match - and source code is in enum`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "phone")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("phone"), value = "8675309".asFHIR()))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )

        assertNotNull(transformResult.first)
        assertEquals(
            Code(value = "phone"),
            transformResult.first.first().system
        )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'phone' " +
                "has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem " +
                "@ Patient.telecom[0].system",
            exception.message
        )
    }

    @Test
    fun `transform succeeds for telecom system with empty source value - if empty source value is in concept map`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("phone"), systemExtension(""), listOf(conceptMapMetadata))
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code(""), value = "8675309".asFHIR()))
        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertEquals(
            listOf(
                ContactPoint(
                    value = "8675309".asFHIR(),
                    system = Code(
                        value = "phone",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.system"
                                            )
                                        ),
                                        code = Code(value = "")
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            transformResult.first
        )
        transformResult.second.alertIfErrors()
    }

    @Test
    fun `transform issues logged and fails for multiple telecoms - system has concept map lookup failure`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "planet")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("home"), useExtension("planet"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "uvw")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "def")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(
            ContactPoint(system = Code("email"), value = "8675310".asFHIR(), use = Code("planet")),
            ContactPoint(system = Code("email"), value = "8675311".asFHIR(), use = Code("uvw")),
            ContactPoint(system = Code("email"), value = "8675312".asFHIR(), use = Code("def")),
            ContactPoint(system = Code("email"), value = "8675313".asFHIR(), use = Code("planet")),
            ContactPoint(system = Code("email"), value = "8675314".asFHIR(), use = Code(""))
        )

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'uvw' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointUse @ Patient.telecom[1].use\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'def' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointUse @ Patient.telecom[2].use\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value '' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointUse @ Patient.telecom[4].use",
            exception.message
        )
    }

    @Test
    fun `transform issues logged and fails for multiple telecoms - system has various issues`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "abc")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "xyz")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(
            ContactPoint(system = Code("email"), value = "8675309".asFHIR()),
            ContactPoint(system = Code("abc"), value = "8675308".asFHIR()),
            ContactPoint(system = Code(""), value = "8675307".asFHIR()),
            ContactPoint(system = Code("email"), value = "8675306".asFHIR()),
            ContactPoint(system = Code("xyz"), value = "8675305".asFHIR())
        )
        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'abc' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem @ Patient.telecom[1].system\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value '' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem @ Patient.telecom[2].system\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'xyz' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem @ Patient.telecom[4].system",
            exception.message
        )
    }

    @Test
    fun `transform succeeds for multiple telecoms - no system has issues`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "telephone")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("phone"), systemExtension("telephone"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "phone")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("phone"), systemExtension("phone"), listOf(conceptMapMetadata))
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(
            ContactPoint(system = Code("telephone"), value = "8675309".asFHIR()),
            ContactPoint(system = Code("phone"), value = "8675302".asFHIR()),
            ContactPoint(system = Code("email"), value = "8675301".asFHIR())
        )
        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertTrue(transformResult.first.size == 3)
        assertEquals(
            Code(value = "phone", extension = listOf(systemExtension("telephone"))),
            transformResult.first[0].system
        )
        assertEquals(
            Code(value = "phone", extension = listOf(systemExtension("phone"))),
            transformResult.first[1].system
        )
        assertEquals(
            Code(value = "email", extension = listOf(systemExtension("email"))),
            transformResult.first[2].system
        )
    }

    @Test
    fun `validate fails for telecom use missing source extension`() {
        val telecom =
            listOf(ContactPoint(system = emailSystem, use = Code(value = emailUseValue), value = emailValue))

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateRonin(telecom, LocationContext(CareTeam::class), Validation()).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CNTCTPT_003: Tenant source telecom use extension is required @ CareTeam.telecom[0].use",
            exception.message
        )
    }

    @Test
    fun `validate fails for telecom use with wrong URL in source extension`() {
        val telecom = listOf(
            ContactPoint(
                system = emailSystem,
                use = Code(
                    value = emailUseValue,
                    extension = listOf(
                        Extension(
                            url = Uri("emailUseExtension"),
                            value = emailUseExtensionValue
                        )
                    )
                ),
                value = emailValue
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateRonin(telecom, LocationContext(Organization::class), Validation()).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CNTCTPT_004: Tenant source telecom use extension is defined without proper URL @ Organization.telecom[0].use",
            exception.message
        )
    }

    @Test
    fun `transform succeeds for telecom use - when concept map returns a good value`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "phone")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("phone"), systemExtension("phone"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "def")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("home"), useExtension("def"), listOf(conceptMapMetadata))
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("phone"), value = "8675309".asFHIR(), use = Code("def")))

        val currentContext = LocationContext(Patient::class)
        val transformResult =
            roninContactPoint.transform(telecom, mockk<Patient>(), tenant, currentContext, Validation())
        assertEquals(
            listOf(
                ContactPoint(
                    value = "8675309".asFHIR(),
                    system = Code(
                        value = "phone",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.system"
                                            )
                                        ),
                                        code = Code(value = "phone")
                                    )
                                )
                            )
                        )
                    ),
                    use = Code(
                        value = "home",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_USE.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.use"
                                            )
                                        ),
                                        code = Code(value = "def")
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            transformResult.first
        )
        transformResult.second.alertIfErrors()
    }

    @Test
    fun `transform for telecom use - when concept map has no match - and source code is not in enum`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "xyz")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("email"), value = "8675309".asFHIR(), use = Code("xyz")))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertNotNull(transformResult.first)
        assertEquals(
            Code(value = "xyz"),
            transformResult.first.first().use
        )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'xyz' has no target defined in " +
                "http://projectronin.io/fhir/CodeSystem/test/ContactPointUse @ Patient.telecom[0].use",
            exception.message
        )
    }

    @Test
    fun `transform for telecom use - when concept map has a match - and the match is not in enum`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "xyz")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("postal"), useExtension("xyz"), listOf(conceptMapMetadata))
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("email"), value = "8675309".asFHIR(), use = Code("xyz")))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertNotNull(transformResult.first)
        assertEquals(
            Code(value = "postal", extension = listOf(useExtension("xyz"))),
            transformResult.first.first().use
        )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_CONMAP_VALUE_SET: http://projectronin.io/fhir/CodeSystem/test/ContactPointUse " +
                "mapped 'xyz' to 'postal' which is outside of required value set @ Patient.telecom[0].use",
            exception.message
        )
    }

    @Test
    fun `transform for telecom use - when concept map has no match - and source code is in enum`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "home")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("email"), value = "8675309".asFHIR(), use = Code("home")))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )

        assertNotNull(transformResult.first)
        assertEquals(
            Code(value = "home"),
            transformResult.first.first().use
        )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'home' " +
                "has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointUse " +
                "@ Patient.telecom[0].use",
            exception.message
        )
    }

    @Test
    fun `transform succeeds for telecom use with empty source value - if empty source value is in concept map`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("home"), useExtension(""), listOf(conceptMapMetadata))
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(ContactPoint(system = Code("email"), value = "8675309".asFHIR(), use = Code("")))

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertEquals(
            listOf(
                ContactPoint(
                    value = "8675309".asFHIR(),
                    system = Code(
                        value = "email",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.system"
                                            )
                                        ),
                                        code = Code(value = "email")
                                    )
                                )
                            )
                        )
                    ),
                    use = Code(
                        value = "home",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_USE.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.use"
                                            )
                                        ),
                                        code = Code(value = "")
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            transformResult.first
        )
        transformResult.second.alertIfErrors()
    }

    @Test
    fun `transform issues logged and fails for multiple telecoms - use has concept map lookup failure`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "planet")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("home"), useExtension("planet"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "uvw")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "def")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(
            ContactPoint(system = Code("email"), value = "8675310".asFHIR(), use = Code("planet")),
            ContactPoint(system = Code("email"), value = "8675311".asFHIR(), use = Code("uvw")),
            ContactPoint(system = Code("email"), value = "8675312".asFHIR(), use = Code("def")),
            ContactPoint(system = Code("email"), value = "8675313".asFHIR(), use = Code("planet")),
            ContactPoint(system = Code("email"), value = "8675314".asFHIR(), use = Code(""))
        )

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'uvw' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointUse @ Patient.telecom[1].use\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'def' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointUse @ Patient.telecom[2].use\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value '' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointUse @ Patient.telecom[4].use",
            exception.message
        )
    }

    @Test
    fun `transform issues logged and fails for multiple telecoms - use has various issues`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "planet")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("abc"), useExtension("planet"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "city")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("def"), useExtension("city"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "villa")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "home")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("home"), useExtension("home"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "work")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("work"), useExtension("work"), listOf(conceptMapMetadata))
        }

        roninContactPoint = RoninContactPoint(registryClient)

        val telecom = listOf(
            ContactPoint(system = Code("email"), value = emailValue, use = Code("city")),
            ContactPoint(system = Code("email"), value = emailValue, use = Code("home")),
            ContactPoint(system = Code("email"), value = emailValue, use = Code("planet")),
            ContactPoint(system = Code("email"), value = emailValue, use = Code("villa")),
            ContactPoint(system = Code("email"), value = emailValue, use = Code("work"))
        )

        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        val exception = assertThrows<IllegalArgumentException> {
            transformResult.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_CONMAP_VALUE_SET: http://projectronin.io/fhir/CodeSystem/test/ContactPointUse " +
                "mapped 'city' to 'def' which is outside of required value set @ Patient.telecom[0].use\n" +
                "ERROR INV_CONMAP_VALUE_SET: http://projectronin.io/fhir/CodeSystem/test/ContactPointUse " +
                "mapped 'planet' to 'abc' which is outside of required value set @ Patient.telecom[2].use\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'villa' has no target defined in " +
                "http://projectronin.io/fhir/CodeSystem/test/ContactPointUse @ Patient.telecom[3].use",
            exception.message
        )
    }

    @Test
    fun `transform succeeds for multiple telecoms - no use has issues`() {
        registryClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(systemCoding("email"), systemExtension("email"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "planet")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("home"), useExtension("planet"), listOf(conceptMapMetadata))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "city")
                    ),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    any<Patient>()
                )
            } returns ConceptMapCoding(useCoding("home"), useExtension("city"), listOf(conceptMapMetadata))
        }
        roninContactPoint = RoninContactPoint(registryClient)
        val telecom = listOf(
            ContactPoint(system = Code("email"), value = "8675309".asFHIR(), use = Code("planet")),
            ContactPoint(system = Code("email"), value = "8675302".asFHIR(), use = Code("city")),
            ContactPoint(system = Code("email"), value = "8675301".asFHIR(), use = Code("planet"))
        )
        val transformResult =
            roninContactPoint.transform(
                telecom,
                mockk<Patient>(),
                tenant,
                LocationContext(Patient::class),
                Validation()
            )
        assertTrue(transformResult.first.size == 3)
        assertEquals(Code(value = "home", extension = listOf(useExtension("planet"))), transformResult.first[0].use)
        assertEquals(Code(value = "home", extension = listOf(useExtension("city"))), transformResult.first[1].use)
        assertEquals(Code(value = "home", extension = listOf(useExtension("planet"))), transformResult.first[2].use)
    }

    private fun systemCoding(value: String) = Coding(
        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
        code = Code(value = value)
    )

    private fun systemExtension(value: String) = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomSystem"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                code = Code(value = value)
            )
        )
    )

    private fun useCoding(value: String) = Coding(
        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
        code = Code(value = value)
    )

    private fun useExtension(value: String) = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomUse"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                code = Code(value = value)
            )
        )
    )
}
