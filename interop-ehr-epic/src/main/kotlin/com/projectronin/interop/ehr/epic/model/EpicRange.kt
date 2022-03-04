package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Range
import com.projectronin.interop.ehr.model.SimpleQuantity
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.Range as R4Range

class EpicRange(override val element: R4Range) : JSONElement(element), Range {
    override val low: SimpleQuantity? by lazy {
        element.low?.let { EpicSimpleQuantity(it) }
    }
    override val high: SimpleQuantity? by lazy {
        element.high?.let { EpicSimpleQuantity(it) }
    }
}
