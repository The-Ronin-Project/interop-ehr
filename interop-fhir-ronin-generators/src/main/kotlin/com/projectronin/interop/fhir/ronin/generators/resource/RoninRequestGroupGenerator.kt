package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.resources.RequestGroupGenerator
import com.projectronin.interop.fhir.generators.resources.requestGroup
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.fhir.ronin.generators.resource.observation.subjectReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmRequestGroup(
    tenant: String,
    block: RequestGroupGenerator.() -> Unit,
): RequestGroup {
    return requestGroup {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.REQUEST_GROUP, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        status of generateCode(status.generate(), possibleRequestGroupStatusCodes.random())
        intent of generateCode(intent.generate(), possibleRequestGroupIntentCodes.random())
        subject of generateReference(subject.generate(), subjectRequestGroupReferenceOptions, tenant, "Patient")
    }
}

fun Patient.rcdmRequestGroup(block: RequestGroupGenerator.() -> Unit): RequestGroup {
    val data = this.referenceData()
    return rcdmRequestGroup(data.tenantId) {
        block.invoke(this)
        subject of
            generateReference(
                subject.generate(),
                subjectReferenceOptions,
                data.tenantId,
                "Patient",
                data.udpId,
            )
    }
}

val possibleRequestGroupStatusCodes =
    listOf(
        Code("draft"),
        Code("active"),
        Code("on-hold"),
        Code("revoked"),
        Code("completed"),
        Code("entered-in-error"),
        Code("unknown"),
    )

val possibleRequestGroupIntentCodes =
    listOf(
        Code("proposal"),
        Code("plan"),
        Code("directive"),
        Code("order"),
        Code("original-order"),
        Code("reflex-order"),
        Code("filler-order"),
        Code("instance-order"),
        Code("option"),
    )

val subjectRequestGroupReferenceOptions =
    listOf(
        "Group",
        "Patient",
    )

val possibleRequestGroupActionConditionKindCodes =
    listOf(
        Code("applicability"),
        Code("start"),
        Code("stop"),
    )

val possibleRequestGroupActionRelationshipCodes =
    listOf(
        Code("before-start"),
        Code("before"),
        Code("before-end"),
        Code("concurrent-with-start"),
        Code("concurrent"),
        Code("concurrent-with-end"),
        Code("after-start"),
        Code("after"),
        Code("after-end"),
    )
