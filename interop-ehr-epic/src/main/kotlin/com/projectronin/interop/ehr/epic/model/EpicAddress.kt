package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.Address
import com.projectronin.interop.ehr.model.base.FHIRElement
import com.projectronin.interop.ehr.model.helper.enum
import com.projectronin.interop.fhir.r4.valueset.AddressUse

class EpicAddress(override val raw: String) : FHIRElement(raw), Address {
    override val use: AddressUse? by lazy {
        jsonObject.enum<AddressUse>("use")
    }

    override val line: List<String> by lazy {
        jsonObject.array("line") ?: listOf()
    }

    override val city: String? by lazy {
        jsonObject.string("city")
    }

    override val state: String? by lazy {
        jsonObject.string("state")
    }

    override val postalCode: String? by lazy {
        jsonObject.string("postalCode")
    }
}
