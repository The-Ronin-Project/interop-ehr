package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.JSONBundle
import com.projectronin.interop.ehr.model.enums.DataSource

/**
 * A bundle of Epic appointments as returned from [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195) API
 */
class EpicAppointmentBundle(
    override val resource: GetAppointmentsResponse,
    private val providerIdMap: Map<String, Map<ScheduleProviderReturnWithTime, Identifier>>,
    private val patientIdMap: Map<String, Identifier>
) :
    JSONBundle<EpicAppointment, GetAppointmentsResponse>(resource) {
    override val dataSource: DataSource = DataSource.EPIC_APPORCHARD

    override val links: List<Link> = listOf()

    override val resources: List<EpicAppointment> by lazy {
        resource.appointments.map {

            EpicAppointment(it, providerIdMap[it.id], patientIdMap[it.id])
        }
    }
}
