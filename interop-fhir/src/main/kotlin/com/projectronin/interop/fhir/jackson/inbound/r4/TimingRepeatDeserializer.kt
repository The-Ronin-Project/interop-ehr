package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.fhir.jackson.getAsDoubleOrNull
import com.projectronin.interop.fhir.jackson.getAsList
import com.projectronin.interop.fhir.jackson.getAsOrNull
import com.projectronin.interop.fhir.jackson.getAsTextOrNull
import com.projectronin.interop.fhir.jackson.getDynamicValueOrNull
import com.projectronin.interop.fhir.r4.datatype.TimingRepeat

/**
 * Jackson deserializer for [TimingRepeat]s
 */
class TimingRepeatDeserializer : StdDeserializer<TimingRepeat>(TimingRepeat::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TimingRepeat {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return TimingRepeat(
            id = node.getAsTextOrNull("id"),
            extension = node.getAsList("extension", p),
            bounds = node.getDynamicValueOrNull("bounds", p),
            count = node.getAsOrNull("count", p),
            countMax = node.getAsOrNull("countMax", p),
            duration = node.getAsDoubleOrNull("duration"),
            durationMax = node.getAsDoubleOrNull("durationMax"),
            durationUnit = node.getAsOrNull("durationUnit", p),
            frequency = node.getAsOrNull("frequency", p),
            frequencyMax = node.getAsOrNull("frequencyMax", p),
            period = node.getAsDoubleOrNull("period"),
            periodMax = node.getAsDoubleOrNull("periodMax"),
            periodUnit = node.getAsOrNull("periodUnit", p),
            dayOfWeek = node.getAsList("dayOfWeek", p),
            timeOfDay = node.getAsList("timeOfDay", p),
            `when` = node.getAsList("when", p),
            offset = node.getAsOrNull("offset", p)
        )
    }
}
