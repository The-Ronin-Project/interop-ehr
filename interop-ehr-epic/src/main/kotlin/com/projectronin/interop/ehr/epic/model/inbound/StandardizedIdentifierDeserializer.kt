package com.projectronin.interop.ehr.epic.model.inbound

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.epic.EpicIdentifierService.StandardizedIdentifier
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.model.EpicIDType

/**
 * Custom deserializer for [StandardizedIdentifier]
 *
 * Required by [com.projectronin.interop.ehr.epic.model.inbound.EpicAppointmentDeserializer].
 *
 */
class StandardizedIdentifierDeserializer : StdDeserializer<StandardizedIdentifier>(StandardizedIdentifier::class.java) {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): StandardizedIdentifier {
        val rootNode: JsonNode = p?.codec?.readTree(p)!!
        val system = if (rootNode.hasNonNull("system")) rootNode.get("system").asText() else null
        val idTypeRaw = rootNode.get("element").toString()
        val iDType = JacksonManager.objectMapper.readValue(idTypeRaw, IDType::class.java)
        return StandardizedIdentifier(system, EpicIDType(iDType))
    }
}
