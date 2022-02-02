package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.base.JSONElement

/**
 * Epic's representation of and Identifier comprised of a Type and an ID.
 */
class EpicIDType(override val element: IDType) : JSONElement(element), Identifier {
    override val system: String = element.type
    override val value: String = element.id
}
