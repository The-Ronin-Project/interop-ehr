package com.projectronin.interop.ehr.epic

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.GetFHIRIDResponse
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.util.unlocalize
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.call.body
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
    @Value("\${epic.fhir.batchSize:5}") private val batchSize: Int,
    private val aidboxPatientService: AidboxPatientService
) : PatientService,
    EpicPagingService(epicClient) {
    private val patientSearchUrlPart = "/api/FHIR/R4/Patient"
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
            "family" to familyName,
            "birthdate" to DateTimeFormatter.ofPattern("yyyy-MM-dd").format(birthDate)
        )
        val bundle = runBlocking {
            val httpResponse = epicClient.get(tenant, patientSearchUrlPart, parameters)
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
                    systemText = requestEntry.value.system?.value?.uppercase(),
                    value = requestEntry.value.value
                )
            ]
            if (foundPatient != null) requestEntry.key to foundPatient else null
        }.toMap()

        logger.info { "Patient find by id for ${tenant.mnemonic} found ${patientsFoundByKey.size} patients" }
        return patientsFoundByKey
    }

    override fun getPatient(tenant: Tenant, patientFHIRID: String): Patient {
        return runBlocking { epicClient.get(tenant, "$patientSearchUrlPart/$patientFHIRID").body() }
    }

    override fun getPatientFHIRId(tenant: Tenant, patientIDValue: String): String {
        return getPatientsFHIRIds(
            tenant,
            tenant.vendorAs<Epic>().patientMRNSystem,
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
        ).mapValues { GetFHIRIDResponse(it.value.unlocalize(tenant)) }

        // Search for any patients that weren't in Aidbox in the EHR.  If there aren't any, return the Aidbox patients.
        val ehrPatientIDValues = patientIDValues.filterNot { patientID ->
            aidboxResponse.keys.contains(patientID)
        }
        if (ehrPatientIDValues.isEmpty()) return aidboxResponse

        val ehrResponse = findPatientsById(
            tenant = tenant,
            ehrPatientIDValues.associateWith { Identifier(value = it, system = Uri(patientIDSystem)) }
        ).filterNot {
            it.value.id == null
        }.mapValues { GetFHIRIDResponse(it.value.id!!.value.unlocalize(tenant), it.value) }

        return aidboxResponse + ehrResponse
    }

    data class SystemValueIdentifier(val systemText: String?, val value: String?)
}
