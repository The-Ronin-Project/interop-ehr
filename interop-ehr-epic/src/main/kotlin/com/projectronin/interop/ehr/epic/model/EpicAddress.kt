package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Address
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.valueset.AddressUse
import com.projectronin.interop.fhir.r4.datatype.Address as R4Address

class EpicAddress(override val element: R4Address) : JSONElement(element), Address {
    override val use: AddressUse? = element.use
    override val line: List<String> = element.line
    override val city: String? = element.city
    override val state: String? = element.state
    override val postalCode: String? = element.postalCode
}
