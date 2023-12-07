package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR

fun generateStringOrDAR(name: String): FHIRString {
    if (name.isNotEmpty()) return name.asFHIR()
    return FHIRString(
        value = null,
        extension =
            listOf(
                Extension(
                    url = Uri("http://hl7.org/fhir/StructureDefinition/data-absent-reason"),
                    value = DynamicValue(DynamicValueType.CODE, Code("unknown")),
                ),
            ),
    )
}
