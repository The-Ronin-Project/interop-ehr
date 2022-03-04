package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.model.enums.QuantityComparator
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Age
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import com.projectronin.interop.fhir.r4.valueset.QuantityComparator as R4QuantityComparator

class EpicAgeTest {
    @Test
    fun `can build from object`() {
        val age = Age(
            value = 20.0,
            comparator = R4QuantityComparator.LESS_THAN,
            unit = "years",
            system = CodeSystem.UCUM.uri,
            code = Code("code")
        )

        val epicAge = EpicAge(age)
        assertEquals(age, epicAge.element)
        assertEquals(20.0, epicAge.value)
        assertEquals(QuantityComparator.LESS_THAN, epicAge.comparator)
        assertEquals("years", epicAge.unit)
        assertEquals(CodeSystem.UCUM.uri.value, epicAge.system)
        assertEquals("code", epicAge.code)
    }

    @Test
    fun `supports no value`() {
        val age = Age(
            comparator = R4QuantityComparator.LESS_THAN,
            unit = "years",
            system = CodeSystem.UCUM.uri,
            code = Code("code")
        )

        val epicAge = EpicAge(age)
        assertEquals(age, epicAge.element)
        assertNull(epicAge.value)
        assertEquals(QuantityComparator.LESS_THAN, epicAge.comparator)
        assertEquals("years", epicAge.unit)
        assertEquals(CodeSystem.UCUM.uri.value, epicAge.system)
        assertEquals("code", epicAge.code)
    }

    @Test
    fun `supports no comparator`() {
        val age = Age(
            value = 20.0,
            unit = "years",
            system = CodeSystem.UCUM.uri,
            code = Code("code")
        )

        val epicAge = EpicAge(age)
        assertEquals(age, epicAge.element)
        assertEquals(20.0, epicAge.value)
        assertNull(epicAge.comparator)
        assertEquals("years", epicAge.unit)
        assertEquals(CodeSystem.UCUM.uri.value, epicAge.system)
        assertEquals("code", epicAge.code)
    }

    @Test
    fun `supports no unit`() {
        val age = Age(
            value = 20.0,
            comparator = R4QuantityComparator.LESS_THAN,
            system = CodeSystem.UCUM.uri,
            code = Code("code")
        )

        val epicAge = EpicAge(age)
        assertEquals(age, epicAge.element)
        assertEquals(20.0, epicAge.value)
        assertEquals(QuantityComparator.LESS_THAN, epicAge.comparator)
        assertNull(epicAge.unit)
        assertEquals(CodeSystem.UCUM.uri.value, epicAge.system)
        assertEquals("code", epicAge.code)
    }

    @Test
    fun `supports no system`() {
        val age = Age(
            value = 20.0,
            comparator = R4QuantityComparator.LESS_THAN,
            unit = "years",
            code = Code("code")
        )

        val epicAge = EpicAge(age)
        assertEquals(age, epicAge.element)
        assertEquals(20.0, epicAge.value)
        assertEquals(QuantityComparator.LESS_THAN, epicAge.comparator)
        assertEquals("years", epicAge.unit)
        assertNull(epicAge.system)
        assertEquals("code", epicAge.code)
    }

    @Test
    fun `supports no code`() {
        val age = Age(
            comparator = R4QuantityComparator.LESS_THAN,
            unit = "years",
            system = CodeSystem.UCUM.uri
        )

        val epicAge = EpicAge(age)
        assertEquals(age, epicAge.element)
        assertNull(epicAge.value)
        assertEquals(QuantityComparator.LESS_THAN, epicAge.comparator)
        assertEquals("years", epicAge.unit)
        assertEquals(CodeSystem.UCUM.uri.value, epicAge.system)
        assertNull(epicAge.code)
    }

    @Test
    fun `returns JSON as raw`() {
        val age = Age(
            value = 20.0,
            comparator = R4QuantityComparator.LESS_THAN,
            unit = "years",
            system = CodeSystem.UCUM.uri,
            code = Code("code")
        )
        val json = """{
          |  "value": 20.0,
          |  "comparator":"<",
          |  "unit":"years",
          |  "system":"${CodeSystem.UCUM.uri.value}",
          |  "code":"code"
          |}""".trimMargin()

        val epicAge = EpicAge(age)
        assertEquals(age, epicAge.element)
        assertEquals(deformat(json), epicAge.raw)
    }
}
