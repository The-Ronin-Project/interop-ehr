package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.ContactPoint
import com.projectronin.interop.ehr.model.base.FHIRElement
import com.projectronin.interop.ehr.model.helper.enum
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse

class EpicContactPoint(override val raw: String) : FHIRElement(raw), ContactPoint {
    override val system: ContactPointSystem? by lazy {
        jsonObject.enum<ContactPointSystem>("system")
    }
    override val use: ContactPointUse? by lazy {
        jsonObject.enum<ContactPointUse>("use")
    }
    override val value: String? by lazy {
        jsonObject.string("value")
    }
}
