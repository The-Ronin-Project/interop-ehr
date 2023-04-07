package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import com.projectronin.interop.fhir.r4.resource.ValueSet
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class NormalizationRegistryClient(
    private val ociClient: OCIClient,
    @Value("\${oci.infx.registry.file:DataNormalizationRegistry/v2/registry.json}")
    private val registryFileName: String
) {
    private val logger = KotlinLogging.logger { }

    /**
     * mnenomic for tenant-agnostic ConceptMaps and ValueSets - cases where the tenant_id is null in the registry item
     */
    private val tenantAgnosticRegistryKey = "tenantAgnosticRegistryKey"

    /**
     * Returns a [Pair] with the transformed [Coding] as the first and an [Extension] as the second,
     * or null if no such mapping could be found. The [Extension] represents the original value before mapping.
     * Calling this will reload the [NormalizationRegistryCache] if necessary.
     */
    fun getConceptMapping(
        tenant: Tenant,
        elementName: String,
        coding: Coding,
        profileUrl: String? = null
    ): Pair<Coding, Extension>? {
        val registryItem = getConceptMapRegistryItem(tenant, elementName, profileUrl) ?: return null
        return getConceptMapping(registryItem, coding)
    }

    /**
     * Returns a [Pair] with the transformed [Coding] as the first and an [Extension] as the second,
     * or null if no such mapping could be found. The [Extension] represents the original value before mapping.
     * Calling this will reload the [NormalizationRegistryCache] if necessary.
     */
    fun <T : CodedEnum<T>> getConceptMappingForEnum(
        tenant: Tenant,
        elementName: String,
        coding: Coding,
        enumClass: KClass<T>,
        profileUrl: String? = null
    ): Pair<Coding, Extension>? {
        val registryItem = getConceptMapRegistryItem(tenant, elementName, profileUrl) ?: return null
        val codedEnum = enumClass.java.enumConstants.find { it.code == coding.code?.value }
        return codedEnum?.let { Pair(coding, createExtension(registryItem, coding)) } ?: getConceptMapping(
            registryItem,
            coding
        )
    }

    private fun getConceptMapping(registryItem: NormalizationRegistryItem, coding: Coding): Pair<Coding, Extension>? {
        val sourceVal = coding.code?.value ?: return null
        val sourceSystem = coding.system?.value ?: return null
        val tenantAgnosticSourceSystem = getTenantAgnosticCodeSystem(sourceSystem)
        val target = registryItem.map?.get(SourceKey(sourceVal, tenantAgnosticSourceSystem)) ?: return null
        return Pair(
            coding.copy(
                system = Uri(target.system),
                code = Code(target.value),
                display = target.display.asFHIR(),
                version = target.version.asFHIR()
            ),
            createExtension(registryItem, coding)
        )
    }

    private fun getConceptMapRegistryItem(
        tenant: Tenant,
        elementName: String,
        profileUrl: String? = null
    ): NormalizationRegistryItem? {
        if (NormalizationRegistryCache.reloadNeeded(tenant.mnemonic)) reload(tenant.mnemonic)
        val cache = NormalizationRegistryCache.getCurrentRegistry()
        return cache.filter { it.tenant_id in listOf(tenant.mnemonic, null) } // null tenant means universal map
            .filter { it.profile_url in listOf(profileUrl, null) } // null profile_url means universal map
            .filter { it.concept_map_uuid?.isNotEmpty() == true }
            .find { it.data_element == elementName }
    }

    private fun createExtension(registryItem: NormalizationRegistryItem, coding: Coding) =
        Extension(
            url = Uri(registryItem.source_extension_url),
            value = DynamicValue(type = DynamicValueType.CODING, value = coding)
        )

    internal fun getNewRegistry(): List<NormalizationRegistryItem> {
        return try {
            JacksonUtil.readJsonList(
                ociClient.getObjectFromINFX(registryFileName)!!,
                NormalizationRegistryItem::class
            )
        } catch (e: Exception) {
            logger.info { e.message }
            listOf()
        }
    }

    // internal for testing purposes.
    internal fun reload(mnemonic: String) {
        val newRegistry = getNewRegistry()
        val currentRegistry = NormalizationRegistryCache.getCurrentRegistry()
        newRegistry.forEach { new ->
            // find matching registry entry based on mapURL
            currentRegistry.find { old -> old.registry_uuid == new.registry_uuid }?.let { old ->
                val useNew = new.tenant_id in listOf(mnemonic, null)
                // ConceptMap
                new.concept_map_uuid?.let {
                    // load a new version. if tenant is null, it's a 'universal' map we should also load.
                    if (old.version != new.version && useNew) {
                        new.map = getConceptMapData(new.filename)
                    } // otherwise copy from the old map
                    else {
                        new.map = old.map
                    }
                } ?: run {
                    // a new ConceptMap was added
                    if (useNew) {
                        new.map = getConceptMapData(new.filename)
                    }
                }
                // ValueSet
                new.value_set_uuid?.let {
                    if (old.version != new.version && useNew) {
                        new.set = getValueSetData(new.filename)
                    } else {
                        new.set = old.set
                    }
                } ?: run {
                    if (useNew) {
                        new.set = getValueSetData(new.filename)
                    }
                }
            }
        }
        NormalizationRegistryCache.setNewRegistry(newRegistry, mnemonic)
    }

    internal fun getConceptMapData(filename: String): Map<SourceKey, TargetValue> {
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
            val targetVersion = group.targetVersion?.value ?: return@forEachGroup
            val agnosticSourceSystem = getTenantAgnosticCodeSystem(sourceSystem)
            group.element?.forEach forEachElement@{ element ->
                val sourceCode = element.code?.value ?: return@forEachElement
                // pray that informatics never has multiple target values
                val targetCode = element.target.first().code?.value ?: return@forEachElement
                val targetDisplay = element.target.first().display?.value ?: return@forEachElement
                mutableMap[SourceKey(sourceCode, agnosticSourceSystem)] = TargetValue(targetCode, targetSystem, targetDisplay, targetVersion)
            }
        }
        return mutableMap
    }

    private fun getTenantAgnosticCodeSystem(system: String): String =
        if (RoninConceptMap.CODE_SYSTEMS.isMappedUri(system)) {
            RoninConceptMap.CODE_SYSTEMS.toTenantAgnosticUri(system)
        } else {
            system
        }

    /**
     * Returns a [List] or null if no such value set could be found.
     * Calling this will reload the [NormalizationRegistryCache] if necessary.
     */
    fun getValueSet(
        tenant: Tenant,
        elementName: String,
        profileUrl: String? = null
    ): List<Coding>? {
        return getValueSetRegistryItemValues(
            getValueSetRegistryItem(tenant.mnemonic, elementName, profileUrl)
        )
    }

    /**
     * Get a "universal" Ronin value set independent of specific tenants.
     */
    fun getValueSet(
        elementName: String,
        profileUrl: String? = null
    ): List<Coding>? {
        return getValueSetRegistryItemValues(
            getValueSetRegistryItem(tenantAgnosticRegistryKey, elementName, profileUrl)
        )
    }

    private fun getValueSetRegistryItem(
        mnemonic: String,
        elementName: String,
        profileUrl: String? = null
    ): NormalizationRegistryItem? {
        if (NormalizationRegistryCache.reloadNeeded(mnemonic)) reload(mnemonic)
        val cache = NormalizationRegistryCache.getCurrentRegistry()
        return cache.filter { it.tenant_id in listOf(mnemonic, null) } // null tenant means universal set
            .filter { it.profile_url in listOf(profileUrl, null) } // null profile_url means universal set
            .filter { it.value_set_uuid?.isNotEmpty() == true }
            .find { it.data_element == elementName }
    }

    private fun getValueSetRegistryItemValues(registryItem: NormalizationRegistryItem?): List<Coding>? {
        return registryItem?.set?.map {
            Coding(
                system = Uri(it.system),
                code = Code(it.value),
                display = it.display.asFHIR(),
                version = it.version.asFHIR()
            )
        }
    }

    internal fun getValueSetData(filename: String): List<TargetValue> {
        val valueSet = try {
            JacksonUtil.readJsonObject(ociClient.getObjectFromINFX(filename)!!, ValueSet::class)
        } catch (e: Exception) {
            logger.info { e.message }
            return emptyList()
        }
        // squish ValueSet into more usable form
        return valueSet.expansion?.contains?.mapNotNull {
            val targetSystem = it.system?.value
            val targetVersion = it.version?.value
            val targetCode = it.code?.value
            val targetDisplay = it.display?.value
            if (targetSystem == null || targetVersion == null || targetCode == null || targetDisplay == null) {
                null
            } else {
                TargetValue(targetCode, targetSystem, targetDisplay, targetVersion)
            }
        } ?: emptyList()
    }
}
