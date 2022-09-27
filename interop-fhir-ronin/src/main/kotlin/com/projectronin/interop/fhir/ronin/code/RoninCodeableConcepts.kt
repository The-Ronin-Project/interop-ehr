package com.projectronin.interop.fhir.ronin.code

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code

/**
 * Provides access to common [CodeableConcept]s.
 */
object RoninCodeableConcepts {
    val TENANT = CodeableConcept(
        coding = listOf(
            Coding(
                system = RoninCodeSystem.TENANT.uri,
                code = Code("TID"),
                display = "Ronin-specified Tenant Identifier"
            )
        )
    )

    val MRN = CodeableConcept(
        coding = listOf(
            Coding(
                system = CodeSystem.HL7_IDENTIFIER_TYPE.uri,
                code = Code("MRN"),
                display = "Medical Record Number"
            )
        )
    )

    val FHIR_ID = CodeableConcept(
        coding = listOf(
            Coding(
                system = RoninCodeSystem.FHIR_ID.uri,
                code = Code("FHIR ID"),
                display = "FHIR Identifier"
            )
        )
    )
}