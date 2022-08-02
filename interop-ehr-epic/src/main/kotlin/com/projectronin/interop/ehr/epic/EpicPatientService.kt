package com.projectronin.interop.ehr.epic

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.GetFHIRIDResponse
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.util.unlocalize
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.projectronin.interop.aidbox.PatientService as AidboxPatientService
import com.projectronin.interop.fhir.r4.resource.Bundle as R4Bundle

/**
 * Service providing access to patients within Epic.
 */
@Component
class EpicPatientService(
    private val epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:100}") private val batchSize: Int,
    private val aidboxPatientService: AidboxPatientService,
) : PatientService,
    EpicPagingService(epicClient) {
    private val patientSearchUrlPart = "/api/FHIR/R4/Patient"
    private val logger = KotlinLogging.logger { }

    override fun findPatient(
        tenant: Tenant,
        birthDate: LocalDate,
        givenName: String,
        familyName: String,
    ): List<Patient> {
        logger.info { "Patient search started for ${tenant.mnemonic}" }

        val parameters = mapOf(
            "given" to givenName,
            "family" to familyName,
            "birthdate" to DateTimeFormatter.ofPattern("yyyy-MM-dd").format(birthDate)
        )
        val bundle = runBlocking {
            val httpResponse = epicClient.get(tenant, patientSearchUrlPart, parameters)
            if (httpResponse.status != HttpStatusCode.OK) {
                logger.error { "Patient search failed for ${tenant.mnemonic}, with a ${httpResponse.status}" }
                throw IOException("Call to tenant ${tenant.mnemonic} failed with a ${httpResponse.status}")
            }
            httpResponse.body<R4Bundle>()
        }

        logger.info { "Patient search completed for ${tenant.mnemonic}" }
        return bundle.toListOfType()
    }

    override fun <K> findPatientsById(tenant: Tenant, patientIdsByKey: Map<K, Identifier>): Map<K, Patient> {
        logger.info { "Patient find by id started for ${tenant.mnemonic} with ${patientIdsByKey.size} patients requested" }

        // Gather the full batch of identifiers to request.
        val patientIdentifiers = patientIdsByKey.filter { entry ->
            val systemFound = entry.value.system != null
            if (!systemFound) logger.warn { "System missing on key, ${entry.key}. Key was removed." }
            systemFound
        }.values.toSet()

        // Chunk the identifiers and run the search
        val patientsFound = patientIdentifiers.chunked(batchSize) {
            val identifierParam = it.joinToString(separator = ",") { patientIdentifier ->
                "${patientIdentifier.system?.value}|${patientIdentifier.value}"
            }
            getBundleWithPaging(
                tenant,
                patientSearchUrlPart,
                mapOf("identifier" to identifierParam)
            )
        }

        // Translate to Patients
        val patientList = mergeResponses(patientsFound).toListOfType<Patient>()
        // Index patients found based on identifiers
        val foundPatientsByIdentifier = patientList.flatMap { patient ->
            patient.identifier.map { identifier ->
                SystemValueIdentifier(
                    systemText = identifier.system?.value?.uppercase(),
                    value = identifier.value
                ) to patient
            }
        }.toMap()

        // Re-key to the request based on requested identifier
        val patientsFoundByKey = patientIdsByKey.mapNotNull { requestEntry ->
            val foundPatient = foundPatientsByIdentifier[
                SystemValueIdentifier(
                    systemText = requestEntry.value.system?.value?.uppercase(), value = requestEntry.value.value
                )
            ]
            if (foundPatient != null) requestEntry.key to foundPatient else null
        }.toMap()

        logger.info { "Patient find by id for ${tenant.mnemonic} found ${patientsFoundByKey.size} patients" }
        return patientsFoundByKey
    }

    /**
     * Finds a Patient's FHIR ID (non-localized), based on the Epic Identifer (MRN or Internal). Searches Aidbox first before querying the EHR.
     * If a patient is found in the EHR, this will return the [Patient] object that was found to save on future queries.
     * Returns a [GetFHIRIDResponse]
     */
    override fun getPatientFHIRId(tenant: Tenant, patientIDValue: String, patientIDSystem: String): GetFHIRIDResponse {
        val patientID = SystemValue(patientIDValue, patientIDSystem)
        // try Aidbox first
        val fhirID = runCatching {
            aidboxPatientService.getPatientFHIRIds(tenantMnemonic = tenant.mnemonic, mapOf("key" to patientID))
                .getValue("key")
        }.getOrNull()
        fhirID?.let { return GetFHIRIDResponse(it.unlocalize(tenant)) }

        // else try EHR
        val parameters = mapOf("identifier" to patientID.queryString)
        val bundle = runBlocking {
            val httpResponse = epicClient.get(tenant, patientSearchUrlPart, parameters)
            if (httpResponse.status != HttpStatusCode.OK) {
                logger.error { "Patient search failed for ${tenant.mnemonic}, with a ${httpResponse.status}" }
                throw IOException("Call to tenant ${tenant.mnemonic} failed with a ${httpResponse.status}")
            }
            httpResponse.body<R4Bundle>()
        }
        val patList = bundle.toListOfType<Patient>()
        if (patList.size != 1) {
            logger.error { "Multiple patients found in ${tenant.mnemonic} for MRN value ${patientID.value}." }
            throw IOException("Multiple patients found in ${tenant.mnemonic} for MRN value ${patientID.value}.")
        }
        val patient = patList.first()
        return GetFHIRIDResponse(patient.id!!.value, patient)
    }

    data class SystemValueIdentifier(val systemText: String?, val value: String?)
}
