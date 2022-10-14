package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

object RoninConditionEncounterDiagnosis :
    USCoreBasedProfile<Condition>(
        R4ConditionValidator,
        RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value
    ) {
    private val qualifyingCode = Code("encounter-diagnosis")

    override fun qualifies(resource: Condition): Boolean {
        return resource.category.any { codeableConcept ->
            codeableConcept.coding.any { coding ->
                coding.system == CodeSystem.CONDITION_CATEGORY.uri && coding.code == qualifyingCode
            }
        }
    }

    override fun validateRonin(element: Condition, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, validation)
        }
    }

    private val requiredCodeError = RequiredFieldError(Condition::code)
    private val noEncounterDiagnosisCategoryError = FHIRError(
        code = "USCORE_CND_ENC_DX_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Encounter diagnosis condition category required for US Core Condition Encounter Diagnosis profile",
        location = LocationContext(Condition::category)
    )

    override fun validateUSCore(element: Condition, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.code, requiredCodeError, parentContext)

            checkTrue(
                element.category.any { codeableConcept ->
                    codeableConcept.coding.any { coding ->
                        coding.system == CodeSystem.CONDITION_CATEGORY.uri && coding.code == qualifyingCode
                    }
                },
                noEncounterDiagnosisCategoryError,
                parentContext
            )
        }
    }

    private val requiredIdError = RequiredFieldError(Condition::id)

    override fun transformInternal(
        original: Condition,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Condition?, Validation> {
        val validation = validation {
            checkNotNull(original.id, requiredIdError, parentContext)
        }

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta.transform(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + original.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            clinicalStatus = original.clinicalStatus?.localize(tenant),
            verificationStatus = original.verificationStatus?.localize(tenant),
            category = original.category.map { it.localize(tenant) },
            severity = original.severity?.localize(tenant),
            code = original.code?.localize(tenant),
            bodySite = original.bodySite.map { it.localize(tenant) },
            subject = original.subject?.localize(tenant),
            encounter = original.encounter?.localize(tenant),
            recorder = original.recorder?.localize(tenant),
            asserter = original.asserter?.localize(tenant),
            stage = original.stage.map { it.localize(tenant) },
            evidence = original.evidence.map { it.localize(tenant) },
            note = original.note.map { it.localize(tenant) }
        )
        return Pair(transformed, validation)
    }
}
