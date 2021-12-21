package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.base.FHIRElement
import com.projectronin.interop.ehr.model.helper.fhirElementList

class EpicCodeableConcept(override val raw: String) : FHIRElement(raw), CodeableConcept {
    override val coding: List<EpicCoding> by lazy {
        jsonObject.fhirElementList("coding", ::EpicCoding)
    }

    override val text: String? by lazy {
        jsonObject.string("text")
    }
}
