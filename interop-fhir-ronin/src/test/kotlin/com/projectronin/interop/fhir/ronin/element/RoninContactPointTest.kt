package com.projectronin.interop.fhir.ronin.element

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.ronin.conceptmap.ConceptMapClient
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.resource.RoninPatient
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [ContactPoint] test cases are exercised in this class using the telecom attribute in [Patient] and [RoninPatient].
 * The same test cases apply to, and can be exercised in this class using, the telecom attribute in
 * [Practitioner] and [RoninPractitioner], or in [Organization] and [RoninOrganization], interchangeably with
 * [Patient] and [RoninPatient]. Interchanging the specific parent resource type makes a difference only in the correct
 * parent resource type name being output to validation issue messages. [ContactPoint] handles this automatically.
 */
class RoninContactPointTest {
    private lateinit var conceptMapClient: ConceptMapClient
    private lateinit var roninContactPoint: RoninContactPoint

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val identifierList = listOf(
        Identifier(
            type = CodeableConcept(
                text = "MRN".asFHIR()
            ),
            system = Uri("mrnSystem"),
            value = "An MRN".asFHIR()
        )
    )

    // contact point attributes to mix to set up validation test cases
    private val emailValue = "name@site.com"
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

    @BeforeEach
    fun setup() {
        conceptMapClient = mockk()
        roninContactPoint = RoninContactPoint(conceptMapClient)
    }

