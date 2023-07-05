package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.datatypes.identifier
import com.projectronin.interop.fhir.generators.resources.PatientGenerator
import com.projectronin.interop.fhir.generators.resources.patient
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmContactPoint
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.generators.util.rcdmName
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.test.data.generator.collection.ListDataGenerator
import java.util.UUID

fun rcdmPatient(tenant: String, block: PatientGenerator.() -> Unit): Patient {
    return patient {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.PATIENT, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmMrn(identifier) +
                rcdmIdentifiers(tenant, identifier, it.value)
        }
        telecom of rcdmContactPoint(tenant, telecom).generate()
        name of rcdmName(name)
    }
}

internal fun rcdmMrn(identifiers: ListDataGenerator<Identifier>): List<Identifier> {
    val generatedIdentifiers = identifiers.generate()
    return if (generatedIdentifiers.none { it.system == CodeSystem.RONIN_MRN.uri && it.type == CodeableConcepts.RONIN_MRN }) {
        listOf(
            identifier {
                system of CodeSystem.RONIN_MRN.uri
                value of UUID.randomUUID().toString()
                type of CodeableConcepts.RONIN_MRN
            }
        )
    } else {
        emptyList()
    }
}

/**
 * gets the data for generating a Reference to the Patient from another resource
 */
internal fun Patient.referenceData(): PatientReferenceData {
    val udpId = this.id?.value ?: ""
    val tenantId = this.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value ?: ""
    if (tenantId.isEmpty() || udpId.isEmpty()) {
        throw IllegalArgumentException("Patient is missing some required data")
    }
    return PatientReferenceData(tenantId, udpId)
}
data class PatientReferenceData(val tenantId: String, val udpId: String)
