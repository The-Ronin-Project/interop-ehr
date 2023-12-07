package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.datatypes.identifier
import com.projectronin.interop.fhir.generators.resources.PractitionerGenerator
import com.projectronin.interop.fhir.generators.resources.practitioner
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.generators.util.rcdmName
import com.projectronin.interop.fhir.ronin.generators.util.rcdmOptionalContactPoint
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import java.util.UUID

fun rcdmPractitioner(
    tenant: String,
    block: PractitionerGenerator.() -> Unit,
): Practitioner {
    return practitioner {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.PRACTITIONER, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        telecom of rcdmOptionalContactPoint(tenant, telecom).generate()
        name of rcdmName(name)
    }
}

/**
 * generates an NPI identifier. Caller supplies an [npiValue] or it is generated
 */
fun rcdmPractitionerNPI(npiValue: String? = null): Identifier {
    return identifier {
        system of CodeSystem.NPI.uri
        value of if (npiValue?.isNotEmpty() == true) npiValue else UUID.randomUUID().toString()
    }
}
