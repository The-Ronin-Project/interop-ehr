package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.TimingRepeatDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.TimingRepeatSerializer
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Time
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.EventTiming
import com.projectronin.interop.fhir.r4.valueset.UnitOfTime

/**
 * Describes the occurrence of an event that may occur multiple times. Timing schedules are used for specifying when
 * events are expected or requested to occur and may also be used to represent the summary of a past or ongoing event.
 * For simplicity, the definitions of Timing components are expressed as 'future' events, but such components can also
 * be used to describe historic or ongoing events. A Timing schedule can be a list of events and/or criteria for when
 * the event happens, which can be expressed in a structured form and/or as a code. When both event and a repeating
 * specification are provided, the list of events should be understood as an interpretation of the information in the
 * repeat structure.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#Timing)
 */
data class Timing(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val event: List<DateTime> = listOf(),
    val repeat: TimingRepeat? = null,
    val code: CodeableConcept? = null
) : BackboneElement

@JsonDeserialize(using = TimingRepeatDeserializer::class)
@JsonSerialize(using = TimingRepeatSerializer::class)
data class TimingRepeat(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val bounds: DynamicValue<Any>? = null,
    val count: PositiveInt? = null,
    val countMax: PositiveInt? = null,
    val duration: Double? = null,
    val durationMax: Double? = null,
    val durationUnit: UnitOfTime? = null,
    val frequency: PositiveInt? = null,
    val frequencyMax: PositiveInt? = null,
    val period: Double? = null,
    val periodMax: Double? = null,
    val periodUnit: UnitOfTime? = null,
    val dayOfWeek: List<DayOfWeek> = listOf(),
    val timeOfDay: List<Time> = listOf(),
    val `when`: List<EventTiming> = listOf(),
    val offset: UnsignedInt? = null
) : Element {
    companion object {
        val acceptedDynamicTypes = listOf(DynamicValueType.DURATION, DynamicValueType.RANGE, DynamicValueType.PERIOD)
        val invalidWhens = listOf(EventTiming.MEAL, EventTiming.BREAKFAST, EventTiming.LUNCH, EventTiming.DINNER)
    }

    init {
        bounds?.let {
            require(acceptedDynamicTypes.contains(bounds.type)) { "bounds can only be one of the following: ${acceptedDynamicTypes.joinToString { it.code }}" }
        }

        require(duration == null || durationUnit != null) { "if there's a duration, there needs to be duration units" }
        require(period == null || periodUnit != null) { "if there's a period, there needs to be period units" }
        require(duration == null || duration >= 0) { "duration SHALL be a non-negative value" }
        require(period == null || period >= 0) { "period SHALL be a non-negative value" }
        require(periodMax == null || period != null) { "If there's a periodMax, there must be a period" }
        require(durationMax == null || duration != null) { "If there's a durationMax, there must be a duration" }
        require(countMax == null || count != null) { "If there's a countMax, there must be a count" }
        require(offset == null || (`when`.isNotEmpty() && `when`.none { invalidWhens.contains(it) })) { "If there's an offset, there must be a when (and not ${invalidWhens.joinToString { it.code }})" }
        require(timeOfDay.isEmpty() || `when`.isEmpty()) { "If there's a timeOfDay, there cannot be a when, or vice versa" }
    }
}
