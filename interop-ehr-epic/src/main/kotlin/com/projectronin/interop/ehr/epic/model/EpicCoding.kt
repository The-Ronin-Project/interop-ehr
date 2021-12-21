package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Coding
import com.projectronin.interop.ehr.model.base.FHIRElement

class EpicCoding(override val raw: String) : FHIRElement(raw), Coding {
    override val system: String? by lazy {
        jsonObject.string("system")
    }

    override val version: String? by lazy {
        jsonObject.string("version")
    }

    override val code: String? by lazy {
        jsonObject.string("code")
    }

    override val display: String? by lazy {
        jsonObject.string("display")
    }

    override val userSelected: Boolean? by lazy {
        jsonObject.boolean("userSelected")
    }
}
