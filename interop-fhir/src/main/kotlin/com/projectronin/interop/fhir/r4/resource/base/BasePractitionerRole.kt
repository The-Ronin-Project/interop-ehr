package com.projectronin.interop.fhir.r4.resource.base

import com.fasterxml.jackson.annotation.JsonTypeName
import com.projectronin.interop.fhir.r4.datatype.AvailableTime
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.NotAvailable
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource

/**
 * Base class representing a FHIR R4 PractitionerRole.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/practitionerrole.html)
 */
@JsonTypeName("PractitionerRole")
abstract class BasePractitionerRole {
    val resourceType: String = "PractitionerRole"

    abstract val id: Id?
    abstract val meta: Meta?
    abstract val implicitRules: Uri?
    abstract val language: Code?
    abstract val text: Narrative?
    abstract val contained: List<ContainedResource>
    abstract val extension: List<Extension>
    abstract val modifierExtension: List<Extension>
    abstract val identifier: List<Identifier>
    abstract val active: Boolean?
    abstract val period: Period?
    abstract val practitioner: Reference?
    abstract val organization: Reference?
    abstract val code: List<CodeableConcept>
    abstract val specialty: List<CodeableConcept>
    abstract val location: List<Reference>
    abstract val healthcareService: List<Reference>
    abstract val telecom: List<ContactPoint>
    abstract val availableTime: List<AvailableTime>
    abstract val notAvailable: List<NotAvailable>
    abstract val availabilityExceptions: String?
    abstract val endpoint: List<Reference>

    protected fun validate() {}
}
