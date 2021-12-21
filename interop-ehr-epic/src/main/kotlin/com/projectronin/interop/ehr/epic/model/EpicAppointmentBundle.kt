package com.projectronin.interop.ehr.epic.model

import com.beust.klaxon.JsonObject
import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.FHIRBundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.model.helper.fhirElementList

/**
 * A bundle of Epic appointments as returned from [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195) API
 */
class EpicAppointmentBundle(override val raw: String) :
    FHIRBundle<EpicAppointment>(raw) {
    override val dataSource: DataSource
        get() = DataSource.EPIC_APPORCHARD

    override val links: List<Link> by lazy {
        jsonObject.fhirElementList("link", ::EpicLink)
    }

    override val resources: List<EpicAppointment> by lazy {
        jsonObject.array<JsonObject>("Appointments")?.map { EpicAppointment(it.toJsonString()) }
            ?: listOf()
    }
}
