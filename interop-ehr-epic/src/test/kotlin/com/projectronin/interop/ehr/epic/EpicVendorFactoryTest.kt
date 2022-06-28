package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.epic.model.EpicAppointment
import com.projectronin.interop.ehr.epic.model.EpicCondition
import com.projectronin.interop.ehr.epic.model.EpicLocation
import com.projectronin.interop.ehr.epic.model.EpicObservation
import com.projectronin.interop.ehr.epic.model.EpicPatient
import com.projectronin.interop.ehr.epic.model.EpicPractitioner
import com.projectronin.interop.ehr.epic.model.EpicPractitionerRole
import com.projectronin.interop.ehr.epic.transform.EpicAppointmentTransformer
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.ehr.model.Location
import com.projectronin.interop.ehr.model.Observation
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.ehr.model.Practitioner
import com.projectronin.interop.ehr.model.PractitionerRole
import com.projectronin.interop.transform.fhir.r4.R4ConditionTransformer
import com.projectronin.interop.transform.fhir.r4.R4LocationTransformer
import com.projectronin.interop.transform.fhir.r4.R4PatientTransformer
import com.projectronin.interop.transform.fhir.r4.R4PractitionerRoleTransformer
import com.projectronin.interop.transform.fhir.r4.R4PractitionerTransformer
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    fun `can deserialize appointment`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicAppointment.json")!!.readText()
        val appointment = vendorFactory.deserialize(originalJson, Appointment::class)
        assertEquals(EpicAppointment::class, appointment::class)
    }

    @Test
    fun `can deserialize and serialize appointment list`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicAppointmentList.json")!!.readText()
        val appointments = vendorFactory.deserializeList(originalJson, Appointment::class)
        val serializedJson = vendorFactory.serializeObject(appointments)
        assertEquals(originalJson, serializedJson)
        assertEquals(2, appointments.size)
    }

    @Test
    fun `can deserialize condition`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicCondition.json")!!.readText()
        val condition = vendorFactory.deserialize(originalJson, Condition::class)
        assertEquals(EpicCondition::class, condition::class)
    }

    @Test
    fun `can deserialize condition list`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicConditionList.json")!!.readText()
        val conditions = vendorFactory.deserializeList(originalJson, Condition::class)
        assertEquals(EpicCondition::class, conditions.first()::class)
        assertEquals(1, conditions.size)
    }

    @Test
    fun `can deserialize location`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicLocation.json")!!.readText()
        val location = vendorFactory.deserialize(originalJson, Location::class)
        assertEquals(EpicLocation::class, location::class)
    }

    @Test
    fun `can deserialize location list`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicLocationList.json")!!.readText()
        val locations = vendorFactory.deserializeList(originalJson, Location::class)
        assertEquals(EpicLocation::class, locations.first()::class)
        assertEquals(1, locations.size)
    }

    @Test
    fun `can deserialize observation`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicObservation.json")!!.readText()
        val observation = vendorFactory.deserialize(originalJson, Observation::class)
        assertEquals(EpicObservation::class, observation::class)
    }

    @Test
    fun `can deserialize observation list`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicObservationList.json")!!.readText()
        val observations = vendorFactory.deserializeList(originalJson, Observation::class)
        assertEquals(EpicObservation::class, observations.first()::class)
        assertEquals(1, observations.size)
    }

    @Test
    fun `can deserialize patient`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicPatient.json")!!.readText()
        val patient = vendorFactory.deserialize(originalJson, Patient::class)
        assertEquals(EpicPatient::class, patient::class)
    }

    @Test
    fun `can deserialize patient list`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicPatientList.json")!!.readText()
        val patients = vendorFactory.deserializeList(originalJson, Patient::class)
        assertEquals(EpicPatient::class, patients.first()::class)
        assertEquals(1, patients.size)
    }

    @Test
    fun `can deserialize practitioner`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicPractitioner.json")!!.readText()
        val practitioner = vendorFactory.deserialize(originalJson, Practitioner::class)
        assertEquals(EpicPractitioner::class, practitioner::class)
    }

    @Test
    fun `can deserialize practitioner list`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicPractitionerList.json")!!.readText()
        val practitioners = vendorFactory.deserializeList(originalJson, Practitioner::class)
        assertEquals(EpicPractitioner::class, practitioners.first()::class)
        assertEquals(1, practitioners.size)
    }

    @Test
    fun `can deserialize practitioner role`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicPractitionerRole.json")!!.readText()
        val practitionerRole = vendorFactory.deserialize(originalJson, PractitionerRole::class)
        assertEquals(EpicPractitionerRole::class, practitionerRole::class)
    }

    @Test
    fun `can deserialize practitioner role list`() {
        val originalJson = this::class.java.getResource("/ExampleSerializedEpicPractitionerRoleList.json")!!.readText()
        val practitionerRoles = vendorFactory.deserializeList(originalJson, PractitionerRole::class)
        assertEquals(EpicPractitionerRole::class, practitionerRoles.first()::class)
        assertEquals(1, practitionerRoles.size)
    }

    @Test
    fun `can't deserialize a different class`() {
        val originalJson = ""
        assertThrows<NotImplementedError> { vendorFactory.deserialize(originalJson, Bundle::class) }
    }

    @Test
    fun `can't deserialize a different class list`() {
        val originalJson = ""
        assertThrows<NotImplementedError> { vendorFactory.deserializeList(originalJson, Bundle::class) }
    }
}
