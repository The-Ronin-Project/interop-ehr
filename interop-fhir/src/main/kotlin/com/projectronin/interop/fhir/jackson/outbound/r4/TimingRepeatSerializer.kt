package com.projectronin.interop.fhir.jackson.outbound.r4

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.projectronin.interop.common.jackson.writeListField
import com.projectronin.interop.common.jackson.writeNullableField
import com.projectronin.interop.fhir.jackson.writeDynamicValueField
import com.projectronin.interop.fhir.r4.datatype.TimingRepeat

/**
 * Jackson serializer for [TimingRepeat]s
 */
class TimingRepeatSerializer : BaseElementSerializer<TimingRepeat>(TimingRepeat::class.java) {
    override fun serializeSpecificElement(value: TimingRepeat, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeDynamicValueField("bounds", value.bounds)
        gen.writeNullableField("count", value.count)
        gen.writeNullableField("countMax", value.countMax)
        gen.writeNullableField("duration", value.duration)
        gen.writeNullableField("durationMax", value.durationMax)
        gen.writeNullableField("durationUnit", value.durationUnit)
        gen.writeNullableField("frequency", value.frequency)
        gen.writeNullableField("frequencyMax", value.frequencyMax)
        gen.writeNullableField("period", value.period)
        gen.writeNullableField("periodMax", value.periodMax)
        gen.writeNullableField("periodUnit", value.periodUnit)
        gen.writeListField("dayOfWeek", value.dayOfWeek)
        gen.writeListField("timeOfDay", value.timeOfDay)
        gen.writeListField("when", value.`when`)
        gen.writeNullableField("offset", value.offset)
    }
}
