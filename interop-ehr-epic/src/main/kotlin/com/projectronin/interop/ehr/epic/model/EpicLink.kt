package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.FHIRElement
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

class EpicLink(override val raw: String) : FHIRElement(raw), Link {
    override val relation: String by lazy {
        jsonObject.string("relation")!!
    }

    override val url: Uri by lazy {
        Uri(jsonObject.string("url")!!)
    }
}