    @Test
    fun `validate fails for telecom system missing source extension`() {
        val telecom =
            listOf(ContactPoint(system = Code(value = emailSystemValue), use = emailUse, value = emailValue.asFHIR()))

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateRonin(telecom, LocationContext("CareTeam", ""), Validation()).alertIfErrors()
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
                value = emailValue.asFHIR()
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateRonin(telecom, LocationContext("Organization", ""), Validation()).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CNTCTPT_002: Tenant source telecom system extension is defined without proper URL @ Organization.telecom[0].system",
            exception.message
        )
    }

    @Test
    fun `transform succeeds for telecom system - when concept map returns a good value`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "abc")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("phone"), systemExtension("abc"))
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(ContactPoint(system = Code("abc"), value = "8675309".asFHIR()))

        val transformResult =
            roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), Validation())
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
    fun `transform fails for telecom system - when concept map has no match`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "xyz")
                    ),
                    ContactPointSystem::class
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(ContactPoint(system = Code("xyz"), value = "8675309".asFHIR()))

        val transformResult =
            roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), Validation())
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
    fun `transform succeeds for telecom system with empty source value - if empty source value is in concept map`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("phone"), systemExtension(""))
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(ContactPoint(system = Code(""), value = "8675309".asFHIR()))
        val transformResult =
            roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), Validation())
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
    fun `transform issues logged and fails for multiple telecoms - system has various issues`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("email"), systemExtension("email"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "abc")
                    ),
                    ContactPointSystem::class
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "")
                    ),
                    ContactPointSystem::class
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "xyz")
                    ),
                    ContactPointSystem::class
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(
            ContactPoint(system = Code("email"), value = "8675309".asFHIR()),
            ContactPoint(system = Code("abc"), value = "8675308".asFHIR()),
            ContactPoint(system = Code(""), value = "8675307".asFHIR()),
            ContactPoint(system = Code("email"), value = "8675306".asFHIR()),
            ContactPoint(system = Code("xyz"), value = "8675305".asFHIR())
        )
        val transformResult =
            roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), Validation())
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
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("email"), systemExtension("email"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "telephone")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("phone"), systemExtension("telephone"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "phone")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("phone"), systemExtension("phone"))
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(
            ContactPoint(system = Code("telephone"), value = "8675309".asFHIR()),
            ContactPoint(system = Code("phone"), value = "8675302".asFHIR()),
            ContactPoint(system = Code("email"), value = "8675301".asFHIR()),
        )
        val transformResult =
            roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), Validation())
        assertTrue(transformResult.first!!.size == 3)
        assertEquals(
            Code(value = "phone", extension = listOf(systemExtension("telephone"))),
            transformResult.first!![0].system
        )
        assertEquals(
            Code(value = "phone", extension = listOf(systemExtension("phone"))),
            transformResult.first!![1].system
        )
        assertEquals(
            Code(value = "email", extension = listOf(systemExtension("email"))),
            transformResult.first!![2].system
        )
    }

    @Test
    fun `validate fails for telecom use missing source extension`() {
        val telecom =
            listOf(ContactPoint(system = emailSystem, use = Code(value = emailUseValue), value = emailValue.asFHIR()))

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateRonin(telecom, LocationContext("CareTeam", ""), Validation()).alertIfErrors()
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
                value = emailValue.asFHIR()
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninContactPoint.validateRonin(telecom, LocationContext("Organization", ""), Validation()).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CNTCTPT_004: Tenant source telecom use extension is defined without proper URL @ Organization.telecom[0].use",
            exception.message
        )
    }

    @Test
    fun `transform succeeds for telecom use - when concept map returns a good value`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "phone")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("phone"), systemExtension("phone"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "def")
                    ),
                    ContactPointUse::class
                )
            } returns Pair(useCoding("home"), useExtension("def"))
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(ContactPoint(system = Code("phone"), value = "8675309".asFHIR(), use = Code("def")))

        val currentContext = LocationContext(Patient::class)
        val transformResult = roninContactPoint.transform(telecom, tenant, currentContext, Validation())
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
    fun `transform fails for telecom use - when concept map has no match`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("email"), systemExtension("email"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "xyz")
                    ),
                    ContactPointUse::class
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(ContactPoint(system = Code("email"), value = "8675309".asFHIR(), use = Code("xyz")))

        val transformResult =
            roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), Validation())
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
    fun `transform succeeds for telecom use with empty source value - if empty source value is in concept map`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("email"), systemExtension("email"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "")
                    ),
                    ContactPointUse::class
                )
            } returns Pair(useCoding("home"), useExtension(""))
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(ContactPoint(system = Code("email"), value = "8675309".asFHIR(), use = Code("")))

        val transformResult =
            roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), Validation())
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
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("email"), systemExtension("email"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "planet")
                    ),
                    ContactPointUse::class
                )
            } returns Pair(useCoding("home"), useExtension("planet"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "uvw")
                    ),
                    ContactPointUse::class
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "def")
                    ),
                    ContactPointUse::class
                )
            } returns null
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "")
                    ),
                    ContactPointUse::class
                )
            } returns null
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(
            ContactPoint(system = Code("email"), value = "8675310".asFHIR(), use = Code("planet")),
            ContactPoint(system = Code("email"), value = "8675311".asFHIR(), use = Code("uvw")),
            ContactPoint(system = Code("email"), value = "8675312".asFHIR(), use = Code("def")),
            ContactPoint(system = Code("email"), value = "8675313".asFHIR(), use = Code("planet")),
            ContactPoint(system = Code("email"), value = "8675314".asFHIR(), use = Code("")),
        )

        val transformResult =
            roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), Validation())
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
    fun `transform succeeds for multiple telecoms - no use has issues`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "email")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("email"), systemExtension("email"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "planet")
                    ),
                    ContactPointUse::class
                )
            } returns Pair(useCoding("home"), useExtension("planet"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "city")
                    ),
                    ContactPointUse::class
                )
            } returns Pair(useCoding("home"), useExtension("city"))
        }
        roninContactPoint = RoninContactPoint(conceptMapClient)
        val telecom = listOf(
            ContactPoint(system = Code("email"), value = "8675309".asFHIR(), use = Code("planet")),
            ContactPoint(system = Code("email"), value = "8675302".asFHIR(), use = Code("city")),
            ContactPoint(system = Code("email"), value = "8675301".asFHIR(), use = Code("planet")),
        )
        val transformResult =
            roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), Validation())
        assertTrue(transformResult.first!!.size == 3)
        assertEquals(Code(value = "home", extension = listOf(useExtension("planet"))), transformResult.first!![0].use)
        assertEquals(Code(value = "home", extension = listOf(useExtension("city"))), transformResult.first!![1].use)
        assertEquals(Code(value = "home", extension = listOf(useExtension("planet"))), transformResult.first!![2].use)
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
