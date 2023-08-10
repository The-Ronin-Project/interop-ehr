package com.projectronin.interop.fhir.ronin.normalization

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
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
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata
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
 * 4. When multiple concept maps match an elementName (Ex: Observation.code)
 *    merge element lists from all matches into 1 concept map and use that.
 */

@Component
class NormalizationRegistryClient(
    private val ociClient: OCIClient,
    @Value("\${oci.infx.registry.file:DataNormalizationRegistry/v3/registry.json}")
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
     * Get a CodeableConcept mapping from the DataNormalizationRegistry.
     * The input CodeableConcept.coding value and system values must not be null.
     * Return a [Triple] with the transformed [CodeableConcept] as the first, a CodeableConcept [Extension] as the
     * second, and the [ConceptMapMetadata] as the third
     * or null if no such mapping could be found. The [Extension] represents the original value before mapping.
     */
    fun getConceptMapping(
        tenant: Tenant,
        elementName: String,
        codeableConcept: CodeableConcept,
        forceCacheReloadTS: LocalDateTime? = null
    ): ConceptMapCodeableConcept? {
        val cacheKey = CacheKey(
            registryType = NormalizationRegistryItem.RegistryType.ConceptMap,
            elementName = elementName,
            tenantId = tenant.mnemonic
        )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS)
        return registryItem?.let { codeableConcept.getConceptMapping(registryItem) }
    }

    /**
     * Get a Coding mapping from the DataNormalizationRegistry.
     * The input Coding value and system must not be null.
     * Return a [Triple] with the transformed [Coding] as the first, a Coding [Extension] as the second, and
     * the [ConceptMapMetadata] as the third
     * or null to match any element of the type [elementName].
     */
    fun getConceptMapping(
        tenant: Tenant,
        elementName: String,
        coding: Coding,
        forceCacheReloadTS: LocalDateTime? = null
    ): ConceptMapCoding? {
        val cacheKey = CacheKey(
            registryType = NormalizationRegistryItem.RegistryType.ConceptMap,
            elementName = elementName,
            tenantId = tenant.mnemonic
        )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS)
        return registryItem?.let { coding.getConceptMapping(registryItem) }
    }

    /**
     * Get a concept map from the DataNormalizationRegistry whose result matches an enum class.
     * Returns a [Triple] with the transformed [Coding] as the first, an [Extension] as the second,
     * the [ConceptMapMetadata] as the third, or null if no such mapping could be found.
     * The [Extension] represents the original value before mapping.
     * The enum class enumerates the values the caller can expect as return values from this concept map.
     * If there is no concept map found, but the input Coding.code value is correct for the enumClass,
     * return the input [Coding] with a source [Extension] using the enumExtensionUrl provided by the caller.
     */
    fun <T : CodedEnum<T>> getConceptMappingForEnum(
        tenant: Tenant,
        elementName: String,
        coding: Coding,
        enumClass: KClass<T>,
        enumExtensionUrl: String,
        forceCacheReloadTS: LocalDateTime? = null
    ): ConceptMapCoding? {
        val cacheKey = CacheKey(
            registryType = NormalizationRegistryItem.RegistryType.ConceptMap,
            elementName = elementName,
            tenantId = tenant.mnemonic
        )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS)
        val codedEnum = enumClass.java.enumConstants.find { it.code == coding.code?.value }
        return codedEnum?.let {
            ConceptMapCoding(coding, createCodingExtension(enumExtensionUrl, coding), registryItem?.metadata)
        } ?: registryItem?.let { coding.getConceptMapping(registryItem) }
    }

    /**
     * Get a value set from the DataNormalizationRegistry.
     * Returns a [Pair] of a list of [Coding] and [ValueSetMetadata] or null if no such value set could be found.
     * @param elementName the name of the element being mapped, i.e.
     *        "Appointment.status" or "Patient.telecom.use".
     * @param profileUrl URL of an RCDM or FHIR profile
     */
    fun getValueSet(
        elementName: String,
        profileUrl: String,
        forceCacheReloadTS: LocalDateTime? = null
    ): ValueSetList {
        val cacheKey = CacheKey(
            registryType = NormalizationRegistryItem.RegistryType.ValueSet,
            elementName = elementName,
            profileUrl = profileUrl
        )
        return getValueSetRegistryItemValues(
            getValueSetItem(cacheKey, forceCacheReloadTS)
        )
    }

    /**
     * Get a value set from the DataNormalizationRegistry, when that value set
     * is required (not preferred or example) for that element, per the profile.
     * Returns a [Pair] of a list of [Coding] and [ValueSetMetadata] or null if no such value set could be found.
     * @param elementName the name of the element being mapped, i.e.
     *        "Appointment.status" or "Patient.telecom.use".
     * @param profileUrl URL of an RCDM or FHIR profile
     * @throws MissingNormalizationContentException if value set is not found.
     */
    fun getRequiredValueSet(
        elementName: String,
        profileUrl: String,
        forceCacheReloadTS: LocalDateTime? = null
    ): ValueSetList {
        return getValueSet(elementName, profileUrl, forceCacheReloadTS).takeIf { it.codes.isNotEmpty() }
            ?: throw MissingNormalizationContentException("Required value set for $profileUrl and $elementName not found")
    }

    /**
     * Find a CodeableConcept mapping in the DataNormalizationRegistry that
     * matches the Coding entries in the input CodeableConcept. The match is on
     * system and value; other Coding attributes are ignored. Coding lists that
     * match must contain the same system and value pairs, but in any order.
     *
     * When a match is found, the returned CodeableConcept will contain Coding
     * entry(ies) with these 4 attributes only: system, code, display, version.
     * These attributes come from the group.source, target.code, target.display,
     * and group.targetVersion values from the DataNormalizationRegistry.
     * also see readGroupElementCode()
     */
    private fun CodeableConcept.getConceptMapping(conceptMapItem: ConceptMapItem): ConceptMapCodeableConcept? {
        val sourceConcept = getSourceConcept()
        val target = conceptMapItem.map[sourceConcept] ?: return null

        // if elementName is a CodeableConcept datatype: expect 1+ target.element
        return ConceptMapCodeableConcept(
            this.copy(
                text = target.text?.asFHIR(),
                coding = target.element.map {
                    Coding(
                        system = Uri(it.system),
                        code = Code(it.value),
                        display = it.display.asFHIR(),
                        version = it.version.asFHIR()
                    )
                }
            ),
            createCodeableConceptExtension(conceptMapItem, this),
            conceptMapItem.metadata
        )
    }

    private fun CodeableConcept.getSourceConcept(): SourceConcept? {
        val sourceKeys = coding.map {
            it.getSourceKey() ?: return null
        }.toSet()
        return SourceConcept(sourceKeys)
    }

    /**
     * Find a Coding mapping in the DataNormalizationRegistry that matches the
     * input Coding system and value. Matching ignores other attributes.
     *
     * When a match is found, the returned Coding will contain these attributes
     * only: system, code, display, version.These attributes come from the
     * group.source, target.code, target.display, and group.targetVersion
     * values from the DataNormalizationRegistry.
     */
    private fun Coding.getConceptMapping(conceptMapItem: ConceptMapItem): ConceptMapCoding? {
        val sourceConcept = getSourceConcept() ?: return null
        val target = conceptMapItem.map[sourceConcept] ?: return null

        // if elementName is a Code or Coding datatype: expect 1 target.element
        return target.element.first().let {
            ConceptMapCoding(
                Coding(
                    system = Uri(it.system),
                    code = Code(it.value),
                    display = it.display.asFHIR(),
                    version = it.version.asFHIR()
                ),
                createCodingExtension(conceptMapItem, this),
                conceptMapItem.metadata
            )
        }
    }

    private fun Coding.getSourceConcept(): SourceConcept? = getSourceKey()?.let { SourceConcept(setOf(it)) }

    private fun Coding.getSourceKey(): SourceKey? {
        val sourceVal = this.code?.value ?: return null
        val sourceSystem = this.system?.value ?: return null
        val agnosticSourceSystem = getTenantAgnosticCodeSystem(sourceSystem)
        return SourceKey(sourceVal, agnosticSourceSystem)
    }

    private fun createCodeableConceptExtension(conceptMapItem: ConceptMapItem, codeableConcept: CodeableConcept) =
        Extension(
            url = Uri(conceptMapItem.source_extension_url),
            value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = codeableConcept)
        )

    private fun createCodingExtension(conceptMapItem: ConceptMapItem, coding: Coding) = Extension(
        url = Uri(conceptMapItem.source_extension_url),
        value = DynamicValue(type = DynamicValueType.CODING, value = coding)
    )

    private fun createCodingExtension(extensionUrl: String, coding: Coding) = Extension(
        url = Uri(extensionUrl),
        value = DynamicValue(type = DynamicValueType.CODING, value = coding)
    )

    private fun getConceptMapItem(
        key: CacheKey,
        forceCacheReloadTS: LocalDateTime?
    ): ConceptMapItem? {
        if (reloadNeeded(key, forceCacheReloadTS)) {
            conceptMapCache.invalidate(key)
        }

        val cachedItem = conceptMapCache.getIfPresent(key)
        return if (cachedItem == null) {
            val matchingItems = registry.filter {
                it.getRegistryItemType() == key.registryType &&
                    it.data_element == key.elementName &&
                    it.tenant_id == key.tenantId
            }
            if (matchingItems.isEmpty()) {
                null
            } else {
                val concatenated = ConceptMapItem(
                    matchingItems.map { item ->
                        getConceptMapData(item.filename).entries
                    }.flatten().associate { it.toPair() },
                    matchingItems.map { it.source_extension_url }
                        .distinct().singleOrNull()
                        ?: throw MissingNormalizationContentException(
                            "Concept map(s) for tenant '${matchingItems.first().tenant_id}' and ${matchingItems.first().data_element} have missing or inconsistent source extension URLs"
                        ),
                    matchingItems.map { item ->
                        ConceptMapMetadata(
                            registryEntryType = item.registry_entry_type!!,
                            conceptMapName = item.concept_map_name!!,
                            conceptMapUuid = item.concept_map_uuid!!,
                            version = item.version!!
                        )
                    }
                )
                itemLastUpdated[key] = LocalDateTime.now()
                conceptMapCache.put(key, concatenated)
                concatenated
            }
        } else {
            cachedItem
        }
    }

    private fun getTenantAgnosticCodeSystem(system: String): String =
        if (RoninConceptMap.CODE_SYSTEMS.isMappedUri(system)) {
            RoninConceptMap.CODE_SYSTEMS.toTenantAgnosticUri(system)
        } else {
            system
        }

    /**
     * Read a ConceptMap group.element.code value into a SourceConcept so that
     * we can match it against the Coding entries from a source CodeableConcept.
     * group.element.code may be a simple code value, but for maps of
     * CodeableConcept to CodeableConcept it will contain a JSON-representation of the concept.
     */
    private fun readGroupElementCode(
        groupElementCode: String,
        tenantAgnosticSourceSystem: String
    ): SourceConcept {
        if (!groupElementCode.contains("{")) {
            return SourceConcept(setOf(SourceKey(groupElementCode, tenantAgnosticSourceSystem)))
        }

        val conceptMapCode = JacksonManager.objectMapper.readValue<ConceptMapCode>(groupElementCode)
        return conceptMapCode.valueCodeableConcept.getSourceConcept()
            ?: throw IllegalStateException("Could not create SourceConcept from $groupElementCode")
    }

    private data class ConceptMapCode(val valueCodeableConcept: CodeableConcept)

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
                val metadata = ValueSetMetadata(
                    registryEntryType = "value-set",
                    valueSetName = it.value_set_name!!,
                    valueSetUuid = it.value_set_uuid!!,
                    version = it.version!!
                )
                val valueSetItem = ValueSetItem(set = getValueSetData(it.filename), metadata)
                itemLastUpdated[key] = LocalDateTime.now()
                valueSetItem
            }
        }
    }

    private fun getValueSetRegistryItemValues(valueSetItem: ValueSetItem?): ValueSetList {
        return ValueSetList(
            valueSetItem?.set?.map {
                Coding(
                    system = Uri(it.system),
                    code = Code(it.value),
                    display = it.display.asFHIR(),
                    version = it.version.asFHIR()
                )
            } ?: emptyList(),
            valueSetItem?.metadata
        )
    }

    /**
     * Checks if the cached item needs a reload. Also reloads registry if needed
     */
    private fun reloadNeeded(key: CacheKey, forceCacheReloadTS: LocalDateTime?): Boolean {
        val defaultReloadTime = LocalDateTime.now().minusHours(defaultReloadHours.toLong())
        if (registryLastUpdated.isBefore(forceCacheReloadTS ?: defaultReloadTime)) {
            registry = getNewRegistry()
        }
        return itemLastUpdated[key]?.isBefore(forceCacheReloadTS ?: defaultReloadTime) ?: true
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

    internal fun getConceptMapData(filename: String): Map<SourceConcept, TargetConcept> {
        val conceptMap = try {
            JacksonUtil.readJsonObject(
                ociClient.getObjectFromINFX(filename)!!,
                ConceptMap::class
            )
        } catch (e: Exception) {
            logger.info { e.message }
            return emptyMap()
        }
        // squish ConceptMap into more usable form
        val mutableMap = mutableMapOf<SourceConcept, TargetConcept>()
        conceptMap.group.forEach forEachGroup@{ group ->
            val targetSystem = group.target?.value ?: return@forEachGroup
            val sourceSystem = group.source?.value ?: return@forEachGroup
            val targetVersion = group.targetVersion?.value ?: return@forEachGroup
            val agnosticSourceSystem = getTenantAgnosticCodeSystem(sourceSystem)
            group.element?.forEach forEachElement@{ element ->
                val targetText = element.display?.value ?: return@forEachElement
                val sourceCode = element.code?.value ?: return@forEachElement
                val targetList = element.target.mapNotNull { target ->
                    target.code?.value?.let { targetCode ->
                        target.display?.value?.let { targetDisplay ->
                            TargetValue(
                                targetCode,
                                targetSystem,
                                targetDisplay,
                                targetVersion
                            )
                        }
                    }
                }
                val targetConcept = TargetConcept(targetList, targetText)
                val sourceConcept = readGroupElementCode(sourceCode, agnosticSourceSystem)
                mutableMap[sourceConcept] = targetConcept
            }
        }
        return mutableMap
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
    val tenantId: String? = null, // non-null for ConceptMap
    val profileUrl: String? = null // null for ConceptMap
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
    val map: Map<SourceConcept, TargetConcept>,
    val source_extension_url: String, // non-null for ConceptMap
    val metadata: List<ConceptMapMetadata>
)

internal data class ValueSetItem(
    val set: List<TargetValue>?,
    val metadata: ValueSetMetadata
)

internal data class SourceKey(val value: String, val system: String)
internal data class TargetValue(val value: String, val system: String, val display: String, val version: String)
internal data class SourceConcept(val element: Set<SourceKey>)
internal data class TargetConcept(val element: List<TargetValue>, val text: String? = null)
