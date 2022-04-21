package com.projectronin.interop.ehr.epic.model.outbound

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.projectronin.interop.ehr.epic.EpicIdentifierService.StandardizedIdentifier

/**
 * Custom deserializer for [StandardizedIdentifier]
 *
 * Required by [com.projectronin.interop.ehr.epic.model.outbound.EpicAppointmentSerializer].
 *
 */
class StandardizedIdentifierSerializer : StdSerializer<StandardizedIdentifier>(StandardizedIdentifier::class.java) {
    override fun serialize(standardizedIdentifier: StandardizedIdentifier?, gen: JsonGenerator?, provider: SerializerProvider?) {
        val jsonGenerator = gen ?: return
        val identifier = standardizedIdentifier ?: return

        jsonGenerator.writeStartObject()
        jsonGenerator.writeStringField("system", identifier.system)
        jsonGenerator.writeFieldName("element")
        jsonGenerator.writeObject(identifier.element)
        jsonGenerator.writeEndObject()
    }
}
