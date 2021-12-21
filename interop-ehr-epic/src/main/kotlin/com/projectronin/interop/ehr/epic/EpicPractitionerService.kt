package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.PractitionerService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicFindPractitionersResponse
import com.projectronin.interop.ehr.model.FindPractitionersResponse
import com.projectronin.interop.ehr.model.helper.mergeBundles
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.receive
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class EpicPractitionerService(private val epicClient: EpicClient) : PractitionerService {
    private val logger = KotlinLogging.logger { }
    private val practitionerSearchUrlPart = "/api/FHIR/R4/PractitionerRole"

    override fun findPractitionersByLocation(tenant: Tenant, locationIds: List<String>): FindPractitionersResponse {
        logger.debug { "Practitioner search started for ${tenant.mnemonic}" }

        val locationsParameter = locationIds.joinToString(separator = ",")
        val parameters = mapOf("_include" to "PractitionerRole:practitioner", "location" to locationsParameter)

        // If there are more than 1000 results, Epic forces us to make multiple queries.
        val responses: MutableList<EpicFindPractitionersResponse> = mutableListOf()
        var nextURL: String? = null

        do {
            val json = runBlocking {
                val httpResponse =
                    if (nextURL == null) {
                        epicClient.get(tenant, practitionerSearchUrlPart, parameters)
                    } else {
                        epicClient.get(tenant, nextURL!!)
                    }

                if (httpResponse.status != HttpStatusCode.OK) {
                    logger.error { "Practitioner search failed for ${tenant.mnemonic}, with a ${httpResponse.status}" }
                    throw IOException("Call to tenant ${tenant.mnemonic} failed with a ${httpResponse.status}")
                }
                httpResponse.receive<String>()
            }

            val response = EpicFindPractitionersResponse(json)

            responses.add(response)
            nextURL = response.getURL("next")
        } while (nextURL != null)

        logger.debug { "Practitioner search completed for ${tenant.mnemonic}" }
        return mergeResponses(responses)
    }

    /**
     * Merges all the [EpicFindPractitionersResponse]s into one bundle.
     */
    private fun mergeResponses(responses: MutableList<EpicFindPractitionersResponse>): EpicFindPractitionersResponse {
        return responses.reduce { acc, epicFindPractitionersResponse -> mergeBundles(acc, epicFindPractitionersResponse, ::EpicFindPractitionersResponse) }
    }
}
