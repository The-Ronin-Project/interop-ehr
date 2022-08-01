package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Participant
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.Identifier

class EpicPatientParticipant(
    override val element: String,
    private val identifier: Identifier?
) : JSONElement(element), Participant {
    override val actor: Reference by lazy {
        EpicPatientReference(element, identifier)
    }
}
