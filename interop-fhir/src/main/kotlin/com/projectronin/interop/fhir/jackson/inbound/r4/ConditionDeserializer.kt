package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.common.jackson.getAs
import com.projectronin.interop.common.jackson.getAsList
import com.projectronin.interop.common.jackson.getAsOrNull
import com.projectronin.interop.fhir.jackson.getDynamicValueOrNull
import com.projectronin.interop.fhir.r4.resource.Condition

/**
 * Jackson deserializer for [Condition]s
 */
class ConditionDeserializer : StdDeserializer<Condition>(Condition::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Condition {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return Condition(
            id = node.getAsOrNull("id", p),
            meta = node.getAsOrNull("meta", p),
            implicitRules = node.getAsOrNull("implicitRules", p),
            language = node.getAsOrNull("language", p),
            text = node.getAsOrNull("text", p),
            contained = node.getAsList("contained", p),
            extension = node.getAsList("extension", p),
            modifierExtension = node.getAsList("modifierExtension", p),
            identifier = node.getAsList("identifier", p),
            clinicalStatus = node.getAsOrNull("clinicalStatus", p),
            verificationStatus = node.getAsOrNull("verificationStatus", p),
            category = node.getAsList("category", p),
            severity = node.getAsOrNull("severity", p),
            code = node.getAsOrNull("code", p),
            bodySite = node.getAsList("bodySite", p),
            subject = node.getAs("subject", p),
            encounter = node.getAsOrNull("encounter", p),
            onset = node.getDynamicValueOrNull("onset", p),
            abatement = node.getDynamicValueOrNull("abatement", p),
            recordedDate = node.getAsOrNull("recordedDate", p),
            recorder = node.getAsOrNull("recorder", p),
            asserter = node.getAsOrNull("asserter", p),
            stage = node.getAsList("stage", p),
            evidence = node.getAsList("evidence", p),
            note = node.getAsList("note", p)
        )
    }
}
