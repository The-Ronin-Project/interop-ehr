package com.projectronin.interop.fhir.ronin.validation

import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.IssueMetadata
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssue
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.validation.client.ResourceClient
import com.projectronin.interop.validation.client.generated.models.NewIssue
import com.projectronin.interop.validation.client.generated.models.NewMetadata
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Severity
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Client responsible for communicating with the Data Ingestion Validation Issue Management Service.
 */
@Component
class ValidationClient(private val resourceClient: ResourceClient) {
    /**
     * Reports [validation] issues for the [resource] for [tenant].
     */
    fun <T : Resource<T>> reportIssues(
        validation: Validation,
        resource: T,
        tenant: Tenant,
    ): UUID {
        return reportIssues(validation, resource, tenant.mnemonic)
    }

    /**
     * Reports [validation] issues for the [resource] for [tenantMnemonic].
     */
    fun <T : Resource<T>> reportIssues(
        validation: Validation,
        resource: T,
        tenantMnemonic: String,
    ): UUID {
        val newResource = validation.asNewResource(resource, tenantMnemonic)
        val generatedId = runBlocking { resourceClient.addResource(newResource) }
        return generatedId.id!!
    }

    /**
     * Converts this [Validation] into a [NewResource].
     */
    private fun <T : Resource<T>> Validation.asNewResource(
        resource: T,
        tenantMnemonic: String,
    ): NewResource =
        NewResource(
            organizationId = tenantMnemonic,
            resourceType = resource.resourceType,
            resource = objectMapper.writeValueAsString(resource),
            issues = this.issues().map { it.asNewIssue() },
        )

    /**
     * Converts this [ValidationIssue] into a [NewIssue].
     */
    private fun ValidationIssue.asNewIssue(): NewIssue =
        NewIssue(
            severity = this.severity.asSeverity(),
            type = this.code,
            description = this.description,
            location = this.location.toString(),
            metadata = this.metadata?.map { it.asNewMetadata() },
        )

    /**
     * Converts ConceptMapMetadata and ValueSetMetadata to NewMetadata
     */
    private fun IssueMetadata.asNewMetadata(): NewMetadata {
        val type = this.registryEntryType
        return when (type) {
            "concept-map" -> {
                this as ConceptMapMetadata
                this.asNewConceptMapMetadata()
            }
            "value_set" -> {
                this as ValueSetMetadata
                this.asNewValueSetMetadata()
            }
            else -> NewMetadata()
        }
    }

    private fun ConceptMapMetadata.asNewConceptMapMetadata(): NewMetadata =
        NewMetadata(
            registryEntryType = this.registryEntryType,
            conceptMapName = this.conceptMapName,
            conceptMapUuid = UUID.fromString(this.conceptMapUuid),
            version = this.version,
        )

    private fun ValueSetMetadata.asNewValueSetMetadata(): NewMetadata =
        NewMetadata(
            registryEntryType = this.registryEntryType,
            valueSetName = this.valueSetName,
            valueSetUuid = UUID.fromString(this.valueSetUuid),
            version = this.version,
        )

    /**
     * Converts this [ValidationIssueSeverity] into a [Severity].
     */
    private fun ValidationIssueSeverity.asSeverity(): Severity =
        when (this) {
            ValidationIssueSeverity.ERROR -> Severity.FAILED
            ValidationIssueSeverity.WARNING -> Severity.WARNING
        }
}
