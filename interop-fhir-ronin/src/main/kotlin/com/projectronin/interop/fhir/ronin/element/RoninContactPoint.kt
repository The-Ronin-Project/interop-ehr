package com.projectronin.interop.fhir.ronin.element

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.ronin.error.ConceptMapInvalidValueSetError
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.util.getCodedEnumOrNull
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and Transformer for a list of ContactPoint elements in Patient, Practitioner, or Organization.
 */
@Component
class RoninContactPoint(private val registryClient: NormalizationRegistryClient) {
    private val requiredTelecomSystemExtensionError = FHIRError(
        code = "RONIN_CNTCTPT_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant source telecom system extension is required",
        location = LocationContext(ContactPoint::system)
    )
    private val wrongTelecomSystemExtensionError = FHIRError(
        code = "RONIN_CNTCTPT_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant source telecom system extension is defined without proper URL",
        location = LocationContext(ContactPoint::system)
    )
    private val requiredTelecomUseExtensionError = FHIRError(
        code = "RONIN_CNTCTPT_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant source telecom use extension is required",
        location = LocationContext(ContactPoint::use)
    )
    private val wrongTelecomUseExtensionError = FHIRError(
        code = "RONIN_CNTCTPT_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant source telecom use extension is defined without proper URL",
        location = LocationContext(ContactPoint::use)
    )

    fun validateRonin(element: List<ContactPoint>, parentContext: LocationContext, validation: Validation): Validation {
        validation.apply {
            element.forEachIndexed { index, telecom ->
                val currentContext = parentContext.append(LocationContext("", "telecom[$index]"))

                // null system is checked by R4ContactPointValidator

                ifNotNull(telecom.system) {
                    val extension = telecom.system?.extension
                    checkNotNull(extension, requiredTelecomSystemExtensionError, currentContext)
                    checkTrue(extension.size == 1, requiredTelecomSystemExtensionError, currentContext)
                    if (extension.size == 1) {
                        checkTrue(
                            (extension.first().url?.value == RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value),
                            wrongTelecomSystemExtensionError,
                            currentContext
                        )
                    }
                }

                // use is allowed to be null

                ifNotNull(telecom.use) {
                    val extension = telecom.use?.extension
                    checkNotNull(extension, requiredTelecomUseExtensionError, currentContext)
                    checkTrue(extension.size == 1, requiredTelecomUseExtensionError, currentContext)
                    if (extension.size == 1) {
                        checkTrue(
                            (extension.first().url?.value == RoninExtension.TENANT_SOURCE_TELECOM_USE.value),
                            wrongTelecomUseExtensionError,
                            currentContext
                        )
                    }
                }
            }
        }
        return validation
    }

    private val requiredTelecomSystemError = RequiredFieldError(ContactPoint::system)
    private val requiredTelecomValueError = RequiredFieldError(ContactPoint::value)

    fun validateUSCore(
        element: List<ContactPoint>,
        parentContext: LocationContext,
        validation: Validation
    ): Validation {
        validation.apply {
            element.forEachIndexed { index, telecom ->
                val currentContext = parentContext.append(LocationContext("", "telecom[$index]"))
                checkNotNull(telecom.system, requiredTelecomSystemError, currentContext)
                checkNotNull(telecom.value, requiredTelecomValueError, currentContext)
            }
        }
        return validation
    }

    private val requiredTelecomSystemWarning = FHIRError(
        code = "RONIN_CNTCTPT_005",
        severity = ValidationIssueSeverity.WARNING,
        description = "telecom filtered for no system",
        location = LocationContext(ContactPoint::system)
    )
    private val requiredTelecomValueWarning = FHIRError(
        code = "RONIN_CNTCTPT_006",
        severity = ValidationIssueSeverity.WARNING,
        description = "telecom filtered for no value",
        location = LocationContext(ContactPoint::value)
    )

    fun <T : Resource<T>> transform(
        element: List<ContactPoint>,
        resource: T,
        tenant: Tenant,
        parentContext: LocationContext,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime? = null
    ): Pair<List<ContactPoint>, Validation> {
        val systemMapName = RoninConceptMap.CODE_SYSTEMS.toUriString(tenant, "ContactPoint.system")
        val useMapName = RoninConceptMap.CODE_SYSTEMS.toUriString(tenant, "ContactPoint.use")
        val transformed = element.mapIndexedNotNull { index, contactPoint ->
            val telecomContext = parentContext.append(LocationContext("", "telecom[$index]"))

            if (contactPoint.value == null) {
                validation.checkTrue(false, requiredTelecomValueWarning, telecomContext)
                return@mapIndexedNotNull null
            }
            if (contactPoint.system == null) {
                validation.checkTrue(false, requiredTelecomSystemWarning, telecomContext)
                return@mapIndexedNotNull null
            }

            val systemContext = telecomContext.append(LocationContext(ContactPoint::system))
            val mappedSystem = contactPoint.system?.value?.let { systemValue ->
                val systemCode = registryClient.getConceptMappingForEnum(
                    tenant,
                    "${parentContext.element}.telecom.system",
                    RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", systemValue),
                    ContactPointSystem::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                    resource,
                    forceCacheReloadTS
                )
                validation.apply {
                    checkNotNull(
                        systemCode,
                        FailedConceptMapLookupError(
                            systemContext,
                            systemValue,
                            systemMapName,
                            systemCode?.metadata
                        ),
                        parentContext
                    )
                }
                if (systemCode == null) {
                    contactPoint.system
                } else {
                    val systemTarget = systemCode.coding.code?.value
                    validation.apply {
                        checkNotNull(
                            getCodedEnumOrNull<ContactPointSystem>(systemTarget),
                            ConceptMapInvalidValueSetError(
                                systemContext,
                                systemMapName,
                                systemValue,
                                systemTarget,
                                systemCode.metadata
                            ),
                            parentContext
                        )
                    }
                    Code(value = systemTarget, extension = listOf(systemCode.extension))
                }
            }

            val useContext = telecomContext.append(LocationContext(ContactPoint::use))
            val mappedUse = contactPoint.use?.value?.let { useValue ->
                val useCode = registryClient.getConceptMappingForEnum(
                    tenant,
                    "${parentContext.element}.telecom.use",
                    RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.use", useValue),
                    ContactPointUse::class,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                    resource,
                    forceCacheReloadTS
                )
                validation.apply {
                    checkNotNull(
                        useCode,
                        FailedConceptMapLookupError(useContext, useValue, useMapName, useCode?.metadata),
                        parentContext
                    )
                }
                if (useCode == null) {
                    contactPoint.use
                } else {
                    val useTarget = useCode.coding.code?.value
                    validation.apply {
                        checkNotNull(
                            getCodedEnumOrNull<ContactPointUse>(useTarget),
                            ConceptMapInvalidValueSetError(
                                useContext,
                                useMapName,
                                useValue,
                                useTarget,
                                useCode.metadata
                            ),
                            parentContext
                        )
                    }
                    Code(value = useTarget, extension = listOf(useCode.extension))
                }
            }
            contactPoint.copy(system = mappedSystem, use = mappedUse)
        }

        return Pair(transformed, validation)
    }
}
