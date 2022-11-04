package com.projectronin.interop.fhir.ronin.code

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code

/**
 * Access to common Ronin [CodeableConcept]s
 */
object RoninCodeableConcepts {
    val TENANT = CodeableConcept(
        text = "Ronin-specified Tenant Identifier",
        coding = listOf(
            Coding(
                system = RoninCodeSystem.TENANT.uri,
                code = Code("TID"),
                display = "Ronin-specified Tenant Identifier"
            )
        )
    )

    val MRN = CodeableConcept(
        text = "Medical Record Number",
        coding = listOf(
            Coding(
                system = CodeSystem.HL7_IDENTIFIER_TYPE.uri,
                code = Code("MRN"),
                display = "Medical Record Number"
            )
        )
    )

    val FHIR_ID = CodeableConcept(
        text = "FHIR Identifier",
        coding = listOf(
            Coding(
                system = RoninCodeSystem.FHIR_ID.uri,
                code = Code("FHIR ID"),
                display = "FHIR Identifier"
            )
        )
    )
}
