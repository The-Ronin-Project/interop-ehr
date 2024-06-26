package com.projectronin.interop.ehr.cerner

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.ehr.dataauthority.models.Identifier
import com.projectronin.ehr.dataauthority.models.IdentifierSearchableResourceTypes
import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.outputs.AppointmentsWithNewPatients
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class CernerAppointmentService(
    cernerClient: CernerClient,
    private val ehrdaClient: EHRDataAuthorityClient,
    private val cernerPatientService: CernerPatientService,
) : AppointmentService, CernerFHIRService<Appointment>(cernerClient) {
    override val fhirURLSearchPart = "/Appointment"
    override val fhirResourceType = Appointment::class.java

    override fun findPatientAppointments(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        patientMRN: String?,
        useEHRFallback: Boolean,
    ): List<Appointment> {
        val parameters =
            mapOf(
                "patient" to patientFHIRId,
                "date" to getDateParam(startDate, endDate, tenant),
            )
        return getResourceListFromSearch(tenant, parameters)
    }

    override fun findProviderAppointments(
        tenant: Tenant,
        providerIDs: List<FHIRIdentifiers>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): AppointmentsWithNewPatients {
        val providerFhirIDs = providerIDs.map { it.id.value!! }
        val parameters =
            mapOf(
                "practitioner" to providerFhirIDs.joinToString(separator = ","),
                "date" to getDateParam(startDate, endDate, tenant),
            )
        val appointments = getResourceListFromSearch(tenant, parameters)
        val newPatients = appointments.findNewPatients(tenant)
        return AppointmentsWithNewPatients(appointments, newPatients)
    }

    override fun findLocationAppointments(
        tenant: Tenant,
        locationFHIRIds: List<String>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): AppointmentsWithNewPatients {
        val parameters =
            mapOf(
                "location" to locationFHIRIds.joinToString(separator = ","),
                "date" to getDateParam(startDate, endDate, tenant),
            )
        val appointments = getResourceListFromSearch(tenant, parameters)
        val newPatients = appointments.findNewPatients(tenant)
        return AppointmentsWithNewPatients(appointments, newPatients)
    }

    /***
     * Using the references in the appointments
     */
    private fun List<Appointment>.findNewPatients(tenant: Tenant): List<Patient> {
        val allParticipants = this.map { it.participant }.flatten()
        val patientParticipants = allParticipants.filter { it.isPatient() }
        val patientReferences = patientParticipants.map { it.actor?.reference?.value!! }
        // distinct ensures if the same patient is in multiple appointments we're only going to operate on them once
        val patientFhirIds = patientReferences.map { it.removePrefix("Patient/") }.distinct()

        if (patientFhirIds.isEmpty()) {
            return emptyList()
        }

        // lookup the fhir IDs found in the appointments against EHRDA and filter out the existing patients
        val newPatientFHIRIds =
            runBlocking {
                val foundFhirIds =
                    ehrdaClient.getResourceIdentifiers(
                        tenant.mnemonic,
                        IdentifierSearchableResourceTypes.Patient,
                        patientFhirIds.map {
                            Identifier(
                                system = CodeSystem.RONIN_FHIR_ID.uri.value!!,
                                value = it,
                            )
                        },
                    ).map { it.searchedIdentifier.value }
                // we want the ones we didn't find
                patientFhirIds - foundFhirIds.toSet()
            }

        return newPatientFHIRIds.map {
            cernerPatientService.getPatient(tenant, it)
        }
    }
}

// This logic might be better served in interop-fhir if we find we can generalize it
fun Participant.isPatient(): Boolean {
    return when {
        this.actor?.reference?.value?.startsWith("Patient") == true -> true
        this.type.firstOrNull { it.text?.value == "Patient" } != null -> true
        else -> false
    }
}
