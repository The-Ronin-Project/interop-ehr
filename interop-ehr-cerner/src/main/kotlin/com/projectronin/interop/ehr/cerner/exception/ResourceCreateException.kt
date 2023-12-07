package com.projectronin.interop.ehr.cerner.exception

import com.projectronin.interop.common.exceptions.LogMarkingException
import com.projectronin.interop.common.logmarkers.LogMarkers
import com.projectronin.interop.tenant.config.model.Tenant
import org.slf4j.Marker

/**
 * An Exception that occurs during resource creation.
 */
class ResourceCreateException(tenant: Tenant, fhirUrl: String, messageBlock: () -> String) : LogMarkingException(
    "Exception when calling $fhirUrl for ${tenant.mnemonic}: ${messageBlock.invoke()}",
) {
    override val logMarker: Marker = LogMarkers.HTTP_REQUEST_FAILURE
}
