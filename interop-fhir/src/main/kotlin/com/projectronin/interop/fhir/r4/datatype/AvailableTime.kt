package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Time

/**
 * Times the practitioner is available or performing this role at the location and/or healthcareservice.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/practitionerrole-definitions.html#PractitionerRole.availableTime)
 */
data class AvailableTime(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val daysOfWeek: List<Code> = listOf(),
    val allDay: Boolean? = null,
    val availableStartTime: Time? = null,
    val availableEndTime: Time? = null
) : BackboneElement
