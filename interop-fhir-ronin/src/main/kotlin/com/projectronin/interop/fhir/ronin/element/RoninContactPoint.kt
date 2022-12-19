package com.projectronin.interop.fhir.ronin.element

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.ronin.conceptmap.ConceptMapClient
import com.projectronin.interop.fhir.ronin.error.ConceptMapInvalidValueSetError
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
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

/**
 * Validator and Transformer for a list of ContactPoint elements in Patient, Practitioner, or Organization.
 */
@Component
class RoninContactPoint(private val conceptMapClient: ConceptMapClient) {
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

    fun transform(
        element: List<ContactPoint>,
        tenant: Tenant,
        parentContext: LocationContext,
        validation: Validation
    ): Pair<List<ContactPoint>?, Validation> {
        val systemMapName = RoninConceptMap.CODE_SYSTEMS.toUriString(tenant, "ContactPoint.system")
        val useMapName = RoninConceptMap.CODE_SYSTEMS.toUriString(tenant, "ContactPoint.use")
        val transformed = element.mapIndexed { index, telecom ->
            val systemContext = LocationContext(parentContext.element, "telecom[$index].system")
            val mappedSystem = telecom.system?.value?.let { systemValue ->
                val systemPair = conceptMapClient.getConceptMappingForEnum(
                    tenant,
                    parentContext.element,
                    "${parentContext.element}.telecom.system",
                    RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", systemValue),
                    ContactPointSystem::class
                )
                validation.apply {
                    checkNotNull(
                        systemPair,
                        FailedConceptMapLookupError(systemContext, systemValue, systemMapName),
                        parentContext
                    )
                }
                if (systemPair == null) null else {
                    val systemTarget = systemPair.first.code?.value
                    validation.apply {
                        checkNotNull(
                            getCodedEnumOrNull<ContactPointSystem>(systemTarget),
                            ConceptMapInvalidValueSetError(systemContext, systemMapName, systemValue, systemTarget),
                            parentContext
                        )
                    }
                    Code(value = systemTarget, extension = listOf(systemPair.second))
                }
            }
            val useContext = LocationContext(parentContext.element, "telecom[$index].use")
            val mappedUse = telecom.use?.value?.let { useValue ->
                val usePair = conceptMapClient.getConceptMappingForEnum(
                    tenant,
                    parentContext.element,
                    "${parentContext.element}.telecom.use",
                    RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.use", useValue),
                    ContactPointUse::class
                )
                validation.apply {
                    checkNotNull(
                        usePair,
                        FailedConceptMapLookupError(useContext, useValue, useMapName),
                        parentContext
                    )
                }
                if (usePair == null) null else {
                    val useTarget = usePair.first.code?.value
                    validation.apply {
                        checkNotNull(
                            getCodedEnumOrNull<ContactPointUse>(useTarget),
                            ConceptMapInvalidValueSetError(useContext, useMapName, useValue, useTarget),
                            parentContext
                        )
                    }
                    Code(value = useTarget, extension = listOf(usePair.second))
                }
            }
            ContactPoint(
                id = telecom.id,
                extension = telecom.extension,
                system = mappedSystem,
                value = telecom.value,
                use = mappedUse,
                rank = telecom.rank,
                period = telecom.period,
            )
        }
        return Pair(transformed, validation)
    }
}
