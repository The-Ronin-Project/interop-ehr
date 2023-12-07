package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.resources.CarePlanGenerator
import com.projectronin.interop.fhir.generators.resources.carePlan
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.observation.subjectReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmCarePlan(
    tenant: String,
    block: CarePlanGenerator.() -> Unit,
): CarePlan {
    return carePlan {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.CARE_PLAN, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        status of generateCode(status.generate(), possibleCarePlanStatusCodes.random())
        intent of generateCode(intent.generate(), possibleCarePlanIntentCodes.random())
        subject of generateReference(subject.generate(), subjectReferenceOptions, tenant, "Patient")
    }
}

fun Patient.rcdmCarePlan(block: CarePlanGenerator.() -> Unit): CarePlan {
    val data = this.referenceData()
    return rcdmCarePlan(data.tenantId) {
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

val possibleCarePlanStatusCodes =
    listOf(
        Code("draft"),
        Code("active"),
        Code("on-hold"),
        Code("revoked"),
        Code("completed"),
        Code("entered-in-error"),
        Code("unknown"),
    )

val possibleCarePlanIntentCodes =
    listOf(
        Code("proposal"),
        Code("plan"),
        Code("order"),
        Code("option"),
    )
