package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.interop.ehr.MedicationAdministrationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.addMetaSource
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
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.resource.MedicationAdministrationDosage
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.util.localizeFhirId
import com.projectronin.interop.fhir.util.unlocalizeFhirId
import com.projectronin.interop.tenant.config.model.Tenant
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
    epicClient: EpicClient,
    private val ehrDataAuthorityClient: EHRDataAuthorityClient,
    private val identifierService: EpicIdentifierService,
) : MedicationAdministrationService, EpicFHIRService<MedicationAdministration>(epicClient) {
    override val fhirURLSearchPart: String = "UNUSED"
    override val fhirResourceType = MedicationAdministration::class.java

    val urlSearchPart = "/api/epic/2014/Clinical/Patient/GETMEDICATIONADMINISTRATIONHISTORY/MedicationAdministration"

    override fun getByID(
        tenant: Tenant,
        resourceFHIRId: String,
    ): MedicationAdministration {
        // Since the lookup doesn't support null responses, this is our only option
        throw UnsupportedOperationException("Epic does not support FHIR ID retrieval of MedicationAdministrations")
    }

    override fun getByIDs(
        tenant: Tenant,
        resourceFHIRIds: List<String>,
    ): Map<String, MedicationAdministration> {
        return emptyMap()
    }

    @Trace
    override fun findMedicationAdministrationsByPatient(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MedicationAdministration> {
        return listOf() // Not implemented for Epic
    }

    override fun findMedicationAdministrationsByRequest(
        tenant: Tenant,
        medicationRequest: MedicationRequest,
    ): List<MedicationAdministration> {
        val patientID =
            medicationRequest.subject?.decomposedId()?.unlocalizeFhirId(tenant.mnemonic) ?: return emptyList()
        val encounterID =
            medicationRequest.encounter?.decomposedId()?.unlocalizeFhirId(tenant.mnemonic) ?: return emptyList()
        val medicationRequestID = medicationRequest.findFhirId() ?: return emptyList()
        val patient =
            runBlocking {
                ehrDataAuthorityClient.getResourceAs<Patient>(
                    tenant.mnemonic,
                    "Patient",
                    patientID.localizeFhirId(tenant.mnemonic),
                )
            } ?: return emptyList()
        val mrn =
            identifierService.getPatientIdentifier(tenant, patient.identifier).value?.value
                ?: return emptyList()
        val encounter =
            runBlocking {
                ehrDataAuthorityClient.getResourceAs<Encounter>(
                    tenant.mnemonic,
                    "Encounter",
                    encounterID.localizeFhirId(tenant.mnemonic),
                )
            } ?: return emptyList()

        val csn =
            identifierService.getEncounterIdentifier(tenant, encounter.identifier).value?.value ?: return emptyList()
        val orderID =
            identifierService.getOrderIdentifier(tenant, medicationRequest.identifier).value?.value
                ?: return emptyList()

        val request =
            EpicMedAdminRequest(
                patientID = mrn,
                patientIDType = "Internal",
                contactID = csn,
                contactIDType = "CSN",
                orderIDs = listOf(EpicOrderID(orderID, "External")),
            )

        val response =
            runBlocking {
                val ehrResponse = epicClient.post(tenant, urlSearchPart, request)
                val medAdminResponse = ehrResponse.body<EpicMedAdmin>()
                medAdminResponse.transactionId = ehrResponse.sourceURL
                medAdminResponse
            }

        return response.orders.flatMap { epicMedOrder ->
            epicMedOrder.medicationAdministrations.mapNotNull { epicMedAdmin ->
                if (epicMedAdmin.administrationInstant.isEmpty()) {
                    null
                } else {
                    MedicationAdministration(
                        id = Id("$orderID-${Instant.parse(epicMedAdmin.administrationInstant).epochSecond}"),
                        // TODO: determine if we should do concept mapping here or in interop-fhir
                        status = Code(epicMedAdmin.action),
                        medication =
                            DynamicValue(
                                type = DynamicValueType.CODEABLE_CONCEPT,
                                value =
                                    CodeableConcept(
                                        text = FHIRString(epicMedOrder.name),
                                    ),
                            ),
                        subject = Reference(reference = FHIRString("Patient/$patientID")),
                        effective =
                            DynamicValue(
                                type = DynamicValueType.DATE_TIME,
                                value = DateTime(epicMedAdmin.administrationInstant),
                            ),
                        request = Reference(reference = FHIRString("MedicationRequest/$medicationRequestID")),
                        dosage =
                            MedicationAdministrationDosage(
                                dose =
                                    Quantity(
                                        value = Decimal(BigDecimal(epicMedAdmin.dose.value)),
                                        unit = FHIRString(epicMedAdmin.dose.unit),
                                    ),
                            ),
                    ).addMetaSource(response.transactionId) as MedicationAdministration
                }
            }
        }
    }
}

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicMedAdmin(
    val orders: List<EpicMedicationOrder> = emptyList(),
    var transactionId: String = "",
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicMedicationOrder(
    val medicationAdministrations: List<EpicMedicationAdministration> = emptyList(),
    val name: String = "",
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicMedicationAdministration(
    val administrationInstant: String,
    val action: String,
    val dose: EpicDose,
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicDose(
    val value: String,
    val unit: String,
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
    val orderIDs: List<EpicOrderID>,
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicOrderID(
    @JsonProperty("ID")
    val id: String,
    val type: String,
)
