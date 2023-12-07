package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.IdentifierGenerator
import com.projectronin.interop.fhir.generators.datatypes.identifier
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.test.data.generator.collection.ListDataGenerator

fun rcdmIdentifiers(
    tenantId: String,
    identifiers: ListDataGenerator<Identifier>,
    udpIdValue: String? = null,
): List<Identifier> {
    val generatedIdentifiers = identifiers.generate()
    val roninIdentifiers = ListDataGenerator(0, IdentifierGenerator())

    if (generatedIdentifiers.none { it.system == CodeSystem.RONIN_FHIR_ID.uri }) {
        roninIdentifiers.plus(
            identifier {
                system of CodeSystem.RONIN_FHIR_ID.uri
                value of fhirIdValue(udpIdValue, tenantId)
                type of CodeableConcepts.RONIN_FHIR_ID
            },
        )
    }
    if (generatedIdentifiers.none { it.system == CodeSystem.RONIN_TENANT.uri }) {
        roninIdentifiers.plus(
            identifier {
                system of CodeSystem.RONIN_TENANT.uri
                value of tenantId
                type of CodeableConcepts.RONIN_TENANT
            },
        )
    }
    if (generatedIdentifiers.none { it.system == CodeSystem.RONIN_DATA_AUTHORITY.uri }) {
        roninIdentifiers.plus(
            identifier {
                system of CodeSystem.RONIN_DATA_AUTHORITY.uri
                value of "EHR Data Authority"
                type of CodeableConcepts.RONIN_DATA_AUTHORITY_ID
            },
        )
    }
    return generatedIdentifiers + roninIdentifiers.generate()
}
