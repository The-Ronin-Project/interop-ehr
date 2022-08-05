package com.projectronin.interop.ehr.util

import com.projectronin.interop.common.exceptions.ServiceUnavailableException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException

/**
 * Extension function for [HttpStatusCode] that throws the correct type of exception based on
 * http status.  Uses the [service] and [tenantMnemonic] to build the exception message.  Any statuses
 * not explicitly defined throw [IOException].
 */
fun HttpStatusCode.handleErrorStatus(service: String, tenantMnemonic: String) {
    when (this) {
        HttpStatusCode.ServiceUnavailable -> throw ServiceUnavailableException(service, "Service unavailable for $tenantMnemonic")
        else -> throw IOException("Call to tenant $tenantMnemonic for $service failed with a $this")
    }
}
