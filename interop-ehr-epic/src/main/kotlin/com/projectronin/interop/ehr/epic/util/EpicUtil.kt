package com.projectronin.interop.ehr.epic.util

import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier

/***
 * standardized way to turn an Epic [IDType] API Response object into a FHIR [Identifier]
 */
fun IDType.toIdentifier(): Identifier {
    return Identifier(value = this.id, type = CodeableConcept(text = this.type))
}
