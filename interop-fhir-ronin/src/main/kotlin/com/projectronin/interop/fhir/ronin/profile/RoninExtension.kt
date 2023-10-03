package com.projectronin.interop.fhir.ronin.profile

import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * Ronin extension.url values
 */
enum class RoninExtension(val value: String) {
    // Ronin Common Data Model
    RONIN_CONCEPT_MAP_SCHEMA("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-ConceptMapSchema"),

    // semantic normalization, see https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-Design.html
    TENANT_SOURCE_APPOINTMENT_STATUS("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
    TENANT_SOURCE_CONDITION_CODE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceConditionCode"),
    TENANT_SOURCE_MEDICATION_CODE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceMedicationCode"),
    TENANT_SOURCE_OBSERVATION_CODE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceObservationCode"),
    TENANT_SOURCE_OBSERVATION_VALUE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceObservationValue"),
    TENANT_SOURCE_OBSERVATION_COMPONENT_CODE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceObservationComponentCode"),
    TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceObservationComponentValue"),
    TENANT_SOURCE_TELECOM_SYSTEM("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomSystem"),
    TENANT_SOURCE_TELECOM_USE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomUse"),
    TENANT_SOURCE_ENCOUNTER_CLASS("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterClass"),
    TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceDocumentReferenceType"),

    // data ingestion and data publish extensions
    TENANT_SOURCE_DOCUMENT_REFERENCE_ATTACHMENT_URL("http://projectronin.io/fhir/StructureDefinition/Extension/originalAttachmentURL"),
    DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL("http://projectronin.io/fhir/StructureDefinition/Extension/datalakeAttachmentURL"),

    @Deprecated("As of RCDM v3.20.0")
    TENANT_SOURCE_ENCOUNTER_TYPE("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterType"),
    RONIN_DATA_AUTHORITY_EXTENSION("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),

    TENANT_SOURCE_CARE_PLAN_CATEGORY("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceCarePlanCategory"),
    ORIGINAL_MEDICATION_DATATYPE("http://projectronin.io/fhir/StructureDefinition/Extension/originalMedicationDatatype");
    val uri = Uri(value)
}
