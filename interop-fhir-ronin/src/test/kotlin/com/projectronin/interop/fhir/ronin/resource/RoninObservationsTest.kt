package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.resource.ObservationReferenceRange
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCodeableConcept
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBloodPressure
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyHeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyMassIndex
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodySurfaceArea
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyTemperature
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyWeight
import com.projectronin.interop.fhir.ronin.resource.observation.RoninHeartRate
import com.projectronin.interop.fhir.ronin.resource.observation.RoninLaboratoryResult
import com.projectronin.interop.fhir.ronin.resource.observation.RoninObservation
import com.projectronin.interop.fhir.ronin.resource.observation.RoninPulseOximetry
import com.projectronin.interop.fhir.ronin.resource.observation.RoninRespiratoryRate
import com.projectronin.interop.fhir.ronin.resource.observation.RoninStagingRelated
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninObservationsTest {
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }
    private val extension1 =
        Extension(
            url = Uri("http://example.com/extension"),
            value = DynamicValue(DynamicValueType.STRING, FHIRString("value")),
        )
    private val normalizer = mockk<Normalizer>()
    private val localizer = mockk<Localizer>()
    private val bodyHeight = mockk<RoninBodyHeight>()
    private val bodyHeightCode = Code("8302-2")

    private val bodyMassIndex = mockk<RoninBodyMassIndex>()
    private val bodySurfaceArea = mockk<RoninBodySurfaceArea>()
    private val bodyWeight = mockk<RoninBodyWeight>()
    private val bodyTemperature = mockk<RoninBodyTemperature>()
    private val bloodPressure = mockk<RoninBloodPressure>()
    private val respiratoryRate = mockk<RoninRespiratoryRate>()
    private val heartRate = mockk<RoninHeartRate>()
    private val pulseOximetry = mockk<RoninPulseOximetry>()
    private val laboratoryResult = mockk<RoninLaboratoryResult>()
    private val stagingRelated = mockk<RoninStagingRelated>()
    private val default =
        mockk<RoninObservation> {
            every { qualifies(any()) } returns true
        }
    private val roninObservations =
        RoninObservations(
            normalizer,
            localizer,
            bodyHeight,
            bodyMassIndex,
            bodySurfaceArea,
            bodyWeight,
            bodyTemperature,
            bloodPressure,
            respiratoryRate,
            heartRate,
            pulseOximetry,
            laboratoryResult,
            stagingRelated,
            default,
        )

    // Observation vital signs vs. NOT vital signs - Observation vital signs start here

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninObservations.qualifies(
                Observation(
                    status = ObservationStatus.AMENDED.asCode(),
                    code =
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        code = bodyHeightCode,
                                    ),
                                ),
                        ),
                ),
            ),
        )
    }

    @Test
    fun `can validate against a profile`() {
        val observation = mockk<Observation>()

        every { bodyHeight.qualifies(observation) } returns true
        every { bodyHeight.validate(observation, LocationContext(Observation::class)) } returns Validation()
        every { bodyMassIndex.qualifies(observation) } returns false
        every { bodySurfaceArea.qualifies(observation) } returns false
        every { bodyWeight.qualifies(observation) } returns false
        every { bodyTemperature.qualifies(observation) } returns false
        every { bloodPressure.qualifies(observation) } returns false
        every { respiratoryRate.qualifies(observation) } returns false
        every { heartRate.qualifies(observation) } returns false
        every { pulseOximetry.qualifies(observation) } returns false
        every { laboratoryResult.qualifies(observation) } returns false
        every { stagingRelated.qualifies(observation) } returns false

        roninObservations.validate(observation).alertIfErrors()
    }

    @Test
    fun `can transform to profile`() {
        val original =
            mockk<Observation> {
                every { id } returns Id("1234")
            }
        every { normalizer.normalize(original, tenant) } returns original

        val mappedObservation =
            mockk<Observation> {
                every { id } returns Id("1234")
                every { extension } returns listOf(extension1)
            }

        val roninObservation =
            mockk<Observation> {
                every { id } returns Id("test-1234")
                every { extension } returns listOf(extension1)
            }
        every { localizer.localize(roninObservation, tenant) } returns roninObservation

        every { bodyHeight.qualifies(original) } returns false
        every { bodyWeight.qualifies(original) } returns true
        every { bodyWeight.conceptMap(original, LocationContext(Observation::class), tenant) } returns
            Pair(
                mappedObservation,
                Validation(),
            )

        every { bodyMassIndex.qualifies(original) } returns false
        every { bodySurfaceArea.qualifies(original) } returns false
        every { bodyTemperature.qualifies(original) } returns false
        every { bloodPressure.qualifies(original) } returns false
        every { respiratoryRate.qualifies(original) } returns false
        every { heartRate.qualifies(original) } returns false
        every { pulseOximetry.qualifies(original) } returns false
        every { laboratoryResult.qualifies(original) } returns false
        every { stagingRelated.qualifies(original) } returns false

        every { bodyHeight.qualifies(mappedObservation) } returns false
        every { bodyWeight.qualifies(mappedObservation) } returns true
        every {
            bodyWeight.transformInternal(
                mappedObservation,
                LocationContext(Observation::class),
                tenant,
            )
        } returns
            Pair(
                TransformResponse(roninObservation),
                Validation(),
            )

        every { bodyMassIndex.qualifies(mappedObservation) } returns false
        every { bodySurfaceArea.qualifies(mappedObservation) } returns false
        every { bodyTemperature.qualifies(mappedObservation) } returns false
        every { bloodPressure.qualifies(mappedObservation) } returns false
        every { respiratoryRate.qualifies(mappedObservation) } returns false
        every { heartRate.qualifies(mappedObservation) } returns false
        every { pulseOximetry.qualifies(mappedObservation) } returns false
        every { laboratoryResult.qualifies(mappedObservation) } returns false
        every { stagingRelated.qualifies(mappedObservation) } returns false

        every { bodyMassIndex.qualifies(roninObservation) } returns false
        every { bodySurfaceArea.qualifies(roninObservation) } returns false
        every { bodyHeight.qualifies(roninObservation) } returns false
        every { bodyWeight.qualifies(roninObservation) } returns true
        every { bodyWeight.validate(roninObservation, LocationContext(Observation::class)) } returns Validation()
        every { bodyTemperature.qualifies(roninObservation) } returns false
        every { bloodPressure.qualifies(roninObservation) } returns false
        every { respiratoryRate.qualifies(roninObservation) } returns false
        every { heartRate.qualifies(roninObservation) } returns false
        every { pulseOximetry.qualifies(roninObservation) } returns false
        every { laboratoryResult.qualifies(roninObservation) } returns false
        every { stagingRelated.qualifies(roninObservation) } returns false

        val (transformResponse, validation) = roninObservations.transform(original, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(roninObservation, transformed)
    }

    /**
     * the following tests that IF we recieve a profile that qualifies for more than one resource the
     * other qualifying profile Canonical is added to the meta.  There is also a check to NOT duplicate the
     * identifiers
     * */
    @Test
    fun `transforms observation with only required attributes - multiple profiles`() {
        val categoryCodings =
            listOf(
                Coding(
                    system = CodeSystem.OBSERVATION_CATEGORY.uri,
                    code = Code("laboratory"),
                ),
                Coding(
                    system = CodeSystem.OBSERVATION_CATEGORY.uri,
                    code = Code("vital-signs"),
                ),
            )
        val laboratoryCodeCodingList =
            listOf(
                Coding(
                    code = Code("2708-6"),
                    display = "Oxygen saturation in Arterial blood".asFHIR(),
                    system = CodeSystem.LOINC.uri,
                ),
            )
        val conceptMapMetadata =
            ConceptMapMetadata(
                registryEntryType = "concept-map",
                conceptMapName = "test-concept-map",
                conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
                version = "1",
            )
        val valueSetMetadata =
            ValueSetMetadata(
                registryEntryType = "value_set",
                valueSetName = "test-value-set",
                valueSetUuid = "03d51d53-1a31-49a9-af74-573b456efca5",
                version = "2",
            )
        val tenantLaboratoryResultConcept =
            CodeableConcept(
                text = "Tenant Laboratory Result".asFHIR(),
                coding =
                    listOf(
                        Coding(
                            code = Code("2708-6"),
                            display = "Laboratory Result".asFHIR(),
                            system = CodeSystem.LOINC.uri,
                        ),
                    ),
            )
        val tenantLaboratoryResultSourceExtension =
            Extension(
                url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
                value =
                    DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        tenantLaboratoryResultConcept,
                    ),
            )
        val mappedTenantLaboratoryResultConcept =
            CodeableConcept(
                text = "Laboratory Result".asFHIR(),
                coding =
                    listOf(
                        Coding(
                            code = Code("2708-6"),
                            display = "Laboratory Result".asFHIR(),
                            system = CodeSystem.LOINC.uri,
                        ),
                    ),
            )
        val laboratoryCodeConcept =
            CodeableConcept(
                text = "Laboratory Result".asFHIR(),
                coding =
                    listOf(
                        Coding(
                            code = Code("2708-6"),
                            display = "Laboratory Result".asFHIR(),
                            system = CodeSystem.LOINC.uri,
                        ),
                    ),
            )
        val tenantComponentCode = CodeableConcept(text = "code2".asFHIR())
        val componentCode = CodeableConcept(text = "Real Code 2".asFHIR())
        val tenantComponentSourceCodeExtension =
            Extension(
                url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.value),
                value =
                    DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        tenantComponentCode,
                    ),
            )
        val concentrationCoding =
            listOf(
                Coding(system = CodeSystem.LOINC.uri, code = Code("3150-0")),
            )
        val concentrationCodeableConcept =
            CodeableConcept(
                coding = concentrationCoding,
                text = "Concentration".asFHIR(),
            )
        val concentrationSourceExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                value =
                    DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        concentrationCodeableConcept,
                    ),
            )
        val flowRateCoding =
            listOf(
                Coding(system = CodeSystem.LOINC.uri, code = Code("3151-8")),
            )
        val flowRateCodeableConcept =
            CodeableConcept(
                coding = flowRateCoding,
                text = "Flow Rate".asFHIR(),
            )
        val flowRateSourceExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                value =
                    DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        flowRateCodeableConcept,
                    ),
            )
        val pulseOximetryCode = Code("2708-6")
        val pulseOximetryCoding =
            Coding(
                system = CodeSystem.LOINC.uri,
                display = "Pulse Oximetry".asFHIR(),
                code = pulseOximetryCode,
            )
        val pulseOximetryCodingList = listOf(pulseOximetryCoding)
        val pulseOximetryConcept =
            CodeableConcept(
                text = "Pulse Oximetry".asFHIR(),
                coding = pulseOximetryCodingList,
            )
        val tenantPulseOximetryCoding =
            Coding(
                system = CodeSystem.LOINC.uri,
                display = "Pulse Oximetry".asFHIR(),
                code = Code("bad-body-height"),
            )
        val tenantPulseOximetryConcept =
            CodeableConcept(
                text = "Tenant Pulse Oximetry".asFHIR(),
                coding = listOf(tenantPulseOximetryCoding),
            )
        val tenantPulseOximetrySourceExtension =
            Extension(
                url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
                value =
                    DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        tenantPulseOximetryConcept,
                    ),
            )
        val registryClientLab =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_LABORATORY_RESULT.value)
                } returns ValueSetList(laboratoryCodeCodingList, valueSetMetadata)
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        laboratoryCodeConcept,
                        any<Observation>(),
                    )
                } returns null
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        tenantLaboratoryResultConcept,
                        any<Observation>(),
                    )
                } returns
                    ConceptMapCodeableConcept(
                        laboratoryCodeConcept,
                        tenantLaboratoryResultSourceExtension,
                        listOf(conceptMapMetadata),
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.component.code",
                        tenantComponentCode,
                        any<Observation>(),
                    )
                } returns
                    ConceptMapCodeableConcept(
                        componentCode,
                        tenantComponentSourceCodeExtension,
                        listOf(conceptMapMetadata),
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "laboratory".asFHIR(),
                            coding = listOf(),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }
        val registryClientPulseOx =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)
                } returns ValueSetList(pulseOximetryCodingList, valueSetMetadata)
                every {
                    getRequiredValueSet(
                        "Observation.component:FlowRate.code",
                        RoninProfile.OBSERVATION_PULSE_OXIMETRY.value,
                    )
                } returns ValueSetList(flowRateCoding, valueSetMetadata)
                every {
                    getRequiredValueSet(
                        "Observation.component:Concentration.code",
                        RoninProfile.OBSERVATION_PULSE_OXIMETRY.value,
                    )
                } returns ValueSetList(concentrationCoding, valueSetMetadata)
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        pulseOximetryConcept,
                        any<Observation>(),
                    )
                } returns null
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        tenantPulseOximetryConcept,
                        any<Observation>(),
                    )
                } returns
                    ConceptMapCodeableConcept(
                        pulseOximetryConcept,
                        tenantPulseOximetrySourceExtension,
                        listOf(conceptMapMetadata),
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.component.code",
                        flowRateCodeableConcept,
                        any<Observation>(),
                    )
                } returns
                    ConceptMapCodeableConcept(
                        flowRateCodeableConcept,
                        flowRateSourceExtension,
                        listOf(conceptMapMetadata),
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.component.code",
                        concentrationCodeableConcept,
                        any<Observation>(),
                    )
                } returns
                    ConceptMapCodeableConcept(
                        concentrationCodeableConcept,
                        concentrationSourceExtension,
                        listOf(conceptMapMetadata),
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("faulty"),
                                        code = Code("59408-5"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        code = Code("8867-4"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        code = Code("13245"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }
        val registryClientBodyHeight =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_HEIGHT.value)
                } returns
                    ValueSetList(
                        listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Body Height".asFHIR(),
                                code = bodyHeightCode,
                            ),
                        ),
                        valueSetMetadata,
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Body Height".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Body Height".asFHIR(),
                                        code = bodyHeightCode,
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Tenant Body Height".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Body Height".asFHIR(),
                                        code = Code("bad-body-height"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }
        val registryClientBodyMassIndex =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_MASS_INDEX.value)
                } returns
                    ValueSetList(
                        listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Body Mass Index".asFHIR(),
                                code = Code("39156-5"),
                            ),
                        ),
                        valueSetMetadata,
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Body Mass Index".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Body Mass Index".asFHIR(),
                                        code = Code("39156-5"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Tenant Body Mass Index".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Tenant Body Mass Index".asFHIR(),
                                        code = Code("bad-body-mass-index"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns
                    ConceptMapCodeableConcept(
                        CodeableConcept(
                            text = "Body Mass Index".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Body Mass Index".asFHIR(),
                                        code = Code("39156-5"),
                                    ),
                                ),
                        ),
                        Extension(
                            url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(
                                        text = "Body Mass Index".asFHIR(),
                                        coding =
                                            listOf(
                                                Coding(
                                                    system = CodeSystem.LOINC.uri,
                                                    display = "Body Mass Index".asFHIR(),
                                                    code = Code("39156-5"),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                        listOf(conceptMapMetadata),
                    )
            }
        val registryClientBSA =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_SURFACE_AREA.value)
                } returns
                    ValueSetList(
                        listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Body Surface Area".asFHIR(),
                                code = Code("3140-1"),
                            ),
                        ),
                        valueSetMetadata,
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Body Surface Area".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Body Surface Area".asFHIR(),
                                        code = Code("3140-1"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }
        val registryClientBodyTemp =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_TEMPERATURE.value)
                } returns
                    ValueSetList(
                        listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Body Temp".asFHIR(),
                                code = Code("8310-5"),
                            ),
                        ),
                        valueSetMetadata,
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Body Temp".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Body Temp".asFHIR(),
                                        code = Code("8310-5"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }

        val registryClientBodyWeight =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_WEIGHT.value)
                } returns
                    ValueSetList(
                        listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Body Weight".asFHIR(),
                                code = Code("29463-7"),
                            ),
                        ),
                        valueSetMetadata,
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Body Weight".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Body Weight".asFHIR(),
                                        code = Code("29463-7"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }
        val registryClientBP =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)
                } returns
                    ValueSetList(
                        listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Blood Pressure".asFHIR(),
                                code = Code("85354-9"),
                            ),
                        ),
                        valueSetMetadata,
                    )
                every {
                    getRequiredValueSet(
                        "Observation.component:systolic.code",
                        RoninProfile.OBSERVATION_BLOOD_PRESSURE.value,
                    )
                } returns
                    ValueSetList(
                        listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8480-6"))),
                        valueSetMetadata,
                    )
                every {
                    getRequiredValueSet(
                        "Observation.component:diastolic.code",
                        RoninProfile.OBSERVATION_BLOOD_PRESSURE.value,
                    )
                } returns
                    ValueSetList(
                        listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8462-4"))),
                        valueSetMetadata,
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Blood Pressure".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Blood Pressure".asFHIR(),
                                        code = Code("85354-9"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }
        val registryClientRR =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_RESPIRATORY_RATE.value)
                } returns
                    ValueSetList(
                        listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Respiratory Rate".asFHIR(),
                                code = Code("9279-1"),
                            ),
                        ),
                        valueSetMetadata,
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Respiratory Rate".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Respiratory Rate".asFHIR(),
                                        code = Code("9279-1"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }
        val registryClientHR =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_HEART_RATE.value)
                } returns
                    ValueSetList(
                        listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Heart Rate".asFHIR(),
                                code = Code("8867-4"),
                            ),
                        ),
                        valueSetMetadata,
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Heart Rate".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.LOINC.uri,
                                        display = "Heart Rate".asFHIR(),
                                        code = Code("8867-4"),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }
        val registryClientStagingRelated =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_STAGING_RELATED.value)
                } returns
                    ValueSetList(
                        listOf(
                            Coding(
                                system = Uri("some-system-uri"),
                                code = Code("some-code"),
                                display = "some-display".asFHIR(),
                            ),
                        ),
                        valueSetMetadata,
                    )
                every {
                    getConceptMapping(
                        tenant,
                        "Observation.code",
                        CodeableConcept(
                            text = "Staging Related".asFHIR(),
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("some-system-uri"),
                                        code = Code("some-code"),
                                        display = "some-display".asFHIR(),
                                    ),
                                ),
                        ),
                        any<Observation>(),
                    )
                } returns null
            }

        val multipleCategoryConceptList = CodeableConcept(coding = categoryCodings)
        val roninObservation =
            RoninObservations(
                normalizer, localizer,
                RoninBodyHeight(normalizer, localizer, registryClientBodyHeight),
                RoninBodyMassIndex(normalizer, localizer, registryClientBodyMassIndex),
                RoninBodySurfaceArea(normalizer, localizer, registryClientBSA),
                RoninBodyWeight(normalizer, localizer, registryClientBodyWeight),
                RoninBodyTemperature(normalizer, localizer, registryClientBodyTemp),
                RoninBloodPressure(normalizer, localizer, registryClientBP),
                RoninRespiratoryRate(normalizer, localizer, registryClientRR),
                RoninHeartRate(normalizer, localizer, registryClientHR),
                RoninPulseOximetry(normalizer, localizer, registryClientPulseOx),
                RoninLaboratoryResult(normalizer, localizer, registryClientLab),
                RoninStagingRelated(normalizer, localizer, registryClientStagingRelated),
                RoninObservation(normalizer, localizer, registryClientLab),
            )

        val observation =
            Observation(
                id = Id("123"),
                meta = Meta(source = Uri("source")),
                status = Code("amended"),
                code =
                    CodeableConcept(
                        text = "Tenant Laboratory Result".asFHIR(),
                        coding =
                            listOf(
                                Coding(
                                    code = Code("2708-6"),
                                    system = CodeSystem.LOINC.uri,
                                    display = "Laboratory Result".asFHIR(),
                                ),
                            ),
                    ),
                category = listOf(multipleCategoryConceptList),
                dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
                subject =
                    Reference(
                        display = "subject".asFHIR(),
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        "2022-01-01T00:00:00Z",
                    ),
            )

        every { pulseOximetry.qualifies(observation) } returns true
        every { laboratoryResult.qualifies(observation) } returns true
        every { normalizer.normalize(observation, tenant) } returns observation
        every { localizer.localize(any(), tenant) } answers { firstArg() }

        val (transformResponse, validation) = roninObservation.transform(observation, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Observation", transformed.resourceType)
        assertEquals(
            listOf(tenantLaboratoryResultSourceExtension),
            transformed.extension,
        )
        assertEquals(Id("123"), transformed.id)
        assertEquals(
            Meta(
                profile =
                    listOf(
                        Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value),
                        Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value),
                    ),
                source = Uri("source"),
            ),
            transformed.meta,
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR(),
                ),
            ),
            transformed.identifier,
        )
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(listOf<Reference>(), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            listOf(multipleCategoryConceptList),
            transformed.category,
        )
        assertEquals(
            mappedTenantLaboratoryResultConcept,
            transformed.code,
        )
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension),
                display = "subject".asFHIR(),
            ),
            transformed.subject,
        )
        assertEquals(listOf<Reference>(), transformed.focus)
        Assertions.assertNull(transformed.encounter)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z",
            ),
            transformed.effective,
        )
        Assertions.assertNull(transformed.issued)
        assertEquals(listOf<Reference>(), transformed.performer)
        Assertions.assertNull(transformed.value)
        assertEquals(CodeableConcept(text = "dataAbsent".asFHIR()), transformed.dataAbsentReason)
        assertEquals(listOf<CodeableConcept>(), transformed.interpretation)
        Assertions.assertNull(transformed.bodySite)
        Assertions.assertNull(transformed.method)
        Assertions.assertNull(transformed.specimen)
        Assertions.assertNull(transformed.device)
        assertEquals(listOf<ObservationReferenceRange>(), transformed.referenceRange)
        assertEquals(listOf<Reference>(), transformed.hasMember)
        assertEquals(listOf<Reference>(), transformed.derivedFrom)
        assertEquals(listOf<ObservationComponent>(), transformed.component)
        assertEquals(listOf<Annotation>(), transformed.note)
    }
}
