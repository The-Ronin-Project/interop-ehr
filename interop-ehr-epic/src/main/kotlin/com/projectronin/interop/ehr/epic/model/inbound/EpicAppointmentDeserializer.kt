package com.projectronin.interop.ehr.epic.model.inbound

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.epic.model.EpicAppointment
import com.projectronin.interop.fhir.r4.datatype.Identifier

/**
 * Custom deserializer for [EpicAppointment]
 *
 * This, plus it's associated [serializer][com.projectronin.interop.ehr.epic.model.outbound.EpicAppointmentSerializer],
 * are used to enable Mirth to queue messages. This allows us to store an [EpicAppointment] in the Mirth DB as raw JSON
 * and process that data from the message content rather than a POJO which Mirth might decide to serialize on its own.
 *
 */
class EpicAppointmentDeserializer : StdDeserializer<EpicAppointment>(EpicAppointment::class.java) {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): EpicAppointment {
        val rootNode: JsonNode = p?.codec?.readTree(p)!!
        val appOrchardApptRaw = rootNode.get("appOrchardAppointment").toString()
        val appOrchardAppt = JacksonManager.objectMapper.readValue(appOrchardApptRaw, Appointment::class.java)
        val providerMap = mutableMapOf<ScheduleProviderReturnWithTime, Identifier>()
        rootNode.get("providerIDMap").elements().forEach {
            val scheduleProviderReturnWithTimeRaw = it.get("key").toString()
            val scheduleProviderReturnWithTime = JacksonManager.objectMapper.readValue(
                scheduleProviderReturnWithTimeRaw,
                ScheduleProviderReturnWithTime::class.java
            )
            val identifierRaw = it.get("value").toString()
            val identifier = JacksonManager.objectMapper.readValue(identifierRaw, Identifier::class.java)
            providerMap[scheduleProviderReturnWithTime] = identifier
        }
        val patientIdentifierRaw = if (rootNode.hasNonNull("patientIdentifier")) {
            rootNode.get("patientIdentifier").toString()
        } else null
        val patientIdentifier = if (patientIdentifierRaw != null) {
            JacksonManager.objectMapper.readValue(patientIdentifierRaw, Identifier::class.java)
        } else null
        return EpicAppointment(appOrchardAppt, providerMap, patientIdentifier)
    }
}
