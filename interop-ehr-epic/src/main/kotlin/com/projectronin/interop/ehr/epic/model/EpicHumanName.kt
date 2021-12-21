package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.HumanName
import com.projectronin.interop.ehr.model.base.FHIRElement
import com.projectronin.interop.ehr.model.helper.enum
import com.projectronin.interop.fhir.r4.valueset.NameUse

class EpicHumanName(override val raw: String) : FHIRElement(raw), HumanName {
    override val use: NameUse? by lazy {
        jsonObject.enum<NameUse>("use")
    }
    override val family: String? by lazy {
        jsonObject.string("family")
    }
    override val given: List<String> by lazy {
        jsonObject.array("given") ?: listOf()
    }
}
