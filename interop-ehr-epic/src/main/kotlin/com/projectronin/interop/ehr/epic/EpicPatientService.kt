package com.projectronin.interop.ehr.epic

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.ehr.dataauthority.models.IdentifierSearchableResourceTypes
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.GetFHIRIDResponse
import com.projectronin.interop.ehr.util.associateFHIRId
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import datadog.trace.api.Trace
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.projectronin.ehr.dataauthority.models.Identifier as EHRDAIdentifier

/**
 * Service providing access to patients within Epic.
 */
@Component
class EpicPatientService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:5}") private val batchSize: Int,
    private val ehrdaClient: EHRDataAuthorityClient
) : PatientService,
    EpicFHIRService<Patient>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Patient"
    override val fhirResourceType = Patient::class.java
    private val logger = KotlinLogging.logger { }

    @Trace
    override fun findPatient(
        tenant: Tenant,
        birthDate: LocalDate,
        givenName: String,
        familyName: String
    ): List<Patient> {
        return findPatient(tenant, birthDate, givenName, familyName, false)
    }

    internal fun findPatient(
        tenant: Tenant,
        birthDate: LocalDate,
        givenName: String,
        familyName: String,
        disableRetry: Boolean
    ): List<Patient> {
        logger.info { "Patient search started for ${tenant.mnemonic}" }

        val parameters = mapOf(
            "given" to givenName,
            "family" to familyName,
            "birthdate" to DateTimeFormatter.ofPattern("yyyy-MM-dd").format(birthDate)
        )
        val patientList = getResourceListFromSearch(tenant, parameters, disableRetry)

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
        }.values.toSet()

        // Chunk the identifiers and run the search
        val patientList = patientIdentifiers.chunked(batchSize) {
            val identifierParam = it.joinToString(separator = ",") { patientIdentifier ->
                "${patientIdentifier.system?.value}|${patientIdentifier.value!!.value}"
            }
            getResourceListFromSearch(
                tenant,
                mapOf("identifier" to identifierParam)
            )
        }.flatten()

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
        val patientsFoundByKey = patientIdsByKey.mapNotNull { requestEntry ->
            val foundPatient = foundPatientsByIdentifier[
                SystemValueIdentifier(
                    systemText = requestEntry.value.system?.value?.uppercase(),
                    value = requestEntry.value.value?.value
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
        val patientFhirId =
            getPatientsFHIRIds(tenant, tenant.vendorAs<Epic>().patientMRNSystem, listOf(patientIDValue))[patientIDValue]

        if (patientFhirId == null) {
            logger.error { "No patient FHIR ID found for patient with ID $patientIDValue in tenant ${tenant.mnemonic}" }
            throw VendorIdentifierNotFoundException("No FHIR ID found for patient")
        }

        return patientFhirId.fhirID
    }

    fun getPatientsFHIRIds(
        tenant: Tenant,
        patientIDSystem: String,
        patientIDValues: List<String>
    ): Map<String, GetFHIRIDResponse> {
        // Try the list of patients against EHRDA first
        val ehrdaResponse = runBlocking {
            ehrdaClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                patientIDValues.map { EHRDAIdentifier(value = it, system = patientIDSystem) }
            ).associateFHIRId().map { (key, value) -> key.value to GetFHIRIDResponse(value) }.toMap()
        }
        // Search for any patients that weren't in EHRDA in the EHR.  If there aren't any, return the EHRDA patients.
        val ehrPatientIDValues = patientIDValues.filterNot { patientID ->
            ehrdaResponse.keys.contains(patientID)
        }
        if (ehrPatientIDValues.isEmpty()) return ehrdaResponse

        val ehrResponse = findPatientsById(
            tenant = tenant,
            ehrPatientIDValues.associateWith { Identifier(value = FHIRString(it), system = Uri(patientIDSystem)) }
        ).filterNot {
            it.value.id == null
        }.mapValues { GetFHIRIDResponse(it.value.id!!.value!!, it.value) }

        return ehrdaResponse + ehrResponse
    }

    data class SystemValueIdentifier(val systemText: String?, val value: String?)
}
