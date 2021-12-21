package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.base.FHIRResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.model.helper.enum
import com.projectronin.interop.ehr.model.helper.fhirElementList
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Epic's representation of an appointment from [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195) API
 */
class EpicAppointment(override val raw: String) : FHIRResource(raw), Appointment {
    override val dataSource: DataSource
        get() = DataSource.EPIC_APPORCHARD

    override val resourceType: ResourceType
        get() = ResourceType.APPOINTMENT

    override val id: String by lazy {
        // Use the contact type of ASN as an identifier to match [Data Platform](https://github.com/projectronin/dp-databricks-jobs/blob/513cd599955f5905dd20623b2714de2ab4d9c3c0/jobs/gold/mdaoc/fhir/appointment.py#L94).
        val contactTypes = jsonObject.fhirElementList("ContactIDs", ::EpicIDType)
        contactTypes.first { it.system == "ASN" }.value
    }

    override val identifier: List<Identifier> by lazy {
        jsonObject.fhirElementList("VisitTypeIDs", ::EpicIDType)
    }

    override val status: AppointmentStatus? by lazy {
        jsonObject.enum<AppointmentStatus>(
            "AppointmentStatus",
            mapOf(
                "completed" to AppointmentStatus.FULFILLED,
                "scheduled" to AppointmentStatus.PENDING,
                "no show" to AppointmentStatus.NOSHOW
            ),
            // Default to entered-in-error to agree with [Data Platform](https://github.com/projectronin/dp-databricks-jobs/blob/513cd599955f5905dd20623b2714de2ab4d9c3c0/jobs/gold/mdaoc/fhir/appointment.py#L114)
            AppointmentStatus.ENTERED_IN_ERROR
        )
    }

    override val appointmentType: CodeableConcept? = null

    override val serviceType: List<CodeableConcept> by lazy {
        // "VisitTypeName" could be service type or in some cases appointment type, for now we are defaulting to freetext service type.
        jsonObject.string("VisitTypeName")?.let { listOf(EpicTextCodableConcept(it)) } ?: listOf()
    }

    override val start: String? by lazy {
        // From testing Epic has some inconsistencies in extra whitespaces
        val rawDateString = jsonObject.string("Date")?.trim() ?: ""
        val rawTimeString = jsonObject.string("AppointmentStartTime")?.trim() ?: ""

        val dateTimeString = "$rawDateString $rawTimeString"
        val dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern("M/d/yyyy h:mm a"))

        dateTime.format(DateTimeFormatter.ISO_DATE_TIME)
    }
}
