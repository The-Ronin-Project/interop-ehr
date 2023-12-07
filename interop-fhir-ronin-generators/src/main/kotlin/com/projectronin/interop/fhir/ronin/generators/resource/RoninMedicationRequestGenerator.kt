package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.resources.MedicationRequestGenerator
import com.projectronin.interop.fhir.generators.resources.medicationRequest
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.observation.subjectReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateDynamicValueReference
import com.projectronin.interop.fhir.ronin.generators.util.generateOptionalDynamicValueReference
import com.projectronin.interop.fhir.ronin.generators.util.generateOptionalReference
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmMedicationRequest(
    tenant: String,
    block: MedicationRequestGenerator.() -> Unit,
): MedicationRequest {
    return medicationRequest {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.MEDICATION_REQUEST, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        extension.plus(originalMedicationDatatype())
        status of generateCode(status.generate(), possibleMedicationRequestStatusCodes.random())
        intent of generateCode(intent.generate(), possibleMedicationRequestIntentCodes.random())
        reported of generateOptionalDynamicValueReference(reported.generate(), reportedReferenceOptions, tenant)
        medication of
            generateDynamicValueReference(
                medication.generate(),
                medicationReferenceOptions,
                tenant,
                "Medication",
            )
        subject of generateReference(subject.generate(), subjectReferenceOptions, tenant, "Patient")
        encounter of generateOptionalReference(encounter.generate(), encounterReferenceOptions, tenant)
        requester of generateReference(requester.generate(), requesterReferenceOptions, tenant)
    }
}

fun Patient.rcdmMedicationRequest(block: MedicationRequestGenerator.() -> Unit): MedicationRequest {
    val data = this.referenceData()
    return rcdmMedicationRequest(data.tenantId) {
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

val reportedReferenceOptions =
    listOf(
        "Practitioner",
        "Organization",
        "Patient",
        "PractitionerRole",
    )

val medicationReferenceOptions =
    listOf(
        "Medication",
    )
val encounterReferenceOptions =
    listOf(
        "Encounter",
    )

val requesterReferenceOptions =
    listOf(
        "Patient",
        "Practitioner",
        "PractitionerRole",
        "Organization",
        "RelatedPerson",
        "Device",
    )

val performerReferenceOptions =
    listOf(
        "Organization",
        "PractitionerRole",
        "Practitioner",
        "Patient",
        "CareTeam",
        "RelatedPerson",
        "Device",
    )

val recorderReferenceOptions =
    listOf(
        "PractitionerRole",
        "Practitioner",
    )

val reasonReferenceOptions =
    listOf(
        "Condition",
        "Observation",
    )

val basedOnReferenceOptions =
    listOf(
        "CarePlan",
        "MedicationRequest",
        "ServiceRequest",
        "ImmunizationRecommendation",
    )

val insuranceReferenceOptions =
    listOf(
        "Coverage",
        "ClaimResponse",
    )

val priorPresciptionReferenceOptions =
    listOf(
        "MedicationRequest",
    )

val detectedIssueReferenceOptions =
    listOf(
        "DetectedIssue",
    )

val eventHistoryReferenceOptions =
    listOf(
        "Provenance",
    )

val possibleMedicationRequestStatusCodes =
    listOf(
        Code("active"),
        Code("on-hold"),
        Code("cancelled"),
        Code("completed"),
        Code("entered-in-error"),
        Code("stopped"),
        Code("draft"),
        Code("unknown"),
    )

val possibleMedicationRequestIntentCodes =
    listOf(
        Code("proposal"),
        Code("plan"),
        Code("order"),
        Code("original-order"),
        Code("reflex-order"),
        Code("filler-order"),
        Code("instance-order"),
        Code("option"),
    )

val possibleMedicationRequestCategoryCodes =
    listOf(
        Code("inpatient"),
        Code("outpatient"),
        Code("community"),
        Code("discharge"),
    )

val possibleMedicationRequestPriorityCodes =
    listOf(
        Code("routine"),
        Code("urgent"),
        Code("asap"),
        Code("stat"),
    )

val dispenseRequestPerformerReferenceOptions =
    listOf(
        "Organization",
    )
