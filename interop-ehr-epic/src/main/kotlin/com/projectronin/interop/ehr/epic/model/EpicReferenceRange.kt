package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.ReferenceRange
import com.projectronin.interop.ehr.model.SimpleQuantity
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.ObservationReferenceRange as R4ReferenceRange

class EpicReferenceRange(override val element: R4ReferenceRange) : JSONElement(element), ReferenceRange {
    override val low: SimpleQuantity? by lazy {
        element.low?.let { EpicSimpleQuantity(it) }
    }
    override val high: SimpleQuantity? by lazy {
        element.high?.let { EpicSimpleQuantity(it) }
    }
    override val text = element.text
}
