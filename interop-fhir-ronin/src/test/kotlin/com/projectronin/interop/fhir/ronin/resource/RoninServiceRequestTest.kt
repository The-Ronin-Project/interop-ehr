package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.fhir.r4.valueset.RequestIntent
import com.projectronin.interop.fhir.r4.valueset.RequestStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCodeableConcept
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninServiceRequestTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val unmappedCategory1 = CodeableConcept(
        coding = listOf(
            Coding(
                system = Uri("UnmappedCategorySystem#1"),
                code = Code(value = "UnmappedCategory#1")
            )
        )
    )
    private val mappedCategory1 = CodeableConcept(
        coding = listOf(
            Coding(
                system = Uri("MappedCategorySystem#1"),
                code = Code(value = "MappedCategory#1")
            )
        )
    )

    private val unmappedCode1 = CodeableConcept(
        coding = listOf(
            Coding(
                system = Uri("UnmappedCodeSystem#1"),
                code = Code(value = "UnmappedCode#1")
            )
        )
    )

    private val mappedCode1 = CodeableConcept(
        coding = listOf(
            Coding(
                system = Uri("Code"),
                code = Code(value = "normalizedCode")
            )
        )
    )

    private val normalizationRegistryClient = mockk<NormalizationRegistryClient> {
        every {
            getConceptMapping(
                tenant,
                "ServiceRequest.category",
                unmappedCategory1,
                any<ServiceRequest>()
            )
        } answers {
            ConceptMapCodeableConcept(
                codeableConcept = mappedCategory1,
                extension = Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = unmappedCategory1
                    )
                ),
                metadata = listOf()
            )
        }
        every {
            getConceptMapping(
                tenant,
                "ServiceRequest.code",
                unmappedCode1,
                any<ServiceRequest>()
            )
        } answers {
            ConceptMapCodeableConcept(
                codeableConcept = mappedCode1,
                extension = Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = unmappedCode1
                    )
                ),
                metadata = listOf()
            )
        }
        every {
            getConceptMapping(
                tenant,
                "ServiceRequest.category",
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("MissedCategorySystem#1"),
                            code = Code(value = "MissedCategory#1")
                        )
                    )
                ),
                any<ServiceRequest>()
            )
        } answers { null }
        every {
            getConceptMapping(
                tenant,
                "ServiceRequest.code",
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("MissedCodeSystem#1"),
                            code = Code(value = "MissedCode#1")
                        )
                    )
                ),
                any<ServiceRequest>()
            )
        } answers { null }
    }

    private val roninServiceRequest = RoninServiceRequest(normalizationRegistryClient, normalizer, localizer)

    private val completeServiceRequest = ServiceRequest(
        meta = Meta(
            profile = listOf(Canonical(RoninProfile.SERVICE_REQUEST.value)),
            source = Uri("source")
        ),
        extension = listOf(
            Extension(
                url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                value = DynamicValue(
                    type = DynamicValueType.CODEABLE_CONCEPT,
                    value = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("Test"),
                                code = Code(value = "1234")
                            )
                        )
                    )
                )
            ),
            Extension(
                url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                value = DynamicValue(
                    type = DynamicValueType.CODEABLE_CONCEPT,
                    value = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("Category"),
                                code = Code(value = "9876")
                            )
                        )
                    )
                )
            )
        ),
        identifier = listOf(
            Identifier(
                type = CodeableConcepts.RONIN_FHIR_ID,
                system = CodeSystem.RONIN_FHIR_ID.uri,
                value = "12345".asFHIR()
            ),
            Identifier(
                type = CodeableConcepts.RONIN_TENANT,
                system = CodeSystem.RONIN_TENANT.uri,
                value = "test".asFHIR()
            ),
            Identifier(
                type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                value = "EHR Data Authority".asFHIR()
            )
        ),
        intent = RequestIntent.ORDER.asCode(),
        status = RequestStatus.ACTIVE.asCode(),
        subject = Reference(
            id = "888".asFHIR(),
            type = Uri("Patient", extension = dataAuthorityExtension),
            reference = "Patient/888".asFHIR()
        ),
        code = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("Test"),
                    code = Code(value = "1234")
                )
            )
        ),
        category = listOf(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("Category"),
                        code = Code(value = "9876")
                    )
                )
            )
        )
    )

    @Test
    fun `validation succeeds with required attributes`() {
        roninServiceRequest.validate(completeServiceRequest).alertIfErrors()
    }

    @Test
    fun `validation succeeds with multiple tenant categories, but only one normalized category`() {
        // Update the complete service request to have a second category extension
        val serviceRequest = completeServiceRequest.copy(
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Test"),
                                    code = Code(value = "1234")
                                )
                            )
                        )
                    )
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Category"),
                                    code = Code(value = "9876")
                                )
                            )
                        )
                    )
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Category"),
                                    code = Code(value = "9998")
                                )
                            )
                        )
                    )
                )
            )
        )

        roninServiceRequest.validate(serviceRequest).alertIfErrors()
    }

    @Test
    fun `validation fails with multiple normalized categories`() {
        // Update the complete service request to have a second category
        val serviceRequest = completeServiceRequest.copy(
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("Category"),
                            code = Code(value = "9876")
                        )
                    )
                ),
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("Category"),
                            code = Code(value = "9998")
                        )
                    )
                )
            )
        )

        val actualException = assertThrows<IllegalArgumentException> {
            roninServiceRequest.validate(serviceRequest).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" + "ERROR RONIN_SERVREQ_004: Service Request requires exactly 1 Category element @ ServiceRequest.category",
            actualException.message
        )
    }

    @Test
    fun `validation fails when missing tenant code or category extensions`() {
        val serviceRequest = completeServiceRequest.copy(extension = listOf())

        val actualException = assertThrows<IllegalArgumentException> {
            roninServiceRequest.validate(serviceRequest).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_SERVREQ_001: Service Request must have at least two extensions @ ServiceRequest.extension\n" +
                "ERROR RONIN_SERVREQ_002: Service Request extension Tenant Source Service Request Category is invalid @ ServiceRequest.extension\n" +
                "ERROR RONIN_SERVREQ_003: Service Request extension Tenant Source Service Request Code is invalid @ ServiceRequest.extension",
            actualException.message
        )
    }

    @Test
    fun `validation fails when subject is wrong type`() {
        val serviceRequest = completeServiceRequest.copy(
            subject = Reference(
                id = "888".asFHIR(),
                type = Uri("Provider"),
                reference = "Patient/888".asFHIR()
            )
        )

        val actualException = assertThrows<IllegalArgumentException> {
            roninServiceRequest.validate(serviceRequest).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" + "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ ServiceRequest.subject.type.extension",
            actualException.message
        )
    }

    @Test
    fun `validation fails when subject is null`() {
        val serviceRequest = completeServiceRequest.copy(subject = null)
        val actualException = assertThrows<IllegalArgumentException> {
            roninServiceRequest.validate(serviceRequest).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ ServiceRequest.subject",
            actualException.message
        )
    }

    @Test
    fun `ensure transform completes and is valid`() {
        val startingServiceRequest = ServiceRequest(
            id = Id("ServiceRequest1"),
            meta = Meta(
                profile = listOf(Canonical("ServiceRequest")),
                source = Uri("source")
            ),
            identifier = listOf(),
            intent = RequestIntent.ORDER.asCode(),
            status = RequestStatus.ACTIVE.asCode(),
            subject = Reference(
                reference = "Patient/Patient#1".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            code = unmappedCode1,
            category = listOf(unmappedCategory1)
        )

        val actualTransformResult = roninServiceRequest.transform(startingServiceRequest, tenant)
        actualTransformResult.second.alertIfErrors()

        val actualServiceRequest = actualTransformResult.first!!.resource
        assertEquals("ServiceRequest1", actualServiceRequest.id?.value)
        assertEquals(mappedCode1, actualServiceRequest.code)
        assertEquals(1, actualServiceRequest.category.size)
        assertEquals(mappedCategory1, actualServiceRequest.category.first())

        val actualCodeExtension =
            actualServiceRequest.extension.find { it.url == RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri }
        assertEquals(unmappedCode1, actualCodeExtension!!.value?.value as CodeableConcept)

        val actualCategoryExtension =
            actualServiceRequest.extension.find { it.url == RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri }
        assertEquals(unmappedCategory1, actualCategoryExtension!!.value?.value as CodeableConcept)
    }

    @Test
    fun `ensure missing mappings fail validation`() {
        val startingServiceRequest = ServiceRequest(
            id = Id("ServiceRequest1"),
            meta = Meta(
                profile = listOf(Canonical("ServiceRequest")),
                source = Uri("source")
            ),
            identifier = listOf(),
            intent = RequestIntent.ORDER.asCode(),
            status = RequestStatus.ACTIVE.asCode(),
            subject = Reference(
                reference = "Patient/Patient#1".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("MissedCodeSystem#1"),
                        code = Code(value = "MissedCode#1")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("MissedCategorySystem#1"),
                            code = Code(value = "MissedCategory#1")
                        )
                    )
                )
            )
        )

        val actualTransformResult = roninServiceRequest.transform(startingServiceRequest, tenant)
        val actualException = assertThrows<IllegalArgumentException> {
            actualTransformResult.second.alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'MissedCategory#1' has no target defined in any ServiceRequest.category concept map for tenant 'test' @ ServiceRequest.code\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'MissedCode#1' has no target defined in any ServiceRequest.code concept map for tenant 'test' @ ServiceRequest.code\n" +
                "ERROR RONIN_SERVREQ_001: Service Request must have at least two extensions @ ServiceRequest.extension\n" +
                "ERROR RONIN_SERVREQ_002: Service Request extension Tenant Source Service Request Category is invalid @ ServiceRequest.extension\n" +
                "ERROR RONIN_SERVREQ_003: Service Request extension Tenant Source Service Request Code is invalid @ ServiceRequest.extension",
            actualException.message
        )
    }

    @Test
    fun `ensure transform does not throw NoSuchElement error if category is null`() {
        val startingServiceRequest = ServiceRequest(
            id = Id("ServiceRequest1"),
            meta = Meta(
                profile = listOf(Canonical("ServiceRequest")),
                source = Uri("source")
            ),
            identifier = listOf(),
            intent = RequestIntent.ORDER.asCode(),
            status = RequestStatus.ACTIVE.asCode(),
            subject = Reference(
                reference = "Patient/Patient#1".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            code = unmappedCode1,
            category = listOf()
        )
        val actualTransformResult = roninServiceRequest.transform(startingServiceRequest, tenant)
        val actualException = assertThrows<IllegalArgumentException> {
            actualTransformResult.second.alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_SERVREQ_001: Service Request must have at least two extensions @ ServiceRequest.extension\n" +
                "ERROR RONIN_SERVREQ_004: Service Request requires exactly 1 Category element @ ServiceRequest.category\n" +
                "ERROR RONIN_SERVREQ_002: Service Request extension Tenant Source Service Request Category is invalid @ ServiceRequest.extension",
            actualException.message
        )
    }
}
