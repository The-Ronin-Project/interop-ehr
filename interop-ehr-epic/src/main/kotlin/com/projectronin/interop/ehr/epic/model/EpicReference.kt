package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.Reference as R4Reference

class EpicReference(override val element: R4Reference) : JSONElement(element), Reference {
    override val reference: String = element.reference!!
    override val display: String? = element.display
    override val identifier: Identifier? = null
    override val type: Reference.ReferenceType? = null
    override val id: Id? = null
}
