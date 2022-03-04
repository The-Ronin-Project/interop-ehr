package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.Reference as R4Reference

class EpicReference(override val element: R4Reference) : JSONElement(element), Reference {
    override val reference: String? = element.reference
    override val display: String? = element.display
    override val identifier: Identifier? by lazy {
        element.identifier?.let { EpicIdentifier(it) }
    }
    override val type: String? = element.type?.value
    override val id: String? = element.id
}
