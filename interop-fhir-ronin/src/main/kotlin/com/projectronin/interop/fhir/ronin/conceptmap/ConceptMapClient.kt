package com.projectronin.interop.fhir.ronin.conceptmap

import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class ConceptMapClient(private val ociClient: OCIClient) {
    private val logger = KotlinLogging.logger { }

    /**
     * Returns a [Pair] with the transformed [Coding] as the first and an [Extension] as the second,
     * or null if no such mapping could be found. The [Extension] represents the original value before mapping.
     * Calling this will reload the [ConceptMapCache] if necessary.
     *
     * @param tenant the [Tenant] currently in use
     * @param resourceType the [String] representation of the type of resource, i.e. "Appointment"
     * @param elementName the name of the element being mapped, i.e. "Appointment.status" or "Patient.telecom.use".
     *      This must match what INFX has decided is the name of the element. Includes the resourceType as prefix.
     * @param coding a FHIR [Coding] to be mapped. value and system must not be null.
     */
    fun getConceptMapping(
        tenant: Tenant,
        resourceType: String,
        elementName: String,
        coding: Coding
    ): Pair<Coding, Extension>? {
        val sourceVal = coding.code?.value ?: return null
        val sourceSystem = coding.system?.value ?: return null
        val tenantAgnosticSourceSystem = getTenantAgnositcCodeSystem(sourceSystem)
        if (ConceptMapCache.reloadNeeded(tenant)) reload(tenant)
        val cache = ConceptMapCache.getCurrentRegistry()
        val registry = cache.filter { it.tenant_id in listOf(tenant.mnemonic, null) } // null tenant means universal map
            .filter { it.resource_type == resourceType }
            .find { it.data_element == elementName } ?: return null
        val target = registry.map?.get(SourceKey(sourceVal, tenantAgnosticSourceSystem)) ?: return null
        return Pair(
            coding.copy(system = Uri(target.system), code = Code(target.value)),
            Extension(
                url = Uri(registry.source_extension_url),
                value = DynamicValue(type = DynamicValueType.CODING, value = coding)
            )
        )
    }

    internal fun getNewRegistry(): List<ConceptMapRegistry> {
        return try {
            JacksonUtil.readJsonList(
                // might want to swap hardcoded name for env config at some point
                ociClient.getObjectFromINFX("DataNormalizationRegistry/v1/registry.json")!!,
                ConceptMapRegistry::class
            )
        } catch (e: Exception) {
            logger.info { e.message }
            listOf()
        }
    }

    // internal for testing purposes.
    internal fun reload(tenant: Tenant) {
        val newRegistry = getNewRegistry()
        val currentRegistry = ConceptMapCache.getCurrentRegistry()
        newRegistry.forEach { new ->
            // find matching registry entry based on mapURL
            currentRegistry.find { old -> old.registry_uuid == new.registry_uuid }?.let { old ->
                // load a new version. if tenant is null, it's a 'universal' map we should also load.
                if (old.version != new.version && new.tenant_id in listOf(tenant.mnemonic, null))
                    new.map = getConceptMap(new.filename)
                // otherwise copy from the old map
                else
                    new.map = old.map
            } ?: run {
                // a new ConceptMap was added
                if (new.tenant_id in listOf(tenant.mnemonic, null))
                    new.map = getConceptMap(new.filename)
            }
        }
        ConceptMapCache.setNewRegistry(newRegistry, tenant)
    }

    internal fun getConceptMap(filename: String): Map<SourceKey, TargetValue> {
        val conceptMap = try {
            JacksonUtil.readJsonObject(ociClient.getObjectFromINFX(filename)!!, ConceptMap::class)
        } catch (e: Exception) {
            logger.info { e.message }
            return emptyMap()
        }
        // squish ConceptMap into more usable form
        val mutableMap = mutableMapOf<SourceKey, TargetValue>()
        conceptMap.group.forEach forEachGroup@{ group ->
            val targetSystem = group.target?.value ?: return@forEachGroup
            val sourceSystem = group.source?.value ?: return@forEachGroup
            val agnosticSourceSystem = getTenantAgnositcCodeSystem(sourceSystem)
            group.element?.forEach forEachElement@{ element ->
                val sourceCode = element.code?.value ?: return@forEachElement
                // pray that informatics never has multiple target values
                val targetCode = element.target.first().code?.value ?: return@forEachElement
                mutableMap[SourceKey(sourceCode, agnosticSourceSystem)] = TargetValue(targetCode, targetSystem)
            }
        }
        return mutableMap
    }

    private fun getTenantAgnositcCodeSystem(system: String): String =
        if (RoninConceptMap.CODE_SYSTEMS.isMappedUri(system)) {
            RoninConceptMap.CODE_SYSTEMS.toTenantAgnosticUri(system)
        } else {
            system
        }
}
