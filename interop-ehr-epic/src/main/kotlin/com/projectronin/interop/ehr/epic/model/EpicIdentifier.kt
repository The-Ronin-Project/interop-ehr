package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.Identifier as R4Identifier

class EpicIdentifier(override val element: R4Identifier) : JSONElement(element), Identifier {
    override val system: String = element.system!!.value
    override val value: String = element.value ?: ""
}
