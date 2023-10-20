package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.vendor.VendorType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicVendorFactoryTest {
    private val appointmentService = mockk<EpicAppointmentService>()
    private val messageService = mockk<EpicMessageService>()
    private val patientService = mockk<EpicPatientService>()
    private val practitionerService = mockk<EpicPractitionerService>()
    private val practitionerRoleService = mockk<EpicPractitionerRoleService>()
    private val conditionService = mockk<EpicConditionService>()
    private val identifierService = mockk<EpicIdentifierService>()
    private val observationService = mockk<EpicObservationService>()
    private val locationService = mockk<EpicLocationService>()
    private val medicationService = mockk<EpicMedicationService>()
    private val medicationStatementService = mockk<EpicMedicationStatementService>()
    private val medicationRequestService = mockk<EpicMedicationRequestService>()
    private val noteService = mockk<EpicNoteService>()
    private val organizationService = mockk<EpicOrganizationService>()
    private val encounterService = mockk<EpicEncounterService>()
    private val requestGroupService = mockk<EpicRequestGroupService>()
    private val onboardFlagService = mockk<EpicOnboardFlagService>()
    private val healthCheckService = mockk<EpicHealthCheckService>()
    private val carePlanService = mockk<EpicCarePlanService>()
    private val documentReferenceService = mockk<EpicDocumentReferenceService>()
    private val binaryService = mockk<EpicBinaryService>()
    private val medAdminService = mockk<EpicMedicationAdministrationService>()
    private val serviceRequestService = mockk<EpicServiceRequestService>()

    private val vendorFactory =
        EpicVendorFactory(
            patientService = patientService,
            appointmentService = appointmentService,
            messageService = messageService,
            practitionerService = practitionerService,
            practitionerRoleService = practitionerRoleService,
            conditionService = conditionService,
            identifierService = identifierService,
            observationService = observationService,
            locationService = locationService,
            medicationService = medicationService,
            medicationStatementService = medicationStatementService,
            medicationRequestService = medicationRequestService,
            noteService = noteService,
            organizationService = organizationService,
            onboardFlagService = onboardFlagService,
            encounterService = encounterService,
            requestGroupService = requestGroupService,
            healthCheckService = healthCheckService,
            carePlanService = carePlanService,
            documentReferenceService = documentReferenceService,
            binaryService = binaryService,
            medicationAdministrationService = medAdminService,
            serviceRequestService = serviceRequestService
        )

    @Test
    fun `vendor type is epic`() {
        assertEquals(VendorType.EPIC, vendorFactory.vendorType)
    }

    @Test
    fun `returns AppointmentService`() {
        assertEquals(appointmentService, vendorFactory.appointmentService)
    }

    @Test
    fun `returns MessageService`() {
        assertEquals(messageService, vendorFactory.messageService)
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
    fun `returns IdentifierService`() {
        assertEquals(identifierService, vendorFactory.identifierService)
    }

    @Test
    fun `returns ObservationService`() {
        assertEquals(observationService, vendorFactory.observationService)
    }

    @Test
    fun `returns LocationService`() {
        assertEquals(locationService, vendorFactory.locationService)
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
    fun `returns OrganizationService`() {
        assertEquals(organizationService, vendorFactory.organizationService)
    }

    @Test
    fun `returns OnboardFlagService`() {
        assertEquals(onboardFlagService, vendorFactory.onboardFlagService)
    }

    @Test
    fun `returns HealthCheckService`() {
        assertEquals(healthCheckService, vendorFactory.healthCheckService)
    }

    @Test
    fun `returns NoteService`() {
        assertEquals(noteService, vendorFactory.noteService)
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
    fun `returns MedAdminService`() {
        assertEquals(medAdminService, vendorFactory.medicationAdministrationService)
    }

    @Test
    fun `returns serviceRequestService`() {
        assertEquals(serviceRequestService, vendorFactory.serviceRequestService)
    }
}
