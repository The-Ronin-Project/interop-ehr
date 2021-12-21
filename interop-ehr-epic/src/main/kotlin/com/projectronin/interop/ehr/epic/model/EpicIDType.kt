package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.base.FHIRElement

/**
 * Epic's representation of and Identifier comprised of a Type and an ID.
 */
class EpicIDType(override val raw: String) : FHIRElement(raw), Identifier {
    override val system: String by lazy {
        jsonObject.string("Type")!!
    }

    override val value: String by lazy {
        jsonObject.string("ID")!!
    }
}
