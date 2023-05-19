package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.IdentifierGenerator
import com.projectronin.interop.fhir.generators.datatypes.identifier
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.test.data.generator.collection.ListDataGenerator
import java.util.UUID

class RoninIdentifiersGenerator : ListDataGenerator<Identifier>(0, IdentifierGenerator()) {
    fun setRoninIdentifiers(identifiers: ListDataGenerator<Identifier>, tenantId: String): ListDataGenerator<Identifier> {
        val interim = identifiers.generate()
        if (interim.none { it.system == CodeSystem.RONIN_FHIR_ID.uri }) {
            identifiers.plus(
                identifier {
                    system of CodeSystem.RONIN_FHIR_ID.uri
                    value of UUID.randomUUID().toString()
                }
            )
        }
        if (interim.none { it.system == CodeSystem.RONIN_TENANT.uri }) {
            identifiers.plus(
                identifier {
                    system of CodeSystem.RONIN_TENANT.uri
                    value of tenantId
                }
            )
        }
        return identifiers
    }
}
