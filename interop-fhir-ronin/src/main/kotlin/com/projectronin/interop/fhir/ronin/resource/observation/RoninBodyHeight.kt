package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
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
class RoninBodyHeight(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninVitalSign(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_BODY_HEIGHT.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_26_1
    override val profileVersion = 3

    // Quantity unit codes - [USCore Body Length Units](http://hl7.org/fhir/R4/valueset-ucum-bodylength.html)
    override val validQuantityCodes = listOf("cm", "[in_i]")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninBodyHeight
    override val validBasedOnValues = listOf("CarePlan", "MedicationRequest")
    override val validDerivedFromValues = listOf("DocumentReference")
    override val validHasMemberValues = listOf("MolecularSequence", "Observation", "QuestionnaireResponse")
    override val validPartOfValues = listOf("MedicationStatement", "Procedure")

    private val noBodySiteError = FHIRError(
        code = "RONIN_HTOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "bodySite not allowed for Body Height observation",
        location = LocationContext(Observation::bodySite)
    )

    override fun validateVitalSign(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            validateVitalSignValue(element.value, parentContext, this)

            checkTrue(element.bodySite == null, noBodySiteError, parentContext)
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
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant),
            bodySite = null // bodySite should not be supplied for Body Height
        )
        return Pair(transformed, validation)
    }
}
