package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Period
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.Period as R4Period

class EpicPeriod(override val element: R4Period) : JSONElement(element), Period {
    override val start: String? = element.start?.value
    override val end: String? = element.end?.value
}
