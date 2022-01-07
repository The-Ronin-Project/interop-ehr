package com.projectronin.interop.fhir.jackson.inbound.r4

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.fhir.jackson.getAs
import com.projectronin.interop.fhir.jackson.getAsList
import com.projectronin.interop.fhir.jackson.getAsOrNull
import com.projectronin.interop.fhir.jackson.getDynamicValueOrNull
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPatient

/**
 * Jackson deserializer for [OncologyPatient]s
 */
class OncologyPatientDeserializer : StdDeserializer<OncologyPatient>(OncologyPatient::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OncologyPatient {
        val node = p.codec.readTree<JsonNode>(p) ?: throw JsonParseException(p, "Unable to parse node")

        return OncologyPatient(
            id = node.getAsOrNull("id", p),
            meta = node.getAsOrNull("meta", p),
            implicitRules = node.getAsOrNull("implicitRules", p),
            language = node.getAsOrNull("language", p),
            text = node.getAsOrNull("text", p),
            contained = node.getAsList("contained", p),
            extension = node.getAsList("extension", p),
            modifierExtension = node.getAsList("modifierExtension", p),
            identifier = node.getAsList("identifier", p),
            active = node.getAsOrNull("active", p),
            name = node.getAsList("name", p),
            telecom = node.getAsList("telecom", p),
            gender = node.getAs("gender", p),
            birthDate = node.getAs("birthDate", p),
            deceased = node.getDynamicValueOrNull("deceased", p),
            address = node.getAsList("address", p),
            maritalStatus = node.getAs("maritalStatus", p),
            multipleBirth = node.getDynamicValueOrNull("multipleBirth", p),
            photo = node.getAsList("photo", p),
            contact = node.getAsList("contact", p),
            communication = node.getAsList("communication", p),
            generalPractitioner = node.getAsList("generalPractitioner", p),
            managingOrganization = node.getAsOrNull("managingOrganization", p),
            link = node.getAsList("link", p)
        )
    }
}
