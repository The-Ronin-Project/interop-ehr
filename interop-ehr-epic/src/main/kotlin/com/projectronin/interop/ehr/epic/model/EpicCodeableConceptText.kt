package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.base.JSONElement

/**
 * Epic representation of a CodeableConcept that can be constructed from an Epic AppOrchard ID Type.
 */
class EpicCodeableConceptText(override val element: String) : JSONElement(element), CodeableConcept {
    override val text: String = element
    override val coding: List<EpicCoding> = emptyList()
}
