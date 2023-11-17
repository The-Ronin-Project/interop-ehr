package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContext
import com.projectronin.interop.fhir.r4.validate.resource.R4DocumentReferenceValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.ronin.util.validateReferenceList
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
    localizer: Localizer,
    protected val registryClient: NormalizationRegistryClient
) :
    USCoreBasedProfile<DocumentReference>(
        R4DocumentReferenceValidator,
        RoninProfile.DOCUMENT_REFERENCE.value,
        normalizer,
        localizer
    ) {
    override val rcdmVersion = RCDMVersion.V3_25_0
    override val profileVersion = 5

    private val requiredCategoryError = RequiredFieldError(DocumentReference::category)
    private val requiredSubjectError = RequiredFieldError(DocumentReference::subject)
    private val requiredExtensionError = RequiredFieldError(DocumentReference::extension)
    private val requiredTypeError = RequiredFieldError(DocumentReference::type)
    private val requiredUrlError = RequiredFieldError(Attachment::url)

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
    private val requiredDatalakeAttachmentExtension = FHIRError(
        code = "RONIN_DOCREF_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Datalake Attachment URL extension is missing or invalid",
        location = LocationContext(Url::extension)
    )
    private val requiredEncounter = FHIRError(
        code = "RONIN_DOCREF_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "No more than one encounter is allowed for this type",
        location = LocationContext(DocumentReferenceContext::encounter)
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

            element.content.forEachIndexed { index, content ->
                content.attachment?.let { attachment ->
                    val attachmentContext = parentContext.append(LocationContext("", "content[$index].attachment"))
                    checkNotNull(attachment.url, requiredUrlError, attachmentContext)

                    // TODO: Disabled until we figure out a better way to do this. https://projectronin.atlassian.net/browse/INT-2147
                    // attachment.url?.let { url ->
                    //     val urlContext = attachmentContext.append(LocationContext("", "url"))
                    //     checkTrue(
                    //         url.extension.any {
                    //             it.url == RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri &&
                    //                 it.value?.type == DynamicValueType.URL
                    //         },
                    //         requiredDatalakeAttachmentExtension,
                    //         urlContext
                    //     )
                    // }
                }
            }

            // status is validated in R4
            // type will be populated from mapping
            // non-empty content is validated in R4
            // non-empty attachment is validated in R4
            // docStatus value set is validated in R4
        }
    }

    override fun validateUSCore(element: DocumentReference, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            validateReference(
                element.subject,
                listOf(ResourceType.Patient),
                LocationContext(DocumentReference::subject),
                validation
            )

            element.context?.let { context ->
                val contextLocationContext = parentContext.append(LocationContext(DocumentReference::context))

                checkTrue(context.encounter.size < 2, requiredEncounter, contextLocationContext)

                validateReferenceList(
                    context.encounter,
                    listOf(ResourceType.Encounter),
                    contextLocationContext.append(LocationContext("", "encounter")),
                    validation
                )
            }

            // USCore adds an optional code (0.*) of Clinical Note, in the category attribute,
            // but the base binding is still an example binding and there should remain unconstrained.

            checkNotNull(element.subject, requiredSubjectError, parentContext)
        }
    }

    override fun conceptMap(
        normalized: DocumentReference,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<DocumentReference, Validation> {
        val validation = Validation()

        // DocumentReference.type is a single CodeableConcept
        val mappedTypePair = normalized.type?.let { type ->
            val typePair = registryClient.getConceptMapping(
                tenant,
                "DocumentReference.type",
                type,
                normalized,
                forceCacheReloadTS
            )
            // validate the mapping we got, use type value to report issues
            validation.apply {
                checkNotNull(
                    typePair,
                    FailedConceptMapLookupError(
                        LocationContext(DocumentReference::type),
                        type.coding.mapNotNull { it.code?.value }
                            .joinToString(", "),
                        "any DocumentReference.type concept map for tenant '${tenant.mnemonic}'",
                        typePair?.metadata
                    ),
                    parentContext
                )
            }
            typePair
        }

        return Pair(
            mappedTypePair?.let {
                normalized.copy(
                    type = it.codeableConcept,
                    extension = normalized.extension + it.extension
                )
            } ?: normalized,
            validation
        )
    }

    override fun transformInternal(
        normalized: DocumentReference,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<DocumentReference>?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(TransformResponse(transformed), Validation())
    }
}
