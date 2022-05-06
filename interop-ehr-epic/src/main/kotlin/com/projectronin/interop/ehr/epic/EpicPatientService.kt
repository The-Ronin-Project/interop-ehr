package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicPatientBundle
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Patient
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
import com.projectronin.interop.fhir.r4.resource.Bundle as R4Bundle

/**
 * Service providing access to patients within Epic.
 */
@Component
class EpicPatientService(
    private val epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:100}") private val batchSize: Int,
) : PatientService,
    EpicPagingService(epicClient) {
    private val patientSearchUrlPart = "/api/FHIR/R4/Patient"
    private val logger = KotlinLogging.logger { }

    override fun findPatient(
        tenant: Tenant,
        birthDate: LocalDate,
        givenName: String,
        familyName: String,
    ): Bundle<Patient> {
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
        return EpicPatientBundle(bundle)
    }

    override fun <K> findPatientsById(tenant: Tenant, patientIdsByKey: Map<K, Identifier>): Map<K, Patient> {
        logger.info { "Patient find by id started for ${tenant.mnemonic} with ${patientIdsByKey.size} patients requested" }

        // Gather the full batch of identifiers to request.
        val patientIdentifiers = patientIdsByKey.filter { entry ->
            val typeFound = entry.value.type != null
            if (!typeFound) logger.warn { "Type missing on key, ${entry.key}. Key was removed." }
            typeFound
        }.values.toSet()

        // Chunk the identifiers and run the search
        val patientsFound = patientIdentifiers.chunked(batchSize) {
            val identifierParam = it.joinToString(separator = ",") { patientIdentifier ->
                "${patientIdentifier.type?.text}|${patientIdentifier.value}"
            }
            getBundleWithPaging(
                tenant,
                patientSearchUrlPart,
                mapOf("identifier" to identifierParam),
                ::EpicPatientBundle
            )
        }

        // Translate to the Epic Patients
        val epicPatientBundle = mergeResponses(patientsFound, ::EpicPatientBundle)
        // Index patients found based on identifiers
        val foundPatientsByIdentifier = epicPatientBundle.resources.flatMap { patient ->
            patient.identifier.map { identifier ->
                TypeValueIdentifier(typeText = identifier.type?.text?.uppercase(), value = identifier.value) to patient
            }
        }.toMap()

        // Re-key to the request based on requested identifier
        val patientsFoundByKey = patientIdsByKey.mapNotNull { requestEntry ->
            val foundPatient = foundPatientsByIdentifier[
                TypeValueIdentifier(
                    typeText = requestEntry.value.type?.text?.uppercase(), value = requestEntry.value.value
                )
            ]
            if (foundPatient != null) requestEntry.key to foundPatient else null
        }.toMap()

        logger.info { "Patient find by id for ${tenant.mnemonic} found ${patientsFoundByKey.size} patients" }
        return patientsFoundByKey
    }

    data class TypeValueIdentifier(val typeText: String?, val value: String)
}
