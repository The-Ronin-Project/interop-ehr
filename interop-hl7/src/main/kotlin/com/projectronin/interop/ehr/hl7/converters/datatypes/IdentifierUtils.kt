package com.projectronin.interop.ehr.hl7.converters.datatypes

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.datatype.CX
import com.projectronin.interop.fhir.r4.datatype.Identifier

fun Identifier.toPID3(message: Message, overrideNameSpace: String? = null): CX {
    val cx = CX(message)
    cx.idNumber.value = this.value?.value
    cx.assigningAuthority.namespaceID.value = overrideNameSpace ?: this.type?.text?.value
    return cx
}
