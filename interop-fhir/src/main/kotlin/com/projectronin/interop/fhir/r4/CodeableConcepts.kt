package com.projectronin.interop.fhir.r4

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code

/**
 * Provides access to common [CodeableConcept]s.
 */
class CodeableConcepts {
    companion object {
        val RONIN_TENANT = CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.RONIN_TENANT.uri,
                    code = Code("TID"),
                    display = "Ronin-specified Tenant Identifier"
                )
            ),
            text = "Tenant ID"
        )

        val SER = CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.SER.uri,
                    code = Code("SER"),
                    display = "Provider SER Identifier"
                )
            ),
            text = "Provider SER Identifier"
        )

        val MRN = CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.MRN.uri,
                    code = Code("MR"),
                    display = "Medical Record Number"
                )
            ),
            text = "MRN"
        )

        val FHIR_STU3_ID = CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    code = Code("STU3"),
                    display = "FHIR STU3 ID"
                )
            ),
            text = "FHIR STU3"
        )

        val RONIN_ID = CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.RONIN_ID.uri,
                    code = Code("RONIN_ID"),
                    display = "Ronin ID"
                )
            ),
            text = "Ronin ID"
        )
    }
}
