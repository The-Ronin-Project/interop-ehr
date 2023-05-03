package com.projectronin.interop.ehr.epic.client

import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.client.EHRClient
import io.ktor.client.HttpClient
import org.springframework.stereotype.Component

/**
 * Client for Epic based EHR systems.
 */
@Component
class EpicClient(
    client: HttpClient,
    authenticationBroker: EHRAuthenticationBroker,
    datalakeService: DatalakePublishService
) : EHRClient(client, authenticationBroker, datalakeService)
