package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Quantity
import com.projectronin.interop.ehr.model.Ratio
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.Ratio as R4Ratio

class EpicRatio(override val element: R4Ratio) : JSONElement(element), Ratio {
    override val denominator: Quantity? by lazy {
        element.denominator?.let { EpicQuantity(it) }
    }
    override val numerator: Quantity? by lazy {
        element.numerator?.let { EpicQuantity(it) }
    }
}
