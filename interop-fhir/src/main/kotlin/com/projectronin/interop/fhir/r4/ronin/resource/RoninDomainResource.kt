package com.projectronin.interop.fhir.r4.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.DomainResource

/**
 * Abstract [DomainResource] containing common logic across Ronin profiles.
 */
abstract class RoninDomainResource(
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val text: Narrative? = null,
    override val contained: List<ContainedResource> = listOf(),
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    private val identifier: List<Identifier>
) : DomainResource {
    // These items are required of all Ronin resource types
    init {
        val tenantIdentifier = identifier.find { it.system == CodeSystem.RONIN_TENANT.uri }
        requireNotNull(tenantIdentifier) { "Tenant identifier is required" }

        require(tenantIdentifier.type == CodeableConcepts.RONIN_TENANT) { "Tenant identifier provided without proper CodeableConcept defined" }
        requireNotNull(tenantIdentifier.value) { "tenant value is required" }
    }
}
