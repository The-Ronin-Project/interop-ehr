package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.validate.resource.R4DocumentReferenceValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.validateCodeInValueSet
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and transformer for the Ronin Document Reference profile
 */
@Component
class RoninDocumentReference(
    normalizer: Normalizer,
    localizer: Localizer
) :
    USCoreBasedProfile<DocumentReference>(
        R4DocumentReferenceValidator,
        RoninProfile.DOCUMENT_REFERENCE.value,
        normalizer,
        localizer
    ) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 3

    // From http://hl7.org/fhir/us/core/STU5.0.1/ValueSet-us-core-documentreference-category.html
    private val usCoreDocumentReferenceCategoryValueSet = listOf(
        Coding(system = CodeSystem.DOCUMENT_REFERENCE_CATEGORY.uri, code = Code("clinical-note"))
    )

    private val requiredCategoryError = RequiredFieldError(DocumentReference::category)
    private val requiredSubjectError = RequiredFieldError(DocumentReference::subject)
    private val requiredExtensionError = RequiredFieldError(DocumentReference::extension)
    private val requiredTypeError = RequiredFieldError(DocumentReference::type)

    private val requiredDocumentReferenceTypeExtension = FHIRError(
        code = "RONIN_DOCREF_001",
        description = "Tenant source Document Reference extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(DocumentReference::extension)
    )
    private val requiredCodingSize = FHIRError(
        code = "RONIN_DOCREF_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "One, and only one, coding entry is allowed for type",
        location = LocationContext(CodeableConcept::coding)
    )

    override fun validateRonin(element: DocumentReference, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            checkNotNull(element.extension, requiredExtensionError, parentContext)
            checkTrue(
                element.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredDocumentReferenceTypeExtension,
                parentContext
            )

            checkNotNull(element.type, requiredTypeError, parentContext)
            ifNotNull(element.type) {
                checkTrue(
                    element.type!!.coding.size == 1, // coding is required
                    requiredCodingSize,
                    parentContext.append(LocationContext(DocumentReference::type))
                )
            }
            validateReference(
                element.subject,
                listOf("Patient"),
                LocationContext(DocumentReference::subject),
                validation
            )

            // status is validated in R4
            // type will be populated from mapping
            // non-empty content is validated in R4
            // non-empty attachment is validated in R4
            // docStatus value set is validated in R4
        }
    }

    override fun validateUSCore(element: DocumentReference, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(element.category.isNotEmpty(), requiredCategoryError, parentContext)
            element.category.validateCodeInValueSet(
                usCoreDocumentReferenceCategoryValueSet,
                DocumentReference::category,
                parentContext,
                this
            )

            checkNotNull(element.subject, requiredSubjectError, parentContext)
        }
    }

    override fun mapInternal(
        normalized: DocumentReference,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<DocumentReference, Validation> {
        // TODO: apply concept maps to get DocumentReference.type and extension
        val tenantSourceTypeExtension = getExtensionOrEmptyList(
            RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE,
            normalized.type
        )

        val mapped = normalized.copy(
            extension = normalized.extension + tenantSourceTypeExtension
        )
        return Pair(mapped, Validation())
    }

    override fun transformInternal(
        normalized: DocumentReference,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<DocumentReference?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(transformed, Validation())
    }
}
