package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.event.interop.internal.v1.ResourceType.Patient
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.fhir.r4.validate.resource.R4ServiceRequestValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCodeableConcept
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RoninServiceRequest(
    private val registryClient: NormalizationRegistryClient,
    normalizer: Normalizer,
    localizer: Localizer
) :
    USCoreBasedProfile<ServiceRequest>(
        R4ServiceRequestValidator,
        RoninProfile.SERVICE_REQUEST.value,
        normalizer,
        localizer
    ) {
    override val rcdmVersion = RCDMVersion.V3_27_0
    override val profileVersion = 1

    private val minimumExtensionError = FHIRError(
        code = "RONIN_SERVREQ_001",
        description = "Service Request must have at least two extensions",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(ServiceRequest::extension)
    )

    private val invalidTenantSourceServiceRequestCategoryError = FHIRError(
        code = "RONIN_SERVREQ_002",
        description = "Service Request extension Tenant Source Service Request Category is invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(ServiceRequest::extension)
    )

    private val invalidTenantSourceServiceRequestCodeError = FHIRError(
        code = "RONIN_SERVREQ_003",
        description = "Service Request extension Tenant Source Service Request Code is invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(ServiceRequest::extension)
    )

    private val invalidCategorySizeError = FHIRError(
        code = "RONIN_SERVREQ_004",
        description = "Service Request requires exactly 1 Category element",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(ServiceRequest::category)
    )

    override fun validateRonin(
        element: ServiceRequest,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)

            // Must have at least 2 extensions
            checkTrue(element.extension.size >= 2, minimumExtensionError, parentContext)

            // Check for a valid category.
            checkTrue(element.category.size == 1, invalidCategorySizeError, parentContext)
            requireCodeableConcept("category", element.category.first(), parentContext, this)
            checkTrue(
                element.extension.any {
                    it.url?.value == RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri.value &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                invalidTenantSourceServiceRequestCategoryError,
                parentContext
            )

            // Check the code field (R4 Requires a code).
            checkTrue(
                element.extension.count {
                    it.url?.value == RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri.value &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                } == 1,
                invalidTenantSourceServiceRequestCodeError,
                parentContext
            )

            requireDataAuthorityExtensionIdentifier(
                element.subject,
                LocationContext(ServiceRequest::subject),
                validation
            )
        }
    }

    override fun validateUSCore(element: ServiceRequest, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            validateReference(
                element.subject,
                listOf(Patient),
                LocationContext(ServiceRequest::subject),
                this
            )
            requireCodeableConcept("code", element.code, parentContext, this)
        }
    }

    override fun conceptMap(
        normalized: ServiceRequest,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<ServiceRequest?, Validation> {
        val validation = Validation()

        val newExtensions = mutableListOf<Extension>()

        val mapCategoryResponse =
            mapElement(
                normalized.category.first(),
                "ServiceRequest.category",
                normalized,
                parentContext,
                tenant,
                validation,
                forceCacheReloadTS
            )
        val mappedCategory = if (mapCategoryResponse == null) {
            normalized.category
        } else {
            newExtensions.add(mapCategoryResponse.extension)
            listOf(mapCategoryResponse.codeableConcept)
        }

        val mappedCodeResponse =
            mapElement(
                normalized.code,
                "ServiceRequest.code",
                normalized,
                parentContext,
                tenant,
                validation,
                forceCacheReloadTS
            )
        val mappedCode = if (mappedCodeResponse == null) {
            normalized.code
        } else {
            newExtensions.add(mappedCodeResponse.extension)
            mappedCodeResponse.codeableConcept
        }

        val mappedServiceRequest = normalized.copy(
            code = mappedCode,
            category = mappedCategory,
            extension = normalized.extension + newExtensions
        )

        return Pair(
            mappedServiceRequest,
            validation
        )
    }

    private fun mapElement(
        normalizedCodeableConcept: CodeableConcept?,
        elementName: String,
        serviceRequest: ServiceRequest,
        parentContext: LocationContext,
        tenant: Tenant,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): ConceptMapCodeableConcept? {
        return normalizedCodeableConcept?.let { code ->
            val serviceRequestCode = registryClient.getConceptMapping(
                tenant,
                elementName,
                code,
                serviceRequest,
                forceCacheReloadTS
            )
            // validate the mapping we got, use code value to report issues
            validation.apply {
                checkNotNull(
                    serviceRequestCode,
                    FailedConceptMapLookupError(
                        LocationContext("", "code"),
                        code.coding.mapNotNull { it.code?.value }
                            .joinToString(", "),
                        "any $elementName concept map for tenant '${tenant.mnemonic}'",
                        serviceRequestCode?.metadata
                    ),
                    parentContext
                )
            }
            serviceRequestCode
        }
    }

    override fun transformInternal(
        normalized: ServiceRequest,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<ServiceRequest>?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(TransformResponse(transformed), Validation())
    }
}
