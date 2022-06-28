package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.epic.model.EpicAppointment
import com.projectronin.interop.ehr.epic.model.EpicCondition
import com.projectronin.interop.ehr.epic.model.EpicLocation
import com.projectronin.interop.ehr.epic.model.EpicObservation
import com.projectronin.interop.ehr.epic.model.EpicPatient
import com.projectronin.interop.ehr.epic.model.EpicPractitioner
import com.projectronin.interop.ehr.epic.model.EpicPractitionerRole
import com.projectronin.interop.ehr.epic.transform.EpicAppointmentTransformer
import com.projectronin.interop.ehr.factory.VendorFactory
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.ehr.model.EHRResource
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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.r4.resource.Condition as R4Condition
import com.projectronin.interop.fhir.r4.resource.Location as R4Location
import com.projectronin.interop.fhir.r4.resource.Observation as R4Observation
import com.projectronin.interop.fhir.r4.resource.Patient as R4Patient
import com.projectronin.interop.fhir.r4.resource.Practitioner as R4Practitioner
import com.projectronin.interop.fhir.r4.resource.PractitionerRole as R4PractitionerRole

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
    private val objectMapper = JacksonManager.objectMapper
    override val vendorType: VendorType
        get() = VendorType.EPIC

    override fun <T : EHRResource> deserialize(string: String, type: KClass<T>): EHRResource {
        return when (type) {
            Appointment::class -> objectMapper.readValue<EpicAppointment>(string)
            Condition::class -> EpicCondition(objectMapper.readValue(string))
            Location::class -> EpicLocation(objectMapper.readValue(string))
            Observation::class -> EpicObservation(objectMapper.readValue(string))
            Patient::class -> EpicPatient(objectMapper.readValue(string))
            Practitioner::class -> EpicPractitioner(objectMapper.readValue(string))
            PractitionerRole::class -> EpicPractitionerRole(objectMapper.readValue(string))
            else -> throw NotImplementedError()
        }
    }

    override fun <T : EHRResource> deserializeList(string: String, type: KClass<T>): List<EHRResource> {
        return when (type) {
            Appointment::class -> objectMapper.readValue<List<EpicAppointment>>(string)
            Condition::class -> objectMapper.readValue<List<R4Condition>>(string).map { EpicCondition(it) }
            Location::class -> objectMapper.readValue<List<R4Location>>(string).map { EpicLocation(it) }
            Observation::class -> objectMapper.readValue<List<R4Observation>>(string).map { EpicObservation(it) }
            Patient::class -> objectMapper.readValue<List<R4Patient>>(string).map { EpicPatient(it) }
            Practitioner::class -> objectMapper.readValue<List<R4Practitioner>>(string).map { EpicPractitioner(it) }
            PractitionerRole::class -> objectMapper.readValue<List<R4PractitionerRole>>(string)
                .map { EpicPractitionerRole(it) }
            else -> throw NotImplementedError()
        }
    }

    override fun <T> serializeObject(t: T): String {
        return JacksonManager.nonAbsentObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(t)
    }
}
