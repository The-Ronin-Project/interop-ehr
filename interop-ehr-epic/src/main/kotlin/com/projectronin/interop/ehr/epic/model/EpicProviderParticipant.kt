package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.model.Participant
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.Identifier

class EpicProviderParticipant(
    override val element: ScheduleProviderReturnWithTime,
    private val providerIdMap: Map<ScheduleProviderReturnWithTime, Identifier>
) : JSONElement(element), Participant {
    override val actor: Reference by lazy {
        EpicProviderReference(element, providerIdMap)
    }
}
