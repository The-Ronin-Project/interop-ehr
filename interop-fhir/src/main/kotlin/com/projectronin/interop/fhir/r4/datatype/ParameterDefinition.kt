package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.valueset.OperationParameterUse

/**
 * The ParameterDefinition structure defines a parameter to a knowledge asset such as a decision support rule or quality
 * measure. Parameters are typically used to communicate patient-independent information such as configuration values,
 * whereas DataRequirements are typically used to communicate patient-dependent information such as MedicationStatements
 * and Encounters.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/metadatatypes.html#ParameterDefinition)
 */
data class ParameterDefinition(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val name: Code? = null,
    val use: OperationParameterUse,
    val min: Int? = null,
    val max: String? = null,
    val documentation: String? = null,
    val type: Code,
    val profile: Canonical? = null
) : Element
