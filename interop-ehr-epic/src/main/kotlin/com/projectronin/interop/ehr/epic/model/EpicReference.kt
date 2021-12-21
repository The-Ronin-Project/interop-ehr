package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.FHIRElement

class EpicReference(override val raw: String) : FHIRElement(raw), Reference {
    override val reference: String by lazy {
        jsonObject.string("reference")!!
    }

    override val display: String? by lazy {
        jsonObject.string("display")
    }
}
