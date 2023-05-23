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
        profileUrl: String? = null
    ): Pair<Coding, Extension>? {
        val registryItem = getConceptMapRegistryItem(tenant, elementName, profileUrl) ?: return null
        return getConceptMapping(registryItem, coding)
    }

    /**
     * Returns a [Pair] with the transformed [Coding] as the first and an [Extension] as the second,
     * or null if no such mapping could be found. The [Extension] represents the original value before mapping.
     * Calling this will reload the [NormalizationRegistryCache] if necessary.
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
            .filter { it.concept_map_uuid?.isNotEmpty() == true }.find { it.data_element == elementName }
    }

    private fun createExtension(registryItem: NormalizationRegistryItem, coding: Coding) = Extension(
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
            val type = new.getRegistryItemType()
            // tenant on the new registry is the tenant we care about
            val requestedTenant = new.tenant_id in listOf(mnemonic, null)
            val old = currentRegistry.findMatching(new)
            // if we don't have an existing entry, or we do and the version is the same and it's for the
            // tenant we want to reload, load it from the registry
            if (old == null || (old.version != new.version && requestedTenant)) {
                when (type) {
                    NormalizationRegistryItem.RegistryType.ConceptMap -> {
                        new.map = getConceptMapData(new.filename)
                    }
                    NormalizationRegistryItem.RegistryType.ValueSet -> {
                        new.set = getValueSetData(new.filename)
                    }
                }
            } else {
                when (type) {
                    NormalizationRegistryItem.RegistryType.ConceptMap -> new.map = old.map
                    NormalizationRegistryItem.RegistryType.ValueSet -> new.set = old.set
                }
            }
        }

        // these are all new tenant registry objects that aren't in the current registry.
        // we don't want to persist these in the cache because we're not reloading for these tenants,
        // but we can't add them to the cache because we haven't loaded them yet and their map / set would be null
        val newOtherTenantRegistryItems =
            newRegistry
                .filter { it.tenant_id !in listOf(mnemonic, null) }
                .filter { currentRegistry.doesNotContain(it) }

        // don't persist new tenant registry items for tenants we didn't call
        val modifiedNewRegistry =
            newRegistry.filter { newOtherTenantRegistryItems.doesNotContain(it) }
        NormalizationRegistryCache.setNewRegistry(modifiedNewRegistry, mnemonic)
    }

    // given a list of NormalizationRegistryItem, find the one that matches the given normalization item
    private fun List<NormalizationRegistryItem>.findMatching(searchItem: NormalizationRegistryItem): NormalizationRegistryItem? {
        return this.find { it.registry_uuid == searchItem.registry_uuid }
    }

    private fun List<NormalizationRegistryItem>.doesNotContain(searchItem: NormalizationRegistryItem): Boolean {
        return this.findMatching(searchItem) == null
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
     * Calling this will reload the [NormalizationRegistryCache] if necessary.
     * @param tenant the current [Tenant], or null to match all tenants.
     * @param elementName the name of the element being mapped, i.e. "Appointment.status" or "Patient.telecom.use".
     * @param profileUrl URL of an RCDM or FHIR profile, or null to match any element of the type [elementName].
     */
    fun getValueSet(
        tenant: Tenant,
        elementName: String,
        profileUrl: String? = null
    ): List<Coding> {
        return getValueSetRegistryItemValues(
            getValueSetRegistryItem(tenant.mnemonic, elementName, profileUrl)
        )
    }

    fun getRequiredValueSet(elementName: String, profileUrl: String? = null): List<Coding> {
        return getValueSet(elementName, profileUrl).takeIf { it.isNotEmpty() }
            ?: throw MissingNormalizationContentException("Required value set for $profileUrl and $elementName not found")
    }

    fun getValueSet(
        elementName: String,
        profileUrl: String? = null
    ): List<Coding> {
        return getValueSetRegistryItemValues(
            getValueSetRegistryItem(tenantAgnosticRegistryKey, elementName, profileUrl)
        )
    }

    /**
     * Get a tenant-specific Ronin value set.
     * @param mnemonic 8-character identifier for the tenant.
     * @param elementName the name of the element being mapped, i.e. "Appointment.status" or "Patient.telecom.use".
     * @param profileUrl URL of an RCDM or FHIR profile, or null to match any element of the type [elementName].
     */
    private fun getValueSetRegistryItem(
        mnemonic: String,
        elementName: String,
        profileUrl: String? = null
    ): NormalizationRegistryItem? {
        if (NormalizationRegistryCache.reloadNeeded(mnemonic)) reload(mnemonic)
        val cache = NormalizationRegistryCache.getCurrentRegistry()
        return cache.filter { it.tenant_id in listOf(mnemonic, null) } // null tenant means universal set
            .filter { it.profile_url in listOf(profileUrl, null) } // null profile_url means universal set
            .filter { it.value_set_uuid?.isNotEmpty() == true }.find { it.data_element == elementName }
    }

    private fun getValueSetRegistryItemValues(registryItem: NormalizationRegistryItem?): List<Coding> {
        return registryItem?.set?.map {
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
