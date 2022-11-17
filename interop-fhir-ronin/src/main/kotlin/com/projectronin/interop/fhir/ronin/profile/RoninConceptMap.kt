package com.projectronin.interop.fhir.ronin.profile

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Access to common Ronin-specific code systems
 */
enum class RoninConceptMap(uriString: String) {
    CODE_SYSTEMS("http://projectronin.io/fhir/CodeSystems");

    val uri = Uri(uriString)

    /**
     * Create a [Coding] for input to a Concept Map Registry request,
     * when the incoming data provides only a Code, with a string value, and no Coding.
     * The method derives the correct Coding.system and assigns the Code to Coding.code.
     */
    fun toCoding(tenant: Tenant, fhirPath: String, value: String) =
        Coding(system = this.toUri(tenant, fhirPath), code = Code(value = value))

    /**
     * Create a [Uri] for the Coding.system input to a Concept Map Registry request,
     * when the incoming data provides only a Code value with no system value.
     */
    fun toUri(tenant: Tenant, fhirPath: String) =
        Uri(this.toUriString(tenant, fhirPath))

    /**
     * Compose the string value for the Coding.system input to a Concept Map Registry request,
     * when the incoming data provides only a Code value with no system.
     */
    fun toUriString(tenant: Tenant, fhirPath: String) =
        "${this.uri.value}/${tenant.mnemonic}/${fhirPath.toUriName()}"
}

/**
 * Parse the concept map URL path suffix - such as "ContactPointUse" -
 * from a dot-separated FHIR field path - such as "ContactPoint.use"
 */
private fun String.toUriName() =
    this.split(".").filter { it.length > 1 }.map { field ->
        "${field[0].uppercase()}${field.substring(1)}"
    }.joinToString("")
