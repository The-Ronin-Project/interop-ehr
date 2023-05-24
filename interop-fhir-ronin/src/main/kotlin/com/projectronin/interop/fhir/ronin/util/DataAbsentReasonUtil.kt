package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * A Data Absent Reason Extension used to indicate that data is absent for an unknown reason.
 */
val dataAbsentReasonExtension = listOf(
    Extension(
        url = Uri("http://hl7.org/fhir/StructureDefinition/data-absent-reason"),
        value = DynamicValue(
            type = DynamicValueType.CODE,
            value = Code("unknown")
        )
    )
)
