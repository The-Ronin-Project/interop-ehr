package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.vendor.VendorType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    private val carePlanService = mockk<CernerCarePlanService>()
    private val documentReferenceService = mockk<CernerDocumentReferenceService>()
    private val binaryService = mockk<CernerBinaryService>()
    private val onboardFlagService = mockk<CernerOnboardFlagService>()
    private val medAdminService = mockk<CernerMedicationAdministrationService>()
    private val serviceRequestService = mockk<CernerServiceRequestService>()

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
            requestGroupService = requestGroupService,
            carePlanService = carePlanService,
            documentReferenceService = documentReferenceService,
            binaryService = binaryService,
            onboardFlagService = onboardFlagService,
            medicationAdministrationService = medAdminService,
            serviceRequestService = serviceRequestService
        )

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
    fun `returns NotImplementedError for NoteService`() {
        assertThrows<NotImplementedError> { vendorFactory.noteService }
    }

    @Test
    fun `returns RequestGroupService`() {
        assertEquals(requestGroupService, vendorFactory.requestGroupService)
    }

    @Test
    fun `returns CarePlanService`() {
        assertEquals(carePlanService, vendorFactory.carePlanService)
    }

    @Test
    fun `returns DocumentReferenceService`() {
        assertEquals(documentReferenceService, vendorFactory.documentReferenceService)
    }

    @Test
    fun `returns BinaryService`() {
        assertEquals(binaryService, vendorFactory.binaryService)
    }

    @Test
    fun `returns OnboardFlagService`() {
        assertEquals(onboardFlagService, vendorFactory.onboardFlagService)
    }

    @Test
    fun `returns MedicationAdministrationService`() {
        assertEquals(medAdminService, vendorFactory.medicationAdministrationService)
    }

    @Test
    fun `returns ServiceRequestService`() {
        assertEquals(serviceRequestService, vendorFactory.serviceRequestService)
    }
}
