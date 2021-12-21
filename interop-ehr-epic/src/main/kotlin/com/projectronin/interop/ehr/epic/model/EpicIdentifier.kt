package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.base.FHIRElement

class EpicIdentifier(override val raw: String) : FHIRElement(raw), Identifier {
    override val system: String by lazy {
        jsonObject.string("system")!!
    }

    override val value: String by lazy {
        jsonObject.string("value") ?: ""
    }
}
