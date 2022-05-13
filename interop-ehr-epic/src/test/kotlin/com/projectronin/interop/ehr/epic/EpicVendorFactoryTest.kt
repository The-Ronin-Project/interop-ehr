package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.epic.transform.EpicAppointmentTransformer
import com.projectronin.interop.transform.fhir.r4.R4ConditionTransformer
import com.projectronin.interop.transform.fhir.r4.R4LocationTransformer
import com.projectronin.interop.transform.fhir.r4.R4PatientTransformer
import com.projectronin.interop.transform.fhir.r4.R4PractitionerRoleTransformer
import com.projectronin.interop.transform.fhir.r4.R4PractitionerTransformer
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
    private val practitionerTransformer = mockk<R4PractitionerTransformer>()
    private val practitionerRoleTransformer = mockk<R4PractitionerRoleTransformer>()
    private val locationTransformer = mockk<R4LocationTransformer>()
    private val patientTransformer = mockk<R4PatientTransformer>()
    private val appointmentTransformer = mockk<EpicAppointmentTransformer>()
    private val conditionTransformer = mockk<R4ConditionTransformer>()
    private val vendorFactory =
        EpicVendorFactory(
            patientService,
            appointmentService,
            messageService,
            practitionerService,
            conditionService,
            identifierService,
            practitionerTransformer,
            practitionerRoleTransformer,
            locationTransformer,
            patientTransformer,
            appointmentTransformer,
            conditionTransformer
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
    fun `returns PractitionerTransformer`() {
        assertEquals(practitionerTransformer, vendorFactory.practitionerTransformer)
    }

    @Test
    fun `returns PractitionerRoleTransformer`() {
        assertEquals(practitionerRoleTransformer, vendorFactory.practitionerRoleTransformer)
    }

    @Test
    fun `returns LocationTransformer`() {
        assertEquals(locationTransformer, vendorFactory.locationTransformer)
    }

    @Test
    fun `returns PatientTransformer`() {
        assertEquals(patientTransformer, vendorFactory.patientTransformer)
    }

    @Test
    fun `returns AppointmentTransformer`() {
        assertEquals(appointmentTransformer, vendorFactory.appointmentTransformer)
    }

    @Test
    fun `returns ConditionTransformer`() {
        assertEquals(conditionTransformer, vendorFactory.conditionTransformer)
    }

    @Test
    fun `can deserialize and serialize`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicAppointmentList.json")!!.readText()
        val appointments = vendorFactory.deserializeAppointments(originalJson)
        val serializedJson = vendorFactory.serializeObject(appointments)
        assertEquals(originalJson, serializedJson)
        assertEquals(2, appointments.size)
    }
}
