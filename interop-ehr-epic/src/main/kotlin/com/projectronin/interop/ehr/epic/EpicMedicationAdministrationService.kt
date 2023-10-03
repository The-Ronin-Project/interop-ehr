package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.interop.ehr.MedicationAdministrationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.resource.MedicationAdministrationDosage
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.unlocalize
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import datadog.trace.api.Trace
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Service providing access to Encounters within Epic.
 */
@Component
class EpicMedicationAdministrationService(
    private val epicClient: EpicClient,
    private val ehrDataAuthorityClient: EHRDataAuthorityClient,
    private val identifierService: EpicIdentifierService
) : MedicationAdministrationService {
    val urlSearchPart = "/api/epic/2014/Clinical/Patient/GETMEDICATIONADMINISTRATIONHISTORY/MedicationAdministration"

    @Trace
    override fun findMedicationAdministrationsByPatient(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<MedicationAdministration> {
        return listOf() // Not implemented for Epic
    }

    override fun findMedicationAdministrationsByRequest(
        tenant: Tenant,
        medicationRequest: MedicationRequest
    ): List<MedicationAdministration> {
        val patientID = medicationRequest.subject?.decomposedId()?.unlocalize(tenant) ?: return emptyList()
        val encounterID = medicationRequest.encounter?.decomposedId()?.unlocalize(tenant) ?: return emptyList()
        val medicationReference = medicationRequest.medication?.value ?: return emptyList()
        val medicationID = (medicationReference as Reference).decomposedId()?.unlocalize(tenant)
        val medicationRequestID = medicationRequest.findFhirId() ?: return emptyList()
        val patient = runBlocking {
            ehrDataAuthorityClient.getResource(
                tenant.mnemonic,
                "Patient",
                patientID.localize(tenant)
            )
        } ?: return emptyList()
        val mrn = identifierService.getPatientIdentifier(tenant, (patient as Patient).identifier).value?.value
            ?: return emptyList()
        val encounter = runBlocking {
            ehrDataAuthorityClient.getResource(
                tenant.mnemonic,
                "Encounter",
                encounterID.localize(tenant)
            )
        } ?: return emptyList()
        val csn = (encounter as Encounter).identifier.firstOrNull {
            it.system == Uri(tenant.vendorAs<Epic>().encounterCSNSystem)
        }?.value?.value ?: return emptyList()

        // can replace this logic once we store order system in tenant config
        val orderID = medicationRequest.identifier.firstOrNull {
            it.system != CodeSystem.RONIN_FHIR_ID.uri &&
                it.system != CodeSystem.RONIN_TENANT.uri &&
                it.system != CodeSystem.RONIN_DATA_AUTHORITY.uri
        }?.value?.value ?: return emptyList()

        val request = EpicMedAdminRequest(
            patientID = mrn,
            patientIDType = "Internal",
            contactID = csn,
            contactIDType = "CSN",
            orderIDs = listOf(EpicOrderID(orderID, "External"))
        )

        val response = runBlocking {
            epicClient.post(tenant, urlSearchPart, request).body<EpicMedAdmin>()
        }

        return response.orders.flatMap { epicMedOrder ->
            epicMedOrder.medicationAdministrations.mapNotNull { epicMedAdmin ->
                if (epicMedAdmin.administrationInstant.isEmpty()) {
                    null
                } else {
                    MedicationAdministration(
                        id = Id("$orderID-${Instant.parse(epicMedAdmin.administrationInstant).epochSecond}"),
                        status = Code(epicMedAdmin.action), // TODO: determine if we should do concept mapping here or in interop-fhir
                        medication = DynamicValue(
                            type = DynamicValueType.CODEABLE_CONCEPT,
                            value = CodeableConcept(
                                text = FHIRString(epicMedOrder.name)
                            )
                        ),
                        subject = Reference(reference = FHIRString("Patient/$patientID")),
                        effective = DynamicValue(
                            type = DynamicValueType.DATE_TIME,
                            value = DateTime(epicMedAdmin.administrationInstant)
                        ),
                        request = Reference(reference = FHIRString("MedicationRequest/$medicationRequestID")),
                        dosage = MedicationAdministrationDosage(
                            dose = Quantity(value = Decimal(BigDecimal(epicMedAdmin.dose.value)), unit = FHIRString(epicMedAdmin.dose.unit))
                        )
                    )
                }
            }
        }
    }
}

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicMedAdmin(
    val orders: List<EpicMedicationOrder> = emptyList()
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicMedicationOrder(
    val medicationAdministrations: List<EpicMedicationAdministration> = emptyList(),
    @JsonProperty("Name")
    val name: String = ""
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicMedicationAdministration(
    val administrationInstant: String,
    val action: String,
    val dose: EpicDose
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicDose(
    val value: String,
    val unit: String
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicMedAdminRequest(
    @JsonProperty("PatientID")
    val patientID: String,
    @JsonProperty("PatientIDType")
    val patientIDType: String,
    @JsonProperty("ContactID")
    val contactID: String,
    @JsonProperty("ContactIDType")
    val contactIDType: String,
    @JsonProperty("OrderIDs")
    val orderIDs: List<EpicOrderID>
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicOrderID(
    @JsonProperty("ID")
    val ID: String,
    val type: String
)
