package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept as R4CodeableConcept

class EpicCodeableConcept(override val element: R4CodeableConcept) : JSONElement(element), CodeableConcept {
    override val text: String? = element.text

    override val coding: List<EpicCoding> by lazy {
        element.coding.map(::EpicCoding)
    }
}
