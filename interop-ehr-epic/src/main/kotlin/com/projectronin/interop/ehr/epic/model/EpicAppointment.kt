package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Participant
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment as AppOrchardAppointment

/**
 * Epic's representation of an appointment from [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195) API
 */
class EpicAppointment(
    override val resource: AppOrchardAppointment,
    private val providerIdMap: Map<String, Map<ScheduleProviderReturnWithTime, Identifier>>
) : JSONResource(resource), Appointment {
    override val dataSource: DataSource = DataSource.EPIC_APPORCHARD
    override val resourceType: ResourceType = ResourceType.APPOINTMENT

    override val id: String = resource.id
    override val appointmentType: CodeableConcept? = null
    override val status: AppointmentStatus = resource.status

    override val identifier: List<Identifier> by lazy {
        resource.visitTypeIDs.map(::EpicIDType)
    }

    override val serviceType: List<CodeableConcept> by lazy {
        // "VisitTypeName" could be service type or in some cases appointment type, for now we are defaulting to freetext service type.
        listOf(EpicTextCodableConcept(resource.visitTypeName))
    }

    override val start: String? by lazy {
        // From testing Epic has some inconsistencies in extra whitespaces
        val rawDateString = resource.date.trim()
        val rawTimeString = resource.appointmentStartTime.trim()

        val dateTimeString = "$rawDateString $rawTimeString"
        val dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern("M/d/yyyy h:mm a"))

        dateTime.format(DateTimeFormatter.ISO_DATE_TIME)
    }

    override val participants: List<Participant> by lazy {
        val providerIdMap = providerIdMap[resource.id] ?: emptyMap()
        resource.providers.map { EpicProviderParticipant(it, providerIdMap) }
    }
}
