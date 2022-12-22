package com.projectronin.interop.ehr.epic.apporchard.model.converters

import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString

/***
 * standardized way to turn an Epic [IDType] API Response object into a FHIR [Identifier]
 */
fun IDType.toIdentifier(): Identifier {
    return Identifier(value = FHIRString(id), type = CodeableConcept(text = FHIRString(type)))
}
