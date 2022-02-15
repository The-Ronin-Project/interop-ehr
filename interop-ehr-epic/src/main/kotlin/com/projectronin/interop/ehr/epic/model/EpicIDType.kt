package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.base.JSONElement

/**
 * Epic's representation of an Identifier comprised of a Type and an ID.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195) and similar.
 */
class EpicIDType(override val element: IDType) : JSONElement(element), Identifier {
    override val system: String? = null
    override val type: CodeableConcept = EpicCodeableConceptText(element.type)
    override val value: String = element.id
}
