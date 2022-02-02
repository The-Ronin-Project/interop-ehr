package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Coding
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.Coding as R4Coding

class EpicCoding(override val element: R4Coding) : JSONElement(element), Coding {
    override val system: String? = element.system?.value
    override val version: String? = element.version
    override val code: String? = element.code?.value
    override val display: String? = element.display
    override val userSelected: Boolean? = element.userSelected
}
