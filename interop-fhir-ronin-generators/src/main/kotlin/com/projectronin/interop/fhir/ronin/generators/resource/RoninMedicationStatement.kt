package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.resources.MedicationStatementGenerator
import com.projectronin.interop.fhir.generators.resources.medicationStatement
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.observation.subjectReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateDynamicValueReference
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmMedicationStatement(tenant: String, block: MedicationStatementGenerator.() -> Unit): MedicationStatement {
    return medicationStatement {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.MEDICATION_STATEMENT, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        status of generateCode(status.generate(), possibleMedicationStatementStatusCodes.random())
        medication of generateDynamicValueReference(
            medication.generate(),
            medicationReferenceOptions,
            tenant,
            "Medication"
        )
        subject of generateReference(subject.generate(), subjectReferenceOptions, tenant, "Patient")
    }
}

fun Patient.rcdmMedicationStatement(block: MedicationStatementGenerator.() -> Unit): MedicationStatement {
    val data = this.referenceData()
    return rcdmMedicationStatement(data.tenantId) {
        block.invoke(this)
        subject of generateReference(
            subject.generate(),
            subjectReferenceOptions,
            data.tenantId,
            "Patient",
            data.udpId
        )
    }
}

val possibleMedicationStatementStatusCodes = listOf(
    Code("active"),
    Code("completed"),
    Code("entered-in-error"),
    Code("intended"),
    Code("stopped"),
    Code("on-hold"),
    Code("unknown"),
    Code("not-taken")
)
