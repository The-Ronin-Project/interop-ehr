package com.projectronin.interop.fhir.r4.ronin.resource

import com.fasterxml.jackson.annotation.JsonTypeName
import com.projectronin.interop.fhir.r4.ExtensionMeanings
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.base.BaseAppointment
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus

/**
 * Project Ronin definition of an Oncology Appointment.
 *
 * See [Project Ronin Profile Spec](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-appointment.html)
 */
@JsonTypeName("Appointment")
data class OncologyAppointment(
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val text: Narrative? = null,
    override val contained: List<ContainedResource> = listOf(),
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    override val identifier: List<Identifier>,
    override val status: AppointmentStatus,
    override val cancellationReason: CodeableConcept? = null,
    override val serviceCategory: List<CodeableConcept> = listOf(),
    override val serviceType: List<CodeableConcept> = listOf(),
    override val specialty: List<CodeableConcept> = listOf(),
    override val appointmentType: CodeableConcept? = null,
    override val reasonCode: List<CodeableConcept> = listOf(),
    override val reasonReference: List<Reference> = listOf(),
    override val priority: Int? = null,
    override val description: String? = null,
    override val supportingInformation: List<Reference> = listOf(),
    override val start: Instant? = null,
    override val end: Instant? = null,
    override val minutesDuration: Int? = null,
    override val slot: List<Reference> = listOf(),
    override val created: DateTime? = null,
    override val comment: String? = null,
    override val patientInstruction: String? = null,
    override val basedOn: List<Reference> = listOf(),
    override val participant: List<Participant>,
    override val requestedPeriod: List<Period> = listOf()
) : RoninDomainResource, BaseAppointment() {
    init {
        validate()

        requireTenantIdentifier(identifier)

        val partnerReference = extension.find { it.url == ExtensionMeanings.PARTNER_DEPARTMENT.uri }
        requireNotNull(partnerReference) {
            "Appointment must have a reference to a partner department"
        }
        require(partnerReference.value?.type == DynamicValueType.REFERENCE) {
            "Partner department reference must be of type Reference"
        }
    }
}
