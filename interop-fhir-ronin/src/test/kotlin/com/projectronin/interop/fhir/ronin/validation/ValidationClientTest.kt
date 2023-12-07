package com.projectronin.interop.fhir.ronin.validation

import com.fasterxml.jackson.databind.json.JsonMapper
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssue
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.validation.client.ResourceClient
import com.projectronin.interop.validation.client.generated.models.GeneratedId
import com.projectronin.interop.validation.client.generated.models.NewIssue
import com.projectronin.interop.validation.client.generated.models.NewMetadata
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Severity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ValidationClientTest {
    private val resourceClient: ResourceClient = mockk()
    private val validationClient = ValidationClient(resourceClient)

    private val jsonMapper = mockk<JsonMapper>()

    @BeforeEach
    fun setup() {
        mockkObject(JacksonManager.Companion)

        every { JacksonManager.objectMapper } returns jsonMapper
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(JacksonManager.Companion)
    }

    @Test
    fun `reports resource with single issue`() {
        val issue =
            mockk<ValidationIssue> {
                every { severity } returns ValidationIssueSeverity.ERROR
                every { code } returns "Error Code"
                every { description } returns "Error Description"
                every { location } returns LocationContext(Patient::name)
                every { metadata } returns null
            }
        val validation =
            mockk<Validation> {
                every { issues() } returns listOf(issue)
            }

        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "test"
            }

        val resource =
            mockk<Patient> {
                every { resourceType } returns "Patient"
            }
        every { jsonMapper.writeValueAsString(resource) } returns "{}"

        val expectedResource =
            NewResource(
                organizationId = "test",
                resourceType = "Patient",
                resource = "{}",
                issues =
                    listOf(
                        NewIssue(
                            severity = Severity.FAILED,
                            type = "Error Code",
                            description = "Error Description",
                            location = "Patient.name",
                        ),
                    ),
            )
        val uuid = UUID.randomUUID()
        coEvery { resourceClient.addResource(expectedResource) } returns GeneratedId(uuid)

        val id = validationClient.reportIssues(validation, resource, tenant)
        assertEquals(uuid, id)
    }

    @Test
    fun `reports resource with multiple issues`() {
        val issue1 =
            mockk<ValidationIssue> {
                every { severity } returns ValidationIssueSeverity.ERROR
                every { code } returns "Error Code"
                every { description } returns "Error Description"
                every { location } returns LocationContext(Patient::name)
                every { metadata } returns null
            }
        val issue2 =
            mockk<ValidationIssue> {
                every { severity } returns ValidationIssueSeverity.WARNING
                every { code } returns "Error Code 2"
                every { description } returns "Error Description 2"
                every { location } returns LocationContext(Patient::identifier)
                every { metadata } returns null
            }
        val validation =
            mockk<Validation> {
                every { issues() } returns listOf(issue1, issue2)
            }

        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "test"
            }

        val resource =
            mockk<Patient> {
                every { resourceType } returns "Patient"
            }
        every { jsonMapper.writeValueAsString(resource) } returns "{}"

        val expectedResource =
            NewResource(
                organizationId = "test",
                resourceType = "Patient",
                resource = "{}",
                issues =
                    listOf(
                        NewIssue(
                            severity = Severity.FAILED,
                            type = "Error Code",
                            description = "Error Description",
                            location = "Patient.name",
                        ),
                        NewIssue(
                            severity = Severity.WARNING,
                            type = "Error Code 2",
                            description = "Error Description 2",
                            location = "Patient.identifier",
                        ),
                    ),
            )
        val uuid = UUID.randomUUID()
        coEvery { resourceClient.addResource(expectedResource) } returns GeneratedId(uuid)

        val id = validationClient.reportIssues(validation, resource, tenant)
        assertEquals(uuid, id)
    }

    @Test
    fun `reports resource using just mnemonic`() {
        val issue =
            mockk<ValidationIssue> {
                every { severity } returns ValidationIssueSeverity.ERROR
                every { code } returns "Error Code"
                every { description } returns "Error Description"
                every { location } returns LocationContext(Patient::name)
                every { metadata } returns null
            }
        val validation =
            mockk<Validation> {
                every { issues() } returns listOf(issue)
            }

        val resource =
            mockk<Patient> {
                every { resourceType } returns "Patient"
            }
        every { jsonMapper.writeValueAsString(resource) } returns "{}"

        val expectedResource =
            NewResource(
                organizationId = "tenant",
                resourceType = "Patient",
                resource = "{}",
                issues =
                    listOf(
                        NewIssue(
                            severity = Severity.FAILED,
                            type = "Error Code",
                            description = "Error Description",
                            location = "Patient.name",
                        ),
                    ),
            )
        val uuid = UUID.randomUUID()
        coEvery { resourceClient.addResource(expectedResource) } returns GeneratedId(uuid)

        val id = validationClient.reportIssues(validation, resource, "tenant")
        assertEquals(uuid, id)
    }

    @Test
    fun `testing concept map metadata`() {
        val meta =
            mockk<ConceptMapMetadata> {
                every { registryEntryType } returns "concept-map"
                every { conceptMapName } returns "test-concept-map"
                every { conceptMapUuid } returns "573b456efca5-03d51d53-1a31-49a9-af74"
                every { version } returns "1"
            }
        val issue =
            mockk<ValidationIssue> {
                every { severity } returns ValidationIssueSeverity.ERROR
                every { code } returns "Error Code"
                every { description } returns "Error Description"
                every { location } returns LocationContext(Patient::name)
                every { metadata } returns listOf(meta)
            }
        val validation =
            mockk<Validation> {
                every { issues() } returns listOf(issue)
            }

        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "test"
            }

        val resource =
            mockk<Patient> {
                every { resourceType } returns "Patient"
            }
        every { jsonMapper.writeValueAsString(resource) } returns "{}"

        val expectedResource =
            NewResource(
                organizationId = "test",
                resourceType = "Patient",
                resource = "{}",
                issues =
                    listOf(
                        NewIssue(
                            severity = Severity.FAILED,
                            type = "Error Code",
                            description = "Error Description",
                            location = "Patient.name",
                            metadata =
                                listOf(
                                    NewMetadata(
                                        registryEntryType = "concept-map",
                                        conceptMapName = "test-concept-map",
                                        conceptMapUuid = UUID.fromString("573b456efca5-03d51d53-1a31-49a9-af74"),
                                        version = "1",
                                    ),
                                ),
                        ),
                    ),
            )
        val uuid = UUID.randomUUID()
        coEvery { resourceClient.addResource(expectedResource) } returns GeneratedId(uuid)

        val id = validationClient.reportIssues(validation, resource, tenant)
        assertEquals(uuid, id)
    }

    @Test
    fun `testing value set metadata`() {
        val meta =
            mockk<ValueSetMetadata> {
                every { registryEntryType } returns "value_set"
                every { valueSetName } returns "test-value-set"
                every { valueSetUuid } returns "573b456efca5-03d51d53-1a31-49a9-af74"
                every { version } returns "1"
            }
        val issue =
            mockk<ValidationIssue> {
                every { severity } returns ValidationIssueSeverity.ERROR
                every { code } returns "Error Code"
                every { description } returns "Error Description"
                every { location } returns LocationContext(Patient::name)
                every { metadata } returns listOf(meta)
            }
        val validation =
            mockk<Validation> {
                every { issues() } returns listOf(issue)
            }

        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "test"
            }

        val resource =
            mockk<Patient> {
                every { resourceType } returns "Patient"
            }
        every { jsonMapper.writeValueAsString(resource) } returns "{}"

        val expectedResource =
            NewResource(
                organizationId = "test",
                resourceType = "Patient",
                resource = "{}",
                issues =
                    listOf(
                        NewIssue(
                            severity = Severity.FAILED,
                            type = "Error Code",
                            description = "Error Description",
                            location = "Patient.name",
                            metadata =
                                listOf(
                                    NewMetadata(
                                        registryEntryType = "value_set",
                                        valueSetName = "test-value-set",
                                        valueSetUuid = UUID.fromString("573b456efca5-03d51d53-1a31-49a9-af74"),
                                        version = "1",
                                    ),
                                ),
                        ),
                    ),
            )
        val uuid = UUID.randomUUID()
        coEvery { resourceClient.addResource(expectedResource) } returns GeneratedId(uuid)

        val id = validationClient.reportIssues(validation, resource, tenant)
        assertEquals(uuid, id)
    }

    @Test
    fun `testing empty metadata`() {
        val meta =
            mockk<ValueSetMetadata> {
                every { registryEntryType } returns "potato-set"
                every { valueSetName } returns "test-value-set"
                every { valueSetUuid } returns "573b456efca5-03d51d53-1a31-49a9-af74"
                every { version } returns "1"
            }
        val issue =
            mockk<ValidationIssue> {
                every { severity } returns ValidationIssueSeverity.ERROR
                every { code } returns "Error Code"
                every { description } returns "Error Description"
                every { location } returns LocationContext(Patient::name)
                every { metadata } returns listOf(meta)
            }
        val validation =
            mockk<Validation> {
                every { issues() } returns listOf(issue)
            }

        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "test"
            }

        val resource =
            mockk<Patient> {
                every { resourceType } returns "Patient"
            }
        every { jsonMapper.writeValueAsString(resource) } returns "{}"

        val expectedResource =
            NewResource(
                organizationId = "test",
                resourceType = "Patient",
                resource = "{}",
                issues =
                    listOf(
                        NewIssue(
                            severity = Severity.FAILED,
                            type = "Error Code",
                            description = "Error Description",
                            location = "Patient.name",
                            metadata = listOf(NewMetadata()),
                        ),
                    ),
            )
        val uuid = UUID.randomUUID()
        coEvery { resourceClient.addResource(expectedResource) } returns GeneratedId(uuid)

        val id = validationClient.reportIssues(validation, resource, tenant)
        assertEquals(uuid, id)
    }
}
