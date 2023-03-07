package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.GetFHIRIDResponse
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import datadog.trace.api.Trace
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.projectronin.interop.aidbox.PatientService as AidboxPatientService

@Component
class CernerPatientService(cernerClient: CernerClient, private val aidboxPatientService: AidboxPatientService) : PatientService, CernerFHIRService<Patient>(cernerClient) {
    override val fhirURLSearchPart = "/Patient"
    override val fhirResourceType = Patient::class.java
    private val logger = KotlinLogging.logger { }

    override fun findPatient(
        tenant: Tenant,
        birthDate: LocalDate,
        givenName: String,
        familyName: String
    ): List<Patient> {
        logger.info { "Patient search started for ${tenant.mnemonic}" }

        val parameters = mapOf(
            "given" to givenName,
            "family:exact" to familyName,
            "birthdate" to DateTimeFormatter.ofPattern("yyyy-MM-dd").format(birthDate)
        )
        val patientList = getResourceListFromSearch(tenant, parameters)

        logger.info { "Patient search completed for ${tenant.mnemonic}" }
        return patientList
    }

    @Trace
    override fun <K> findPatientsById(tenant: Tenant, patientIdsByKey: Map<K, Identifier>): Map<K, Patient> {
        logger.info { "Patient find by id started for ${tenant.mnemonic} with ${patientIdsByKey.size} patients requested" }

        // Gather the full batch of identifiers to request.
        val patientIdentifiers = patientIdsByKey.filter { entry ->
            val systemFound = entry.value.system != null
            if (!systemFound) logger.warn { "System missing on key, ${entry.key}. Key was removed." }
            systemFound
        }

        val patientList = patientIdentifiers.values.toSet().mapNotNull {
            val identifierParam = "${it.system!!.value}|${it.value!!.value}"
            // Cerner only allows searching for one identifier at a time
            // which means we should only be getting back one patient per call here
            // .single() ensures that's the case
            getResourceListFromSearch(
                tenant,
                mapOf("identifier" to identifierParam)
            ).singleOrNull()
        }

        // Index patients found based on identifiers
        val foundPatientsByIdentifier = patientList.flatMap { patient ->
            patient.identifier.map { identifier ->
                SystemValueIdentifier(
                    systemText = identifier.system?.value?.uppercase(),
                    value = identifier.value?.value
                ) to patient
            }
        }.toMap()

        // Re-key to the request based on requested identifier
        val patientsFoundByKey = patientIdentifiers.mapNotNull { requestEntry ->
            val foundPatient = foundPatientsByIdentifier[
                SystemValueIdentifier(
                    systemText = requestEntry.value.system!!.value!!.uppercase(),
                    value = requestEntry.value.value!!.value
                )
            ]
            if (foundPatient != null) requestEntry.key to foundPatient else null
        }.toMap()

        logger.info { "Patient find by id for ${tenant.mnemonic} found ${patientsFoundByKey.size} patients" }
        return patientsFoundByKey
    }

    @Trace
    override fun getPatient(tenant: Tenant, patientFHIRID: String): Patient {
        return runBlocking { getByID(tenant, patientFHIRID) }
    }

    @Trace
    override fun getPatientFHIRId(tenant: Tenant, patientIDValue: String): String {
        return getPatientsFHIRIds(
            tenant,
            tenant.vendorAs<Cerner>().patientMRNSystem,
            listOf(patientIDValue)
        )[patientIDValue]!!.fhirID
    }

    fun getPatientsFHIRIds(
        tenant: Tenant,
        patientIDSystem: String,
        patientIDValues: List<String>
    ): Map<String, GetFHIRIDResponse> {
        // Try the list of patients against Aidbox first
        val aidboxResponse = aidboxPatientService.getPatientFHIRIds(
            tenantMnemonic = tenant.mnemonic,
            patientIDValues.associateWith { SystemValue(it, patientIDSystem) }
        ).mapValues { GetFHIRIDResponse(it.value) }

        // Search for any patients that weren't in Aidbox in the EHR.  If there aren't any, return the Aidbox patients.
        val ehrPatientIDValues = patientIDValues.filterNot { patientID ->
            aidboxResponse.keys.contains(patientID)
        }
        if (ehrPatientIDValues.isEmpty()) return aidboxResponse

        val ehrResponse = findPatientsById(
            tenant = tenant,
            ehrPatientIDValues.associateWith { Identifier(value = FHIRString(it), system = Uri(patientIDSystem)) }
        ).filterNot {
            it.value.id == null
        }.mapValues { GetFHIRIDResponse(it.value.id!!.value!!, it.value) }

        return aidboxResponse + ehrResponse
    }

    data class SystemValueIdentifier(val systemText: String?, val value: String?)
}
