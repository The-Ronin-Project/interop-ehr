package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.validate.resource.R4AppointmentValidator
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.ronin.error.ConceptMapInvalidValueSetError
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.getCodedEnumOrNull
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Validator and Transformer for the Ronin Appointment profile.
 */
@Component
class RoninAppointment(private val registryClient: NormalizationRegistryClient, normalizer: Normalizer, localizer: Localizer) :
    USCoreBasedProfile<Appointment>(R4AppointmentValidator, RoninProfile.APPOINTMENT.value, normalizer, localizer) {

    private val requiredAppointmentExtensionError = FHIRError(
        code = "RONIN_APPT_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Appointment extension list may not be empty",
        location = LocationContext(Appointment::status)
    )
    private val invalidAppointmentStatusExtensionError = FHIRError(
        code = "RONIN_APPT_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant source appointment status extension is missing or invalid",
        location = LocationContext(Appointment::status)
    )

    override fun validateRonin(element: Appointment, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)
            // null status is checked by R4ContactPointValidator

            // extension - not empty - 1..*
            val extension = element.extension
            checkTrue(extension.isNotEmpty(), requiredAppointmentExtensionError, parentContext)

            // extension - status tenant source extension - 1..1
            if (extension.isNotEmpty()) {
                checkTrue(
                    extension.any {
                        it.url?.value == RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value &&
                            it.value?.type == DynamicValueType.CODING
                    },
                    invalidAppointmentStatusExtensionError,
                    parentContext
                )
            }
        }
    }

    /**
     * There is [no USCore profile for Appointment](https://build.fhir.org/ig/HL7/US-Core/profiles-and-extensions.html)
     */
    override fun validateUSCore(element: Appointment, parentContext: LocationContext, validation: Validation) {}

    override fun transformInternal(
        normalized: Appointment,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Appointment?, Validation> {
        val validation = Validation()

        val mappedStatusPair = normalized.status?.value?.let { statusValue ->
            val statusPair = registryClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "Appointment.status", statusValue),
                AppointmentStatus::class
            )
            validation.apply {
                checkNotNull(
                    statusPair,
                    FailedConceptMapLookupError(
                        LocationContext(Appointment::status),
                        statusValue,
                        RoninConceptMap.CODE_SYSTEMS.toUriString(tenant, "Appointment.status")
                    ),
                    parentContext
                )
                ifNotNull(statusPair) {
                    statusPair.first.let { coding ->
                        val statusCode = coding.code
                        val statusTarget = statusCode?.value
                        val statusMapName = RoninConceptMap.CODE_SYSTEMS.toUriString(tenant, "Appointment.status")
                        val statusContext = parentContext.append(LocationContext("Appointment", "status"))
                        validation.apply {
                            checkNotNull(
                                getCodedEnumOrNull<AppointmentStatus>(statusTarget),
                                ConceptMapInvalidValueSetError(statusContext, statusMapName, statusValue, statusTarget),
                                parentContext
                            )
                        }
                    }
                }
            }
            statusPair
        }
        val statusValue = mappedStatusPair?.first?.code
        val statusExtension = mappedStatusPair?.let { listOf(mappedStatusPair.second) } ?: emptyList()

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant),
            extension = normalized.extension + statusExtension,
            status = statusValue
        )
        return Pair(transformed, validation)
    }
}
