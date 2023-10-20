package com.projectronin.interop.fhir.ronin.resource.extractor

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.Resource
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Extractor for extracting Medications from medication-containing Resources.
 */
@Component
class MedicationExtractor {
    private val logger = KotlinLogging.logger { }

    /**
     * Extracts Medications from the details from the resource, including the [medication] and [contained] resources.
     * If no medication was extracted, null will be returned. Otherwise, the response will include the extracted Medication
     * and any changes to the supplied values that should be applied to the resource.
     */
    fun extractMedication(
        medication: DynamicValue<Any>?,
        contained: List<Resource<*>>,
        resource: Resource<*>
    ): MedicationExtraction? {
        val resourceIdString = resource.id?.value
        if (medication == null || resourceIdString == null) {
            return null
        }

        val source = resource.meta?.source
        return when (medication.type) {
            DynamicValueType.CODEABLE_CONCEPT -> extractCodeableConcept(
                medication.value as CodeableConcept,
                resourceIdString,
                source
            )

            DynamicValueType.REFERENCE -> extractReference(
                medication.value as Reference,
                contained,
                resourceIdString,
                source
            )

            else -> {
                logger.warn { "Medication $resourceIdString supplied with an unknown type [${medication.type}] so no Medications were extracted" }
                null
            }
        }
    }

    /**
     * Extracts the Medication from the [medicationCodeableConcept].
     */
    private fun extractCodeableConcept(
        medicationCodeableConcept: CodeableConcept,
        resourceId: String,
        source: Uri?
    ): MedicationExtraction {
        val codingsForId =
            medicationCodeableConcept.coding.filter { it.userSelected?.value ?: false }.takeIf { it.isNotEmpty() }
                ?: medicationCodeableConcept.coding
        val codeableConceptId = codingsForId.mapNotNull { it.code?.value }.joinToString(separator = "-")
        val medicationId = "codeable-$resourceId-$codeableConceptId"

        val medication = Medication(
            id = Id(medicationId),
            meta = Meta(source = source),
            code = medicationCodeableConcept
        )
        val medicationReference = Reference(reference = FHIRString("Medication/$medicationId"))
        return MedicationExtraction(
            extractedMedication = medication,
            updatedMedication = DynamicValue(DynamicValueType.REFERENCE, medicationReference)
        )
    }

    /**
     * Extracts a Medication from the [medicationReference] if the reference is to a [contained] Medication.
     * If the [medicationReference] is not to a [contained] Medication, then null will be returned.
     */
    private fun extractReference(
        medicationReference: Reference,
        contained: List<Resource<*>>,
        resourceId: String,
        source: Uri?
    ): MedicationExtraction? {
        // If there's no contained, then we can't do anything
        if (contained.isEmpty()) {
            return null
        }

        // If the reference is not a "local" reference, then we have nothing to do.
        if (medicationReference.reference?.value?.startsWith("#") != true) {
            return null
        }

        val medication =
            contained.firstOrNull {
                it.resourceType == "Medication" && it.id?.value == medicationReference.reference?.value?.removePrefix(
                    "#"
                )
            } as? Medication
        return medication?.let {
            val newMeta = medication.meta?.copy(source = source) ?: Meta(source = source)

            val newMedicationId = "contained-$resourceId-${medication.id!!.value!!}"
            val newMedication = medication.copy(id = Id(newMedicationId), meta = newMeta)
            val newMedicationReference = Reference(reference = FHIRString("Medication/$newMedicationId"))
            val newContained = contained - medication

            MedicationExtraction(
                extractedMedication = newMedication,
                updatedMedication = DynamicValue(DynamicValueType.REFERENCE, newMedicationReference),
                updatedContained = newContained
            )
        }
    }
}

data class MedicationExtraction(
    val extractedMedication: Medication,
    val updatedMedication: DynamicValue<Reference>,
    val updatedContained: List<Resource<*>>? = null
)
