package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.JSONBundle
import com.projectronin.interop.ehr.model.enums.DataSource

/**
 * A bundle of Epic appointments as returned from [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195) API
 */
class EpicAppointmentBundle(override val resource: GetAppointmentsResponse) :
    JSONBundle<EpicAppointment, GetAppointmentsResponse>(resource) {
    override val dataSource: DataSource
        get() = DataSource.EPIC_APPORCHARD

    override val links: List<Link> = listOf()

    override val resources: List<EpicAppointment> by lazy {
        resource.appointments.map(::EpicAppointment)
    }
}
