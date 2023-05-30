package com.projectronin.interop.fhir.ronin.normalization

import com.github.benmanes.caffeine.cache.Caffeine
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
import java.time.Duration
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * Note: here is what INFX has confirmed regarding logic in this client:
 * 1. All value sets entries will have a profile URL and a null tenant.
 * 2. All concept maps will have a tenant.
 * 3. Some concept maps might not have a profile URL.
 */

@Component
class NormalizationRegistryClient(
    private val ociClient: OCIClient,
    @Value("\${oci.infx.registry.file:DataNormalizationRegistry/v2/registry.json}")
    private val registryFileName: String,
    @Value("\${oci.infx.registry.refresh.hours:12}")
    private val defaultReloadHours: String = "12" // use string to prevent issues
) {
    private val logger = KotlinLogging.logger { }
    internal var conceptMapCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(defaultReloadHours.toLong()))
        .build<CacheKey, ConceptMapItem>()
    internal var valueSetCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(defaultReloadHours.toLong()))
        .build<CacheKey, ValueSetItem>()
    internal val itemLastUpdated = mutableMapOf<CacheKey, LocalDateTime>()
    private var registry = listOf<NormalizationRegistryItem>()
    private var registryLastUpdated = LocalDateTime.MIN

    /**
     * Returns a [Pair] with the transformed [Coding] as the first and an [Extension] as the second,
     * or null if no such mapping could be found. The [Extension] represents the original value before mapping.
     * @param tenant the current [Tenant], or null to match all tenants.
     * @param elementName the name of the element being mapped, i.e. "Appointment.status" or "Patient.telecom.use".
     * @param coding a FHIR [Coding] to be mapped. value and system must not be null.
     * @param profileUrl the URL of an [RCDM](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles.html)
     * or FHIR profile, which you can find listed in the RCDM and in enum class [RoninProfile](com.projectronin.interop.fhir.ronin.normalization.RoninProfile),
     * or null to match any element of the type [elementName].
     */
    fun getConceptMapping(
        tenant: Tenant,
        elementName: String,
        coding: Coding,
        profileUrl: String? = null,
        forceCacheReloadTS: LocalDateTime? = null
    ): Pair<Coding, Extension>? {
        val cacheKey = CacheKey(
            registryType = NormalizationRegistryItem.RegistryType.ConceptMap,
            elementName = elementName,
            tenantId = tenant.mnemonic,
            profileUrl = profileUrl
        )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS) ?: return null
        return getConceptMapping(registryItem, coding)
    }

    /**
     * Returns a [Pair] with the transformed [Coding] as the first and an [Extension] as the second,
     * or null if no such mapping could be found. The [Extension] represents the original value before mapping.
     * @param tenant the current [Tenant], or null to match all tenants.
     * @param elementName the name of the element being mapped, i.e. "Appointment.status" or "Patient.telecom.use".
     * @param coding a FHIR [Coding] to be mapped. value and system must not be null.
     * @param enumClass a class that enumerates the values the caller can expect as return values from this concept map.
     * @param profileUrl URL of an RCDM or FHIR profile, or null to match any element of the type [elementName].
     */
    fun <T : CodedEnum<T>> getConceptMappingForEnum(
        tenant: Tenant,
        elementName: String,
        coding: Coding,
        enumClass: KClass<T>,
        profileUrl: String? = null,
        forceCacheReloadTS: LocalDateTime? = null
    ): Pair<Coding, Extension>? {
        val cacheKey = CacheKey(
            registryType = NormalizationRegistryItem.RegistryType.ConceptMap,
            elementName = elementName,
            tenantId = tenant.mnemonic,
            profileUrl = profileUrl
        )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS) ?: return null
        val codedEnum = enumClass.java.enumConstants.find { it.code == coding.code?.value }
        return codedEnum?.let { Pair(coding, createExtension(registryItem, coding)) } ?: getConceptMapping(
            registryItem,
            coding
        )
    }

    private fun getConceptMapping(conceptMapItem: ConceptMapItem, coding: Coding): Pair<Coding, Extension>? {
        val sourceVal = coding.code?.value ?: return null
        val sourceSystem = coding.system?.value ?: return null
        val tenantAgnosticSourceSystem = getTenantAgnosticCodeSystem(sourceSystem)
        val target = conceptMapItem.map?.get(SourceKey(sourceVal, tenantAgnosticSourceSystem)) ?: return null
        return Pair(
            coding.copy(
                system = Uri(target.system),
                code = Code(target.value),
                display = target.display.asFHIR(),
                version = target.version.asFHIR()
            ),
            createExtension(conceptMapItem, coding)
        )
    }

    private fun createExtension(conceptMapItem: ConceptMapItem, coding: Coding) = Extension(
        url = Uri(conceptMapItem.source_extension_url),
        value = DynamicValue(type = DynamicValueType.CODING, value = coding)
    )

    // checks if the cached item needs a reload. Also reloads registry if needed
    private fun reloadNeeded(key: CacheKey, forceCacheReloadTS: LocalDateTime?): Boolean {
        val defaultReloadTime = LocalDateTime.now().minusHours(defaultReloadHours.toLong())
        if (registryLastUpdated.isBefore(forceCacheReloadTS ?: defaultReloadTime)) {
            registry = getNewRegistry()
        }
        return itemLastUpdated[key]?.isBefore(forceCacheReloadTS ?: defaultReloadTime) ?: true
    }

    private fun getConceptMapItem(
        key: CacheKey,
        forceCacheReloadTS: LocalDateTime?
    ): ConceptMapItem? {
        if (reloadNeeded(key, forceCacheReloadTS)) {
            conceptMapCache.invalidate(key)
        }

        val cachedItem = conceptMapCache.getIfPresent(key) // exact match
            ?: conceptMapCache.getIfPresent(key.copy(profileUrl = null)) // universal profile
        return if (cachedItem == null) {
            val matchingItems = registry.filter {
                it.getRegistryItemType() == key.registryType &&
                    it.data_element == key.elementName &&
                    it.tenant_id == key.tenantId
            }
            // check for exact profile match, then fall back to universal profile
            val registryItem = matchingItems.find { it.profile_url == key.profileUrl }
                ?: matchingItems.find { it.profile_url == null }

            registryItem?.let {
                val conceptMapItem =
                    ConceptMapItem(map = getConceptMapData(it.filename), source_extension_url = it.source_extension_url)
                val newKey = key.copy(profileUrl = registryItem.profile_url)
                itemLastUpdated[newKey] = LocalDateTime.now()
                conceptMapCache.put(newKey, conceptMapItem)
                conceptMapItem
            }
        } else {
            cachedItem
        }
    }

    private fun getValueSetItem(
        key: CacheKey,
        forceCacheReloadTS: LocalDateTime?
    ): ValueSetItem? {
        if (reloadNeeded(key, forceCacheReloadTS)) {
            valueSetCache.invalidate(key)
        }
        return valueSetCache.get(key) {
            // if not found in cache, calculate and store
            registry.find {
                it.getRegistryItemType() == key.registryType &&
                    it.data_element == key.elementName &&
                    it.profile_url == key.profileUrl
            }?.let {
                val valueSetItem = ValueSetItem(set = getValueSetData(it.filename))
                itemLastUpdated[key] = LocalDateTime.now()
                valueSetItem
            }
        }
    }

    internal fun getNewRegistry(): List<NormalizationRegistryItem> {
        return try {
            registryLastUpdated = LocalDateTime.now()
            JacksonUtil.readJsonList(
                ociClient.getObjectFromINFX(registryFileName)!!,
                NormalizationRegistryItem::class
            )
        } catch (e: Exception) {
            logger.error { "Failed to load normalization registry: ${e.message}" }
            registryLastUpdated = LocalDateTime.MIN // reset
            registry // keep the 'old' registry in place
        }
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
                mutableMap[SourceKey(sourceCode, agnosticSourceSystem)] =
                    TargetValue(targetCode, targetSystem, targetDisplay, targetVersion)
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
     * @param elementName the name of the element being mapped, i.e. "Appointment.status" or "Patient.telecom.use".
     * @param profileUrl URL of an RCDM or FHIR profile
     */
    fun getValueSet(
        elementName: String,
        profileUrl: String,
        forceCacheReloadTS: LocalDateTime? = null
    ): List<Coding> {
        val cacheKey = CacheKey(
            registryType = NormalizationRegistryItem.RegistryType.ValueSet,
            elementName = elementName,
            profileUrl = profileUrl
        )
        getValueSetItem(cacheKey, forceCacheReloadTS)
        return getValueSetRegistryItemValues(
            getValueSetItem(cacheKey, forceCacheReloadTS)
        )
    }

    fun getRequiredValueSet(
        elementName: String,
        profileUrl: String,
        forceCacheReloadTS: LocalDateTime? = null
    ): List<Coding> {
        return getValueSet(elementName, profileUrl, forceCacheReloadTS).takeIf { it.isNotEmpty() }
            ?: throw MissingNormalizationContentException("Required value set for $profileUrl and $elementName not found")
    }

    private fun getValueSetRegistryItemValues(valueSetItem: ValueSetItem?): List<Coding> {
        return valueSetItem?.set?.map {
            Coding(
                system = Uri(it.system),
                code = Code(it.value),
                display = it.display.asFHIR(),
                version = it.version.asFHIR()
            )
        } ?: emptyList()
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

internal data class CacheKey(
    val registryType: NormalizationRegistryItem.RegistryType,
    val elementName: String,
    val tenantId: String? = null,
    val profileUrl: String? = null
)

internal data class NormalizationRegistryItem(
    val registry_uuid: String,
    val data_element: String, // i.e. 'Appointment.status'
    val filename: String,
    val version: String? = null,
    val source_extension_url: String? = null, // non-null for ConceptMap
    val resource_type: String, // i.e. 'Appointment' - repeated in data_element
    val tenant_id: String? = null, // null applies to all tenants
    val profile_url: String? = null,
    val concept_map_name: String? = null,
    val concept_map_uuid: String? = null,
    val value_set_name: String? = null,
    val value_set_uuid: String? = null,
    val registry_entry_type: String? = null
) {
    enum class RegistryType {
        ConceptMap,
        ValueSet
    }

    fun getRegistryItemType(): RegistryType {
        return if (registry_entry_type == "concept_map") {
            RegistryType.ConceptMap
        } else {
            RegistryType.ValueSet
        }
    }
}

internal data class ConceptMapItem(
    val map: Map<SourceKey, TargetValue>?,
    val source_extension_url: String?
)

internal data class ValueSetItem(
    val set: List<TargetValue>?
)

internal data class SourceKey(val value: String, val system: String)
internal data class TargetValue(val value: String, val system: String, val display: String, val version: String)
