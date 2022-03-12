package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicPatientBundle
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.receive
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component
import com.projectronin.interop.fhir.r4.resource.Bundle as R4Bundle

/**
 * Service providing access to patients within Epic.
 */
@Component
class EpicPatientService(private val epicClient: EpicClient) : PatientService {
    private val logger = KotlinLogging.logger { }
    private val patientSearchUrlPart = "/api/FHIR/R4/Patient"

    override fun findPatient(
        tenant: Tenant,
        birthDate: String,
        givenName: String,
        familyName: String
    ): Bundle<Patient> {
        logger.info { "Patient search started for ${tenant.mnemonic}" }

        val parameters = mapOf("given" to givenName, "family" to familyName, "birthdate" to birthDate)
        val bundle = runBlocking {
            val httpResponse = epicClient.get(tenant, patientSearchUrlPart, parameters)
            if (httpResponse.status != HttpStatusCode.OK) {
                logger.error { "Patient search failed for ${tenant.mnemonic}, with a ${httpResponse.status}" }
                throw IOException("Call to tenant ${tenant.mnemonic} failed with a ${httpResponse.status}")
            }
            httpResponse.receive<R4Bundle>()
        }

        logger.info { "Patient search completed for ${tenant.mnemonic}" }
        return EpicPatientBundle(bundle)
    }

    override fun <K> findPatientsById(tenant: Tenant, patientIdsByKey: Map<K, Identifier>): Map<K, Patient> {
        logger.info { "Patient find by id started for ${tenant.mnemonic} with ${patientIdsByKey.size} patients requested" }

        // Gather the full batch of identifiers to request.
        val identifierParam = patientIdsByKey.filter { entry ->
            val typeFound = entry.value.type != null
            if (!typeFound) logger.warn { "Type missing on key, ${entry.key}. Key was removed." }
            typeFound
        }.values.toSet().joinToString(separator = ",") { patientIdentifier ->
            "${patientIdentifier.type?.text}|${patientIdentifier.value}"
        }

        val patientsFound = runBlocking {
            val httpResponse = epicClient.get(tenant, patientSearchUrlPart, mapOf("identifier" to identifierParam))
            if (httpResponse.status != HttpStatusCode.OK) {
                logger.error { "Patient find by id failed for ${tenant.mnemonic}, with a ${httpResponse.status}" }
                throw IOException("Call to tenant ${tenant.mnemonic} failed with a ${httpResponse.status}")
            }
            httpResponse.receive<R4Bundle>()
        }

        // Translate to the Epic Patients
        val epicPatientBundle = EpicPatientBundle(patientsFound)
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
