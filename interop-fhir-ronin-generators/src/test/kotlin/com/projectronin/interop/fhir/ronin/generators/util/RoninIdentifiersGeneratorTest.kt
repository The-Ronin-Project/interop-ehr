package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.IdentifierGenerator
import com.projectronin.interop.fhir.generators.datatypes.identifier
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.test.data.generator.collection.ListDataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class RoninIdentifiersGeneratorTest {

    @Test
    fun `generates Ronin Identifiers if no Ronin Identifiers`() {
        val testList: ListDataGenerator<Identifier> = ListDataGenerator(2, IdentifierGenerator())
        val identifiers = RoninIdentifiersGenerator().setRoninIdentifiers(testList, "test").generate()
        assertNotNull(identifiers)
        assertEquals(4, identifiers.size)
        assertEquals(CodeSystem.RONIN_FHIR_ID.uri, identifiers[2].system)
        assertEquals(CodeSystem.RONIN_TENANT.uri, identifiers[3].system)
    }

    @Test
    fun `generates Ronin Tenant ID if no Ronin Tenant ID`() {
        val testList: ListDataGenerator<Identifier> = ListDataGenerator(0, IdentifierGenerator()).plus(
            identifier {
                system of CodeSystem.RONIN_FHIR_ID.uri
                value of UUID.randomUUID().toString()
            }
        )
        val identifiers = RoninIdentifiersGenerator().setRoninIdentifiers(testList, "test").generate()
        assertNotNull(identifiers)
        assertEquals(2, identifiers.size)
        assertEquals(CodeSystem.RONIN_FHIR_ID.uri, identifiers[0].system)
        assertEquals(CodeSystem.RONIN_TENANT.uri, identifiers[1].system)
    }

    @Test
    fun `generates Ronin FHIR ID if no Ronin FHIR ID`() {
        val testList: ListDataGenerator<Identifier> = ListDataGenerator(0, IdentifierGenerator()).plus(
            identifier {
                system of CodeSystem.RONIN_TENANT.uri
                value of "test"
            }
        )
        val identifiers = RoninIdentifiersGenerator().setRoninIdentifiers(testList, "test").generate()
        assertNotNull(identifiers)
        assertEquals(2, identifiers.size)
        assertEquals(CodeSystem.RONIN_FHIR_ID.uri, identifiers[1].system)
        assertEquals(CodeSystem.RONIN_TENANT.uri, identifiers[0].system)
    }

    @Test
    fun`doesn't generate additional IDs if the Ronin Ids exist`() {
        val testList: ListDataGenerator<Identifier> = ListDataGenerator(0, IdentifierGenerator()).plus(
            identifier {
                system of CodeSystem.RONIN_FHIR_ID.uri
                value of UUID.randomUUID().toString()
            }
        ).plus(
            identifier {
                system of CodeSystem.RONIN_TENANT.uri
                value of "test"
            }
        )
        val identifiers = RoninIdentifiersGenerator().setRoninIdentifiers(testList, "test").generate()
        assertEquals(2, identifiers.size)
        assertEquals(testList.generate(), identifiers)
    }
}
