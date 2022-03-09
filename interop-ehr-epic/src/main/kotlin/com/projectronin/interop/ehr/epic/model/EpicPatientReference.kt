package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.ReferenceTypes
import com.projectronin.interop.ehr.model.base.JSONElement

class EpicPatientReference(
    override val element: String,
    override val identifier: Identifier?
) : JSONElement(element), Reference {
    override val reference: String? = null
    override val type: String = ReferenceTypes.PATIENT
    override val display: String = element
    override val id: String? = null
}
