package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.resources.LocationGenerator
import com.projectronin.interop.fhir.generators.resources.location
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateStringOrDAR
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmContactPoint
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmLocation(
    tenant: String,
    block: LocationGenerator.() -> Unit,
): Location {
    return location {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.LOCATION, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        status of generateCode(status.generate(), possibleLocationStatus.random())
        name of generateStringOrDAR(name.generate()).toString()
        mode of generateCode(mode.generate(), modeValues.random())
        telecom of rcdmContactPoint(tenant, telecom).generate()
    }
}

val possibleLocationStatus =
    listOf(
        Code("active"),
        Code("suspended"),
        Code("inactive"),
    )

val modeValues =
    listOf(
        Code("instance"),
        Code("kind"),
    )
