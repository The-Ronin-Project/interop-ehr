package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.epic.model.EpicAppointment
import com.projectronin.interop.ehr.epic.model.EpicCondition
import com.projectronin.interop.ehr.epic.model.EpicObservation
import com.projectronin.interop.ehr.epic.model.EpicPatient
import com.projectronin.interop.ehr.epic.transform.EpicAppointmentTransformer
import com.projectronin.interop.ehr.factory.VendorFactory
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.ehr.model.EHRResource
import com.projectronin.interop.ehr.model.Observation
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.transform.fhir.r4.R4ConditionTransformer
import com.projectronin.interop.transform.fhir.r4.R4LocationTransformer
import com.projectronin.interop.transform.fhir.r4.R4PatientTransformer
import com.projectronin.interop.transform.fhir.r4.R4PractitionerRoleTransformer
import com.projectronin.interop.transform.fhir.r4.R4PractitionerTransformer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Epic implementation of [VendorFactory].
 */
@Component
class EpicVendorFactory(
    override val patientService: EpicPatientService,
    override val appointmentService: EpicAppointmentService,
    override val messageService: EpicMessageService,
    override val practitionerService: EpicPractitionerService,
    override val conditionService: EpicConditionService,
    override val identifierService: EpicIdentifierService,
    override val practitionerTransformer: R4PractitionerTransformer,
    override val practitionerRoleTransformer: R4PractitionerRoleTransformer,
    override val locationTransformer: R4LocationTransformer,
    @Qualifier("epic") override val patientTransformer: R4PatientTransformer,
    override val appointmentTransformer: EpicAppointmentTransformer,
    override val conditionTransformer: R4ConditionTransformer
) : VendorFactory {
    override val vendorType: VendorType
        get() = VendorType.EPIC

    override fun deserializeAppointments(string: String): List<EpicAppointment> {
        return JacksonManager.objectMapper.readValue(string)
    }

    override fun <T : EHRResource> deserialize(string: String, type: KClass<T>): EHRResource {
        return when (type) {
            Patient::class -> EpicPatient(JacksonManager.objectMapper.readValue(string))
            Condition::class -> EpicCondition(JacksonManager.objectMapper.readValue(string))
            Appointment::class -> JacksonManager.objectMapper.readValue<EpicAppointment>(string)
            Observation::class -> EpicObservation(JacksonManager.objectMapper.readValue(string))
            else -> throw NotImplementedError()
        }
    }

    override fun <T> serializeObject(t: T): String {
        return JacksonManager.nonAbsentObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(t)
    }
}
