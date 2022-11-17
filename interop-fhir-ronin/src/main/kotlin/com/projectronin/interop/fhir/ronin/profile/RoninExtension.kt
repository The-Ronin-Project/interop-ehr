package com.projectronin.interop.fhir.ronin.profile

import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * Ronin extension.url values
 */
enum class RoninExtension(val value: String) {
    RONIN_CONCEPT_MAP_SCHEMA("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-ConceptMapSchema"),
    TENANT_SOURCE_APPOINTMENT_STATUS("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
    TENANT_SOURCE_CONDITION_CODE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceConditionCode"),
    TENANT_SOURCE_MEDICATION_CODE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceMedicationCode"),
    TENANT_SOURCE_OBSERVATION_CODE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceObservationCode"),
    TENANT_SOURCE_TELECOM_SYSTEM("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomSystem"),
    TENANT_SOURCE_TELECOM_USE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomUse");

    val uri = Uri(value)
}
