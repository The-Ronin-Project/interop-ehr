package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicPatientBundle
import com.projectronin.interop.ehr.model.Bundle
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
class EpicPatientService(private val epicClient: EpicClient) :
    PatientService {
    private val logger = KotlinLogging.logger { }
    private val patientSearchUrlPart = "/api/FHIR/R4/Patient"

    override fun findPatient(
        tenant: Tenant,
        birthDate: String,
        givenName: String,
        familyName: String
    ): Bundle<Patient> {
        logger.debug { "Patient search started for ${tenant.mnemonic}" }

        val parameters = mapOf("given" to givenName, "family" to familyName, "birthdate" to birthDate)
        val bundle = runBlocking {
            val httpResponse = epicClient.get(tenant, patientSearchUrlPart, parameters)
            if (httpResponse.status != HttpStatusCode.OK) {
                logger.error { "Patient search failed for ${tenant.mnemonic}, with a ${httpResponse.status}" }
                throw IOException("Call to tenant ${tenant.mnemonic} failed with a ${httpResponse.status}")
            }
            httpResponse.receive<R4Bundle>()
        }

        logger.debug { "Patient search completed for ${tenant.mnemonic}" }
        return EpicPatientBundle(bundle)
    }
}
