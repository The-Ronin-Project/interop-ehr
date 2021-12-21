package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.DosageDeserializer
import com.projectronin.interop.fhir.jackson.inbound.r4.DoseAndRateDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.DosageSerializer
import com.projectronin.interop.fhir.jackson.outbound.r4.DoseAndRateSerializer

/**
 * The Dosage structure defines general dosage instruction information typically represented in medication requests,
 * medication dispenses and medication statements.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/dosage.html#Dosage)
 */
@JsonDeserialize(using = DosageDeserializer::class)
@JsonSerialize(using = DosageSerializer::class)
data class Dosage(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val sequence: Int? = null,
    val text: String? = null,
    val additionalInstruction: List<CodeableConcept> = listOf(),
    val patientInstruction: String? = null,
    val timing: Timing? = null,
    val asNeeded: DynamicValue<Any>? = null,
    val site: CodeableConcept? = null,
    val route: CodeableConcept? = null,
    val method: CodeableConcept? = null,
    val doseAndRate: List<DoseAndRate> = listOf(),
    val maxDosePerPeriod: Ratio? = null,
    val maxDosePerAdministration: SimpleQuantity? = null,
    val maxDosePerLifetime: SimpleQuantity? = null
) : BackboneElement {
    companion object {
        val acceptedDynamicTypes = listOf(DynamicValueType.BOOLEAN, DynamicValueType.CODEABLE_CONCEPT)
    }

    init {
        asNeeded?.let {
            require(acceptedDynamicTypes.contains(asNeeded.type)) { "asNeeded can only be one of the following: ${acceptedDynamicTypes.joinToString { it.code }}" }
        }
    }
}

@JsonDeserialize(using = DoseAndRateDeserializer::class)
@JsonSerialize(using = DoseAndRateSerializer::class)
data class DoseAndRate(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val type: CodeableConcept? = null,
    val dose: DynamicValue<Any>? = null,
    val rate: DynamicValue<Any>? = null
) : Element {
    companion object {
        val acceptedDoseTypes = listOf(DynamicValueType.RANGE, DynamicValueType.QUANTITY)
        val acceptedRateTypes = listOf(DynamicValueType.RATIO, DynamicValueType.RANGE, DynamicValueType.QUANTITY)
    }

    init {
        dose?.let {
            require(acceptedDoseTypes.contains(dose.type)) { "dose can only be one of the following: ${acceptedDoseTypes.joinToString { it.code }}" }
        }
        rate?.let {
            require(acceptedRateTypes.contains(rate.type)) { "rate can only be one of the following: ${acceptedRateTypes.joinToString { it.code }}" }
        }
    }
}
