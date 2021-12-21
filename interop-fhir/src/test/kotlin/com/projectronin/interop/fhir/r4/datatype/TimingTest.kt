package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Time
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.EventTiming
import com.projectronin.interop.fhir.r4.valueset.UnitOfTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TimingTest {
    @Test
    fun `fails for repeat with unsupported bounds dynamic value type`() {
        val exception =
            assertThrows<IllegalArgumentException> { TimingRepeat(bounds = DynamicValue(DynamicValueType.INTEGER, 1)) }
        assertEquals("bounds can only be one of the following: Duration, Range, Period", exception.message)
    }

    @Test
    fun `fails for repeat with duration and no durating units`() {
        val exception =
            assertThrows<IllegalArgumentException> { TimingRepeat(duration = 2.0) }
        assertEquals("if there's a duration, there needs to be duration units", exception.message)
    }

    @Test
    fun `fails for repeat with period and no period units`() {
        val exception =
            assertThrows<IllegalArgumentException> { TimingRepeat(period = 2.0) }
        assertEquals("if there's a period, there needs to be period units", exception.message)
    }

    @Test
    fun `fails for repeat with negative duration`() {
        val exception =
            assertThrows<IllegalArgumentException> { TimingRepeat(duration = -2.0, durationUnit = UnitOfTime.HOUR) }
        assertEquals("duration SHALL be a non-negative value", exception.message)
    }

    @Test
    fun `fails for repeat with negative period`() {
        val exception =
            assertThrows<IllegalArgumentException> { TimingRepeat(period = -2.0, periodUnit = UnitOfTime.DAY) }
        assertEquals("period SHALL be a non-negative value", exception.message)
    }

    @Test
    fun `fails for repeat with period max but no period`() {
        val exception =
            assertThrows<IllegalArgumentException> { TimingRepeat(periodMax = 14.0) }
        assertEquals("If there's a periodMax, there must be a period", exception.message)
    }

    @Test
    fun `fails for repeat with duration max but no duration`() {
        val exception =
            assertThrows<IllegalArgumentException> { TimingRepeat(durationMax = 21.0) }
        assertEquals("If there's a durationMax, there must be a duration", exception.message)
    }

    @Test
    fun `fails for repeat with count max but no count`() {
        val exception =
            assertThrows<IllegalArgumentException> { TimingRepeat(countMax = PositiveInt(100)) }
        assertEquals("If there's a countMax, there must be a count", exception.message)
    }

    @Test
    fun `fails for repeat with offset and no when`() {
        val exception =
            assertThrows<IllegalArgumentException> { TimingRepeat(offset = UnsignedInt(12)) }
        assertEquals("If there's an offset, there must be a when (and not C, CM, CD, CV)", exception.message)
    }

    @Test
    fun `fails for repeat with offset and an unsupported when`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                TimingRepeat(
                    offset = UnsignedInt(12),
                    `when` = listOf(EventTiming.MEAL)
                )
            }
        assertEquals("If there's an offset, there must be a when (and not C, CM, CD, CV)", exception.message)
    }

    @Test
    fun `fails for repeat with time of day and when`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                TimingRepeat(
                    timeOfDay = listOf(Time("08:00:00")),
                    `when` = listOf(EventTiming.UPON_WAKING)
                )
            }
        assertEquals("If there's a timeOfDay, there cannot be a when, or vice versa", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val timing = Timing(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            event = listOf(DateTime("2021-11-18")),
            repeat = TimingRepeat(
                id = "67890",
                extension = listOf(
                    Extension(
                        url = Uri("http://localhost/extension"),
                        value = DynamicValue(DynamicValueType.STRING, "Value")
                    )
                ),
                bounds = DynamicValue(DynamicValueType.DURATION, Duration(value = 3.0, code = Code("a"))),
                count = PositiveInt(5),
                countMax = PositiveInt(10),
                duration = 14.0,
                durationMax = 30.0,
                durationUnit = UnitOfTime.DAY,
                frequency = PositiveInt(2),
                frequencyMax = PositiveInt(10),
                period = 6.0,
                periodMax = 8.0,
                periodUnit = UnitOfTime.HOUR,
                dayOfWeek = listOf(DayOfWeek.MONDAY),
                `when` = listOf(EventTiming.UPON_WAKING),
                offset = UnsignedInt(12)
            ),
            code = CodeableConcept(text = "code-concept")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(timing)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "event" : [ "2021-11-18" ],
            |  "repeat" : {
            |    "id" : "67890",
            |    "extension" : [ {
            |      "url" : "http://localhost/extension",
            |      "valueString" : "Value"
            |    } ],
            |    "boundsDuration" : {
            |      "value" : 3.0,
            |      "code" : "a"
            |    },
            |    "count" : 5,
            |    "countMax" : 10,
            |    "duration" : 14.0,
            |    "durationMax" : 30.0,
            |    "durationUnit" : "d",
            |    "frequency" : 2,
            |    "frequencyMax" : 10,
            |    "period" : 6.0,
            |    "periodMax" : 8.0,
            |    "periodUnit" : "h",
            |    "dayOfWeek" : [ "mon" ],
            |    "when" : [ "WAKE" ],
            |    "offset" : 12
            |  },
            |  "code" : {
            |    "text" : "code-concept"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedTiming = objectMapper.readValue<Timing>(json)
        assertEquals(timing, deserializedTiming)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val timing = Timing(
            repeat = TimingRepeat(
                timeOfDay = listOf(Time("12:00:00"))
            )
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(timing)

        val expectedJson = """
            |{
            |  "repeat" : {
            |    "timeOfDay" : [ "12:00:00" ]
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "repeat" : {
            |    "count" : 3
            |  }
            |}""".trimMargin()
        val timing = objectMapper.readValue<Timing>(json)

        assertNull(timing.id)
        assertEquals(listOf<Extension>(), timing.extension)
        assertEquals(listOf<DateTime>(), timing.event)
        assertEquals(TimingRepeat(count = PositiveInt(3)), timing.repeat)
        assertNull(timing.code)
    }
}
