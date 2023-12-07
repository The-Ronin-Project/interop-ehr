package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.generators.resources.ServiceRequestGenerator
import com.projectronin.interop.fhir.generators.resources.serviceRequest
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.fhir.ronin.generators.resource.observation.subjectReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.util.generateCodeableConcept
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateRequiredCodeableConceptList
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmServiceRequest(
    tenant: String,
    block: ServiceRequestGenerator.() -> Unit,
): ServiceRequest {
    return serviceRequest {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.SERVICE_REQUEST, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        extension.plus(tenantSourceServiceRequestCode()).plus(tenantSourceServiceRequestCategory())
        subject of generateReference(subject.generate(), listOf("Patient"), tenant, "Patient")
        category of generateRequiredCodeableConceptList(category.generate(), placeholder.random())
        code of generateCodeableConcept(code.generate(), placeholder.random())
    }
}

fun Patient.rcdmServiceRequest(block: ServiceRequestGenerator.() -> Unit): ServiceRequest {
    val data = this.referenceData()
    return rcdmServiceRequest(data.tenantId) {
        block.invoke(this)
        subject of
            generateReference(
                subject.generate(),
                subjectReferenceOptions,
                data.tenantId,
                "Patient",
                data.udpId,
            )
    }
}

fun tenantSourceServiceRequestCode(): Extension {
    return Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceServiceRequestCode"),
        value =
            DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    coding =
                        listOf(
                            possibleCodeCodings.random(),
                        ),
                ),
            ),
    )
}

fun tenantSourceServiceRequestCategory(): Extension {
    return Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceServiceRequestCategory"),
        value =
            DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    coding =
                        listOf(
                            possibleCategoryCodings.random(),
                        ),
                ),
            ),
    )
}

val possibleCodeCodings =
    listOf(
        Coding(
            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.696580"),
            code = Code("39334"),
            display = FHIRString("BRONCHOSCOPY"),
        ),
        Coding(
            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.696580"),
            code = Code("13960"),
            display = FHIRString("MYCHART GLUCOSE FLOWSHEET"),
        ),
        Coding(
            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.696580"),
            code = Code("29837"),
            display = FHIRString("MYCHART BP FLOWSHEET"),
        ),
    )

val possibleCategoryCodings =
    listOf(
        Coding(
            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.10.798268.30"),
            code = Code("1"),
            display = FHIRString("Procedures"),
        ),
        Coding(
            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.10.798268.30"),
            code = Code("7"),
            display = FHIRString("Lab"),
        ),
        Coding(
            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.10.798268.30"),
            code = Code("10"),
            display = FHIRString("Appointment"),
        ),
    )

// TODO: replace placeholder list with actual bindings (currently http://projectronin.io/fhir/ValueSet/placeholder in RCDM)
val placeholder =
    listOf(
        coding {
            code of Code("placeholder")
        },
    )
