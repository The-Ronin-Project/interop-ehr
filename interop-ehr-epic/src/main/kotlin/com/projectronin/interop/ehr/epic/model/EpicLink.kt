package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.BundleLink
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

class EpicLink(override val element: BundleLink) : JSONElement(element), Link {
    override val relation: String = element.relation
    override val url: Uri = element.url
}
