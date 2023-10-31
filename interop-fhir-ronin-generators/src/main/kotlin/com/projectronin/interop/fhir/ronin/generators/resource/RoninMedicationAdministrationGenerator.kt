package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.resources.MedicationAdministrationGenerator
import com.projectronin.interop.fhir.generators.resources.medicationAdministration
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.observation.subjectReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.util.generateDynamicValueReference
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmMedicationAdministration(
    tenant: String,
    block: MedicationAdministrationGenerator.() -> Unit
): MedicationAdministration {
    return medicationAdministration {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.MEDICATION_ADMINISTRATION, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        extension.plus(originalMedicationDatatype()).plus(originalMedicationAdministrationStatus())
        subject of generateReference(subject.generate(), listOf("Patient"), tenant, "Patient")
        medication of generateDynamicValueReference(medication.generate(), listOf("Medication"), tenant, "Medication")
    }
}

fun Patient.rcdmMedicationAdministration(block: MedicationAdministrationGenerator.() -> Unit): MedicationAdministration {
    val data = this.referenceData()
    return rcdmMedicationAdministration(data.tenantId) {
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

val possibleMedicationDatatypeCodes = listOf(
    Code("literal reference"),
    Code("logical reference"),
    Code("contained reference"),
    Code("codeable concept")
)

fun originalMedicationDatatype(): Extension {
    return Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/originalMedicationDatatype"),
        value = DynamicValue(
            DynamicValueType.CODE,
            possibleMedicationDatatypeCodes.random()
        )
    )
}

fun originalMedicationAdministrationStatus(): Extension {
    return Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value),
        value = DynamicValue(
            DynamicValueType.CODE,
            Code("original-status")
        )
    )
}
