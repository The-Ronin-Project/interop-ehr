package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.SimpleQuantity
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.SimpleQuantity as R4SimpleQuantity

open class EpicSimpleQuantity(override val element: R4SimpleQuantity) : JSONElement(element), SimpleQuantity {
    override val value: Double? = element.value
    override val unit: String? = element.unit
    override val system: String? = element.system?.value
    override val code: String? = element.code?.value
}
