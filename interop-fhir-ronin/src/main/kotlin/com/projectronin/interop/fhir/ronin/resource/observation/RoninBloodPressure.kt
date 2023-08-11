package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.isInValueSet
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RoninBloodPressure(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninVitalSign(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_BLOOD_PRESSURE.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_24_1
    override val profileVersion = 4

    // Multipart qualifying codes for RoninBloodPressure
    fun validSystolicCodes(): ValueSetList = registryClient.getRequiredValueSet(
        "Observation.component:systolic.code",
        profile
    )
    fun validDiastolicCodes(): ValueSetList = registryClient.getRequiredValueSet(
        "Observation.component:diastolic.code",
        profile
    )

    // Quantity unit codes - [USCore Blood Pressure Units](http://hl7.org/fhir/us/core/STU5.0.1/StructureDefinition-us-core-blood-pressure.html)
    override val validQuantityCodes = listOf("mm[Hg]")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninBloodPressure
    override val validBasedOnValues = listOf("CarePlan", "MedicationRequest")
    override val validPartOfValues = listOf("MedicationStatement", "Procedure")
    override val validDerivedFromValues = listOf("DocumentReference")
    override val validHasMemberValues = listOf("MolecularSequence", "Observation", "QuestionnaireResponse")

    override fun validateVitalSign(element: Observation, parentContext: LocationContext, validation: Validation) {
        if (element.dataAbsentReason == null) {
            val components = element.component
            val systolic = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(validSystolicCodes().codes) } ?: false
            }
            val diastolic = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(validDiastolicCodes().codes) } ?: false
            }

            if (systolic.size == 1) {
                validateVitalSignValue(
                    systolic.first().value,
                    validQuantityCodes,
                    LocationContext("Observation", "component:systolic"),
                    validation
                )
            }
            if (diastolic.size == 1) {
                validateVitalSignValue(
                    diastolic.first().value,
                    validQuantityCodes,
                    LocationContext("Observation", "component:diastolic"),
                    validation
                )
            }
            validation.apply {
                val componentSystolicCodeContext = LocationContext("Observation", "component:systolic.code")
                checkTrue(
                    systolic.isNotEmpty(),
                    FHIRError(
                        code = "USCORE_BPOBS_001",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        validSystolicCodes().codes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = componentSystolicCodeContext,
                        metadata = listOf(validSystolicCodes().metadata!!)
                    ),
                    parentContext
                )
                checkTrue(
                    systolic.size <= 1,
                    FHIRError(
                        code = "USCORE_BPOBS_004",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Only 1 entry is allowed for systolic blood pressure",
                        location = componentSystolicCodeContext,
                        metadata = listOf(validSystolicCodes().metadata!!)
                    ),
                    parentContext
                )

                val componentDiastolicCodeContext = LocationContext("Observation", "component:diastolic.code")
                checkTrue(
                    diastolic.isNotEmpty(),
                    FHIRError(
                        code = "USCORE_BPOBS_002",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        validDiastolicCodes().codes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = componentDiastolicCodeContext,
                        metadata = listOf(validDiastolicCodes().metadata!!)
                    ),
                    parentContext
                )
                checkTrue(
                    diastolic.size <= 1,
                    FHIRError(
                        code = "USCORE_BPOBS_005",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Only 1 entry is allowed for diastolic blood pressure",
                        location = componentDiastolicCodeContext,
                        metadata = listOf(validDiastolicCodes().metadata!!)
                    ),
                    parentContext
                )
            }
        }
    }

    private val requiredIdError = RequiredFieldError(Observation::id)

    override fun transformInternal(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Observation?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(transformed, validation)
    }
}
