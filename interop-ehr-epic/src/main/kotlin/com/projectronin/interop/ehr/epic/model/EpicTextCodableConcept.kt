package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Coding
import com.projectronin.interop.ehr.model.base.JSONElement

class EpicTextCodableConcept(override val element: String) : JSONElement(element), CodeableConcept {
    override val text: String = element
    override val coding: List<Coding> = listOf()
}
