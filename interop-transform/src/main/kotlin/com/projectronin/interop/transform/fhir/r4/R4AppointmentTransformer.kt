package com.projectronin.interop.transform.fhir.r4

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyAppointment
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.AppointmentTransformer
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import org.springframework.stereotype.Component
import com.projectronin.interop.fhir.r4.resource.Appointment as R4Appointment

/**
 * Implementation of [AppointmentTransformer] suitable for all R4 FHIR Appointments
 */
@Component
class R4AppointmentTransformer : AppointmentTransformer {
    private val logger = KotlinLogging.logger { }

    override fun transformAppointments(
        bundle: Bundle<Appointment>,
        tenant: Tenant
    ): List<OncologyAppointment> {
        require(bundle.dataSource == DataSource.FHIR_R4) { "Bundle is not an R4 FHIR resource" }

        return bundle.transformResources(tenant, this::transformAppointment)
    }

    override fun transformAppointment(appointment: Appointment, tenant: Tenant): OncologyAppointment? {
        require(appointment.dataSource == DataSource.FHIR_R4) { "Appointment is not an R4 FHIR resource" }

        val r4Appointment = try {
            JacksonManager.objectMapper.readValue<R4Appointment>(appointment.raw)
        } catch (e: Exception) {
            logger.warn { "Unable to read R4 Appointment: ${e.message}" }
            return null
        }

        val id = r4Appointment.id
        if (id == null) {
            logger.warn { "Unable to transform Appointment due to missing ID" }
            return null
        }

        try {
            return OncologyAppointment(
                id = id.localize(tenant),
                meta = r4Appointment.meta?.localize(tenant),
                implicitRules = r4Appointment.implicitRules,
                language = r4Appointment.language,
                text = r4Appointment.text?.localize(tenant),
                contained = r4Appointment.contained,
                extension = r4Appointment.extension.map { it.localize(tenant) },
                modifierExtension = r4Appointment.modifierExtension.map { it.localize(tenant) },
                identifier = r4Appointment.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
                status = r4Appointment.status,
                cancellationReason = r4Appointment.cancellationReason?.localize(tenant),
                serviceCategory = r4Appointment.serviceCategory.map { it.localize(tenant) },
                serviceType = r4Appointment.serviceType.map { it.localize(tenant) },
                specialty = r4Appointment.specialty.map { it.localize(tenant) },
                appointmentType = r4Appointment.appointmentType?.localize(tenant),
                reasonCode = r4Appointment.reasonCode.map { it.localize(tenant) },
                reasonReference = r4Appointment.reasonReference.map { it.localize(tenant) },
                priority = r4Appointment.priority,
                description = r4Appointment.description,
                supportingInformation = r4Appointment.supportingInformation.map { it.localize(tenant) },
                start = r4Appointment.start,
                end = r4Appointment.end,
                minutesDuration = r4Appointment.minutesDuration,
                slot = r4Appointment.slot.map { it.localize(tenant) },
                created = r4Appointment.created,
                comment = r4Appointment.comment,
                patientInstruction = r4Appointment.patientInstruction,
                basedOn = r4Appointment.basedOn.map { it.localize(tenant) },
                participant = r4Appointment.participant.map { it.localize(tenant) },
                requestedPeriod = r4Appointment.requestedPeriod.map { it.localize(tenant) }
            )
        } catch (e: Exception) {
            logger.warn { "Unable to transform Appointment: ${e.message}" }
            return null
        }
    }
}
