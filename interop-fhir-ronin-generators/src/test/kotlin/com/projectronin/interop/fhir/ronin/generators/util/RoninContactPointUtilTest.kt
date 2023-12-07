package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.ContactPointGenerator
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.ronin.element.RoninContactPoint
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.test.data.generator.collection.ListDataGenerator
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninContactPointUtilTest {
    private lateinit var registryClient: NormalizationRegistryClient
    private lateinit var roninContactPoint: RoninContactPoint

    @BeforeEach
    fun setup() {
        registryClient = mockk()
        roninContactPoint = RoninContactPoint(registryClient)
    }

    @Test
    fun `generate ronin contact point list with existing list`() {
        val contactPointList = ListDataGenerator(2, ContactPointGenerator())
        val roninContactPoint1 = rcdmContactPoint("test", contactPointList).generate()
        val validation = roninContactPoint.validateRonin(roninContactPoint1, LocationContext(Patient::class), Validation()).hasErrors()
        assertEquals(false, validation)
        assertEquals(2, roninContactPoint1.size)
        assertEquals(
            "http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomSystem",
            roninContactPoint1[0].system!!.extension[0].url!!.value,
        )
        assertEquals(
            "http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomUse",
            roninContactPoint1[0].use!!.extension[0].url!!.value,
        )
        assertEquals(
            "http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomSystem",
            roninContactPoint1[1].system!!.extension[0].url!!.value,
        )
        assertEquals(
            "http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomUse",
            roninContactPoint1[1].use!!.extension[0].url!!.value,
        )
    }

    @Test
    fun `generate do not generate contact point if no input`() {
        val contactPointList = ListDataGenerator(0, ContactPointGenerator())
        val roninContactPoint1 = rcdmContactPoint("test", contactPointList).generate()
        val validation = roninContactPoint.validateRonin(roninContactPoint1, LocationContext(Patient::class), Validation()).hasErrors()
        assertEquals(false, validation)
        assertEquals(0, roninContactPoint1.size)
    }

    @Test
    fun `does not generate contact point for invalid system and use codes`() {
        val contactPoint1 =
            ContactPoint(
                value = "123-456-7890".asFHIR(),
                system = Code(ContactPointSystem.PHONE.code),
            )
        val contactPoint2 =
            ContactPoint(
                value = "321-654-0987".asFHIR(),
                system = Code(ContactPointSystem.PHONE.code),
                use = Code(ContactPointUse.HOME.code),
            )
        val contactPoint3 =
            ContactPoint(
                value = "098-765-4321".asFHIR(),
                use = Code(ContactPointUse.HOME.code),
            )
        val contactPoint4 =
            ContactPoint(
                value = "098-765-4321".asFHIR(),
            )
        val contactPointList =
            ListDataGenerator(
                0,
                ContactPointGenerator(),
            ).plus(contactPoint1).plus(contactPoint2).plus(contactPoint3).plus(contactPoint4)
        val roninContactPoint1 = rcdmContactPoint("test", contactPointList).generate()
        val validation = roninContactPoint.validateRonin(roninContactPoint1, LocationContext(Patient::class), Validation()).hasErrors()
        assertEquals(false, validation)
        assertEquals(1, roninContactPoint1.size)
    }

    @Test
    fun `test extensions`() {
        val systemValue = ContactPointSystem.PHONE.code
        val systemUri = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri
        val systemExt =
            DynamicValue(
                type = DynamicValueType.CODING,
                value =
                    Coding(
                        system =
                            Uri(
                                value = "http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem",
                            ),
                        code = Code(value = systemValue),
                    ),
            )
        val useValue = ContactPointUse.HOME.code
        val useUri = RoninExtension.TENANT_SOURCE_TELECOM_USE.uri
        val useExt =
            DynamicValue(
                type = DynamicValueType.CODING,
                value =
                    Coding(
                        system =
                            Uri(
                                value = "http://projectronin.io/fhir/CodeSystem/test/ContactPointUse",
                            ),
                        code = Code(value = useValue),
                    ),
            )
        val contactPoint1 =
            ContactPoint(
                value = "321-654-0987".asFHIR(),
                system =
                    Code(
                        value = systemValue,
                        extension =
                            listOf(
                                Extension(
                                    url = systemUri,
                                    value = systemExt,
                                ),
                            ),
                    ),
                use =
                    Code(
                        value = useValue,
                        extension =
                            listOf(
                                Extension(
                                    url = useUri,
                                    value = useExt,
                                ),
                            ),
                    ),
            )
        val contactPointList = ListDataGenerator(0, ContactPointGenerator()).plus(contactPoint1)
        val roninContactPoint1 = rcdmContactPoint("test", contactPointList).generate()
        val validation = roninContactPoint.validateRonin(roninContactPoint1, LocationContext(Patient::class), Validation()).hasErrors()
        assertEquals(false, validation)
        assertEquals(1, roninContactPoint1.size)
    }
}
