package com.projectronin.interop.ehr.epic.model.outbound

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.projectronin.interop.ehr.epic.model.EpicAppointment

/**
 * Custom serializer for [EpicAppointment]
 *
 * This, plus it's associated [deserializer][com.projectronin.interop.ehr.epic.model.inbound.EpicAppointmentDeserializer],
 * are used to enable Mirth to queue messages. This allows us to store an [EpicAppointment] in the Mirth DB as raw JSON
 * and process that data from the message content rather than a POJO which Mirth might decide to serialize on its own.
 *
 */
class EpicAppointmentSerializer : StdSerializer<EpicAppointment>(EpicAppointment::class.java) {
    override fun serialize(epicAppointment: EpicAppointment?, gen: JsonGenerator?, provider: SerializerProvider?) {
        val jsonGenerator = gen ?: return
        val appointment = epicAppointment ?: return

        jsonGenerator.writeStartObject()
        jsonGenerator.writeFieldName("appOrchardAppointment")
        jsonGenerator.writeObject(appointment.resource)
        jsonGenerator.writeArrayFieldStart("providerIDMap")
        appointment.providerIdMap?.forEach {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeFieldName("key")
            jsonGenerator.writeObject(it.key)
            jsonGenerator.writeFieldName("value")
            jsonGenerator.writeObject(it.value)
            jsonGenerator.writeEndObject()
        }
        jsonGenerator.writeEndArray()
        jsonGenerator.writeFieldName("patientIdentifier")
        jsonGenerator.writeObject(appointment.patientIdentifier)
        jsonGenerator.writeEndObject()
    }
}
