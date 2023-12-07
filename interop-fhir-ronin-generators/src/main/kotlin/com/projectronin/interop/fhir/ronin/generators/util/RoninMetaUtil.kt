package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.MetaGenerator
import com.projectronin.interop.fhir.generators.datatypes.meta
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmMeta(
    roninProfile: RoninProfile,
    tenantId: String,
    block: MetaGenerator.() -> Unit,
): Meta {
    return meta {
        block.invoke(this)
        source of Uri(tenantId)
        profile of listOf(Canonical(roninProfile.value))
    }
}
