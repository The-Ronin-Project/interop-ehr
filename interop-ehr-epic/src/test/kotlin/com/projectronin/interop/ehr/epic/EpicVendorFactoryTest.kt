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
    private val conditionService = mockk<EpicConditionService>()
    private val identifierService = mockk<EpicIdentifierService>()
    private val observationService = mockk<EpicObservationService>()
    private val locationService = mockk<EpicLocationService>()
    private val medicationService = mockk<EpicMedicationService>()
    private val medicationStatementService = mockk<EpicMedicationStatementService>()

    private val vendorFactory =
        EpicVendorFactory(
            patientService,
            appointmentService,
            messageService,
            practitionerService,
            conditionService,
            identifierService,
            observationService,
            locationService,
            medicationService,
            medicationStatementService,
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
}
