package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.datalake.oci.client.OCIClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.ktorm.database.Database
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

class CernerVendorFactoryTest {
    private val appointmentService = mockk<CernerAppointmentService>()
    private val patientService = mockk<CernerPatientService>()
    private val practitionerService = mockk<CernerPractitionerService>()
    private val conditionService = mockk<CernerConditionService>()
    private val locationService = mockk<CernerLocationService>()
    private val healthCheckService = mockk<CernerHealthCheckService>()
    private val identifierService = mockk<CernerIdentifierService>()
    private val messageService = mockk<CernerMessageService>()
    private val medicationStatementService = mockk<CernerMedicationStatementService>()
    private val medicationService = mockk<CernerMedicationService>()
    private val medicationRequestService = mockk<CernerMedicationRequestService>()
    private val encounterService = mockk<CernerEncounterService>()
    private val observationService = mockk<CernerObservationService>()
    private val organizationService = mockk<CernerOrganizationService>()
    private val requestGroupService = mockk<CernerRequestGroupService>()

    private val vendorFactory =
        CernerVendorFactory(
            patientService = patientService,
            practitionerService = practitionerService,
            appointmentService = appointmentService,
            conditionService = conditionService,
            locationService = locationService,
            healthCheckService = healthCheckService,
            identifierService = identifierService,
            messageService = messageService,
            medicationStatementService = medicationStatementService,
            encounterService = encounterService,
            observationService = observationService,
            organizationService = organizationService,
            medicationRequestService = medicationRequestService,
            medicationService = medicationService,
            requestGroupService = requestGroupService
        )

    @Test
    fun `is all wired for spring`() {
        @Configuration
        @ComponentScan("com.projectronin.interop", "com.projectronin.ehr")
        class TestConfig() {
            @Bean
            @Qualifier("ehr")
            fun ehrDatabase(): Database = mockk()

            @Bean
            fun ociClient(): OCIClient = mockk()

            @Bean
            fun datalakePublishService(): DatalakePublishService = mockk()
        }

        val applicationContext = AnnotationConfigApplicationContext(TestConfig::class.java)

        // If the Vendor Factory and its dependencies are not wired, this will throw an UnsatisfiedDependencyException.
        applicationContext.getBean(CernerVendorFactory::class.java)
    }

    @Test
    fun `vendor type is cerner`() {
        assertEquals(VendorType.CERNER, vendorFactory.vendorType)
    }

    @Test
    fun `returns AppointmentService`() {
        assertEquals(appointmentService, vendorFactory.appointmentService)
    }

    @Test
    fun `returns PatientService`() {
        assertEquals(patientService, vendorFactory.patientService)
    }

    @Test
    fun `returns PractitionerService`() {
        assertEquals(practitionerService, vendorFactory.practitionerService)
    }

    @Test
    fun `returns ConditionService`() {
        assertEquals(conditionService, vendorFactory.conditionService)
    }

    @Test
    fun `returns LocationService`() {
        assertEquals(locationService, vendorFactory.locationService)
    }

    @Test
    fun `returns IdentifierService`() {
        assertEquals(identifierService, vendorFactory.identifierService)
    }

    @Test
    fun `returns MessageService`() {
        assertEquals(messageService, vendorFactory.messageService)
    }

    @Test
    fun `returns ObservationService`() {
        assertEquals(observationService, vendorFactory.observationService)
    }

    @Test
    fun `returns OrganizationService`() {
        assertEquals(organizationService, vendorFactory.organizationService)
    }

    @Test
    fun `returns MedicationService`() {
        assertEquals(medicationService, vendorFactory.medicationService)
    }

    @Test
    fun `returns MedicationStatementService`() {
        assertEquals(medicationStatementService, vendorFactory.medicationStatementService)
    }

    @Test
    fun `returns MedicationRequestService`() {
        assertEquals(medicationRequestService, vendorFactory.medicationRequestService)
    }

    @Test
    fun `returns HealthCheckService`() {
        assertEquals(healthCheckService, vendorFactory.healthCheckService)
    }

    @Test
    fun `returns NotImplementedError for OnboardFlagService`() {
        assertThrows<NotImplementedError> { vendorFactory.onboardFlagService }
    }

    @Test
    fun `returns NotImplementedError for NoteService`() {
        assertThrows<NotImplementedError> { vendorFactory.noteService }
    }

    @Test
    fun `returns RequestGroupService`() {
        assertEquals(requestGroupService, vendorFactory.requestGroupService)
    }
}
