package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.ReferenceTypes
import com.projectronin.interop.ehr.model.base.JSONElement

class EpicProviderReference(
    override val element: ScheduleProviderReturnWithTime,
    private val providerIdMap: Map<ScheduleProviderReturnWithTime, Identifier>
) : JSONElement(element), Reference {
    override val reference: String? = null
    override val type: String? = ReferenceTypes.PRACTITIONER
    override val display: String = element.providerName
    override val id: String? = null
    override val identifier: Identifier? by lazy {
        providerIdMap[element]
    }
}
