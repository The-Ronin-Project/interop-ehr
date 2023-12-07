package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.HumanNameGenerator
import com.projectronin.interop.fhir.generators.datatypes.name
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.test.data.generator.collection.ListDataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class RoninNameUtilTest {
    @Test
    fun `generates a Ronin Official Name`() {
        val testList = ListDataGenerator(2, HumanNameGenerator())
        val officialName = rcdmName(testList)
        assertNotNull(officialName)
        assertEquals("official", officialName[2].use?.value)
    }

    @Test
    fun `doesnt generates a Ronin Official Name`() {
        val testList =
            ListDataGenerator(0, HumanNameGenerator()).plus(
                name {
                    use of Code("official")
                    family of "family"
                    given of listOf("given")
                },
            )
        val officialName = rcdmName(testList)
        assertEquals(testList.generate(), officialName)
    }

    @Test
    fun `doesnt generates a Ronin Official Name because the list of names all have uses`() {
        val testList =
            ListDataGenerator(0, HumanNameGenerator()).plus(
                name {
                    use of Code("usual")
                    family of "family"
                    given of listOf("given")
                },
            ).plus(
                name {
                    use of Code("old")
                    family of "family"
                    given of listOf("given")
                },
            )
        val officialName = rcdmName(testList)
        assertEquals(testList.generate(), officialName)
    }
}
