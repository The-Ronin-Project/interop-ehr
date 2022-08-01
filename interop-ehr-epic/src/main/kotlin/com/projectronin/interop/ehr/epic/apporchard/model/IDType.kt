package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier

/**
 * Represents an Epic IDType from AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195#2ParamType84)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class IDType(
    @JsonProperty("ID")
    val id: String,
    val type: String
) {
    fun toIdentifier(): Identifier {
        return Identifier(value = id, type = CodeableConcept(text = type))
    }
}
