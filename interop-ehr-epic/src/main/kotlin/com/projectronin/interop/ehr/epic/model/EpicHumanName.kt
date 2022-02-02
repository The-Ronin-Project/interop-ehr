package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.HumanName
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.valueset.NameUse
import com.projectronin.interop.fhir.r4.datatype.HumanName as R4HumanName

class EpicHumanName(override val element: R4HumanName) : JSONElement(element), HumanName {
    override val use: NameUse? = element.use
    override val family: String? = element.family
    override val given: List<String> = element.given
}
