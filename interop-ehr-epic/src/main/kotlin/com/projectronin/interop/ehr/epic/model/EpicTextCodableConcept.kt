package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Coding
import com.projectronin.interop.ehr.model.base.FHIRElement

class EpicTextCodableConcept(override val raw: String) : FHIRElement(raw), CodeableConcept {

    override val coding: List<Coding> = listOf()

    override val text: String by lazy {
        // Since we are dealing with a freetext concept, the raw is simply the text.
        raw
    }
}
