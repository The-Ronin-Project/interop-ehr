package com.projectronin.interop.fhir.ronin.transform

import com.projectronin.interop.common.logmarkers.LogMarkers
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.ronin.validation.ValidationClient
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Performs transformations, ensuring that any validation issues are reported through the [validationClient].
 */
@Component
class TransformManager(private val validationClient: ValidationClient) {
    val logger = KotlinLogging.logger {}

    /**
     * Attempts to transform the [resource] with the [transformer] for [tenant]. If transformation was not possible,
     * null will be returned. Any issues that occur during transformation will be reported to the Data Ingestion Validation
     * Issue Management Service. Pass in a timestamp to [forceCacheReloadTS] if this transform requires a fresh pull of
     * normalization data (concept maps + value sets).
     */
    fun <T : Resource<T>> transformResource(
        resource: T,
        transformer: ProfileTransformer<T>,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime? = null
    ): T? {
        val (transformed, validation) = transformer.transform(resource, tenant, forceCacheReloadTS)

        if (validation.hasIssues()) {
            // If we did not get back a transformed resource, we need to report out against the original.
            val reportedResource = transformed ?: resource
            logger.warn(LogMarkers.VALIDATION_ISSUE) { "Failed to transform ${resource.resourceType}" }
            validation.issues().forEach { logger.warn { it } } // makes mirth debugging much easier
            validationClient.reportIssues(validation, reportedResource, tenant)
        }

        return transformed
    }
}
