package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.ContactPoint
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.datatype.ContactPoint as R4ContactPoint

class EpicContactPoint(override val element: R4ContactPoint) : JSONElement(element), ContactPoint {
    override val system: ContactPointSystem? = element.system
    override val use: ContactPointUse? = element.use
    override val value: String? = element.value
}
