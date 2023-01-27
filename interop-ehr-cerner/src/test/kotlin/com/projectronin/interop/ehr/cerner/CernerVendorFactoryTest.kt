package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.vendor.VendorType
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

    private val vendorFactory =
        CernerVendorFactory(
            patientService,
            practitionerService,
            appointmentService,
            conditionService,
            locationService
        )

    @Test
    fun `is all wired for spring`() {
        @Configuration
        @ComponentScan("com.projectronin.interop")
        class TestConfig() {
            @Bean
            @Qualifier("ehr")
            fun ehrDatabase(): Database = mockk()

            @Bean
            fun ociClient(): OCIClient = mockk()

            @Bean
            fun datalakePublishService(): Database = mockk()
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
    fun `returns NotImplementedError for MessageService`() {
        assertThrows<NotImplementedError> { vendorFactory.messageService }
    }

    @Test
    fun `returns NotImplementedError for IdentifierService`() {
        assertThrows<NotImplementedError> { vendorFactory.identifierService }
    }

    @Test
    fun `returns NotImplementedError for ObservationService`() {
        assertThrows<NotImplementedError> { vendorFactory.observationService }
    }

    @Test
    fun `returns NotImplementedError for MedicationService`() {
        assertThrows<NotImplementedError> { vendorFactory.medicationService }
    }

    @Test
    fun `returns NotImplementedError for MedicationStatementService`() {
        assertThrows<NotImplementedError> { vendorFactory.medicationStatementService }
    }

    @Test
    fun `returns NotImplementedError for MedicationRequestService`() {
        assertThrows<NotImplementedError> { vendorFactory.medicationRequestService }
    }

    @Test
    fun `returns NotImplementedError for OrganizationService`() {
        assertThrows<NotImplementedError> { vendorFactory.organizationService }
    }
}
