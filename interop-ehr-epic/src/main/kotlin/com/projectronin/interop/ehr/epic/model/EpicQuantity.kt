package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.ehr.model.Quantity
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.ehr.model.enums.QuantityComparator
import com.projectronin.interop.fhir.r4.datatype.Quantity as R4Quantity

class EpicQuantity(override val element: R4Quantity) : JSONElement(element), Quantity {
    override val comparator: QuantityComparator? by lazy {
        element.comparator?.let { CodedEnum.byCode<QuantityComparator>(it.code) }
    }
    override val value: Double? = element.value
    override val unit: String? = element.unit
    override val system: String? = element.system?.value
    override val code: String? = element.code?.value
}