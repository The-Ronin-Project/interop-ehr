package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Participant
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.JSONElement

class EpicPatientParticipant(
    override val element: String,
    private val identifier: Identifier?
) : JSONElement(element), Participant {
    override val actor: Reference by lazy {
        EpicPatientReference(element, identifier)
    }
}
