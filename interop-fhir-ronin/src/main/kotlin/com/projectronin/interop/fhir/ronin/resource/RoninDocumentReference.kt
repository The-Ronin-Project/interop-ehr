package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.validate.resource.R4DocumentReferenceValidator
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.validateCodeInValueSet
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Validator and transformer for the Ronin Document Reference profile
 */
@Component
class RoninDocumentReference(
    normalizer: Normalizer,
    localizer: Localizer,
    private val registryClient: NormalizationRegistryClient
) :
    USCoreBasedProfile<DocumentReference>(
        R4DocumentReferenceValidator,
        RoninProfile.DOCUMENT_REFERENCE.value,
        normalizer,
        localizer
    ) {

    // From http://hl7.org/fhir/us/core/STU5.0.1/ValueSet-us-core-documentreference-category.html
    private val usCoreDocumentReferenceCategoryValueSet = listOf(
        Coding(system = CodeSystem.DOCUMENT_REFERENCE_CATEGORY.uri, code = Code("clinical-note"))
    )

    private val requiredCategoryError = RequiredFieldError(DocumentReference::category)
    private val requiredSubjectError = RequiredFieldError(DocumentReference::subject)

    override fun validateRonin(element: DocumentReference, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)
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

    override fun transformInternal(
        normalized: DocumentReference,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<DocumentReference?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(transformed, Validation())
    }
}
