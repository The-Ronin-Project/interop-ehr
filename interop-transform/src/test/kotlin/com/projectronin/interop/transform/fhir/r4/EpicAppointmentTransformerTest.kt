package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.ExtensionMeanings
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicAppointmentTransformerTest {
    private val transformer = EpicAppointmentTransformer()

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val epicAppointmentSearchJson = this::class.java.getResource("/ExampleEpicAppointment.json")!!.readText()

    @Test
    fun `transforms appointment with all attributes`() {
        val appointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentNotes" : [ "Notes" ],
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "scheduled",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "ExtraExtensions" : [ {
                "ExtensionIds" : [ {
                  "ID" : "abc",
                  "Type" : "type"
                } ],
                "ExtensionName" : "extension name",
                "Lines" : [ {
                  "LineNumber" : 1,
                  "SubLines" : [ {
                    "SubLineNumber" : 2,
                    "Value" : "subline value"
                  } ],
                  "Value" : "line value"
                } ],
                "Value" : "extension value"
              } ],
              "ExtraItems" : [ {
                "ItemNumber" : "number",
                "Lines" : [ {
                  "LineNumber" : 1,
                  "SubLines" : [ {
                    "SubLineNumber" : 2,
                    "Value" : "subline value"
                  } ],
                  "Value" : "line value"
                } ],
                "Value" : "item value"
              } ],
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderIDs" : [ {
                  "ID" : "9876",
                  "Type" : "Internal"
                } ],
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns appointmentJson
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)

        oncologyAppointment!!
        assertEquals(Id(value = "test-12345"), oncologyAppointment.id)
        assertEquals(
            listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Organization/test-6789"))
                )
            ),
            oncologyAppointment.extension
        )
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_ID, system = CodeSystem.RONIN_ID.uri, value = "test-12345"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.PENDING, oncologyAppointment.status)
        assertEquals(
            CodeableConcept(text = "Test visit type"),
            oncologyAppointment.appointmentType
        )
        assertEquals(Instant("2015-04-30T20:30:00Z"), oncologyAppointment.start)
        assertEquals(Instant("2015-04-30T21:00:00Z"), oncologyAppointment.end)
        assertEquals(30, oncologyAppointment.minutesDuration)
        assertEquals("Notes", oncologyAppointment.comment)
        assertEquals(
            listOf(
                Participant(
                    actor = listOf(
                        Reference(
                            reference = "Patient/test-54321",
                            display = "Test Name"
                        ),
                    ),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor = listOf(
                        Reference(
                            reference = "Practitioner/test-9876",
                            display = "Test Doc"
                        ),
                    ),
                    status = ParticipationStatus.ACCEPTED,
                    period = Period(
                        start = DateTime(value = "2015-04-30T20:30:00Z"),
                        end = DateTime(value = "2015-04-30T20:45:00Z")
                    )
                )
            ),
            oncologyAppointment.participant
        )
    }

    @Test
    fun `transform appointment with only required attributes`() {
        val appointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "completed",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns appointmentJson
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)
        oncologyAppointment!!

        assertEquals("Appointment", oncologyAppointment.resourceType)
        assertEquals(Id(value = "test-12345"), oncologyAppointment.id)
        assertEquals(
            listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Organization/test-6789"))
                )
            ),
            oncologyAppointment.extension
        )
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_ID, system = CodeSystem.RONIN_ID.uri, value = "test-12345"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.FULFILLED, oncologyAppointment.status)
        assertEquals(
            CodeableConcept(text = "Test visit type"),
            oncologyAppointment.appointmentType
        )
        assertEquals(Instant("2015-04-30T20:30:00Z"), oncologyAppointment.start)
        assertEquals(Instant("2015-04-30T21:00:00Z"), oncologyAppointment.end)
        assertEquals(30, oncologyAppointment.minutesDuration)
        assertNull(oncologyAppointment.comment)
        assertEquals(
            listOf(
                Participant(
                    actor = listOf(
                        Reference(
                            reference = "Patient/test-54321",
                            display = "Test Name"
                        ),
                    ),
                    status = ParticipationStatus.ACCEPTED
                )
            ),
            oncologyAppointment.participant
        )
        assertNull(oncologyAppointment.meta)
        assertNull(oncologyAppointment.implicitRules)
        assertNull(oncologyAppointment.language)
        assertNull(oncologyAppointment.text)
        assertEquals(listOf<ContainedResource>(), oncologyAppointment.contained)
        assertEquals(listOf<Extension>(), oncologyAppointment.modifierExtension)
        assertNull(oncologyAppointment.cancellationReason)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.serviceCategory)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.serviceType)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.specialty)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.reasonCode)
        assertEquals(listOf<Reference>(), oncologyAppointment.reasonReference)
        assertNull(oncologyAppointment.priority)
        assertNull(oncologyAppointment.description)
        assertEquals(listOf<Reference>(), oncologyAppointment.supportingInformation)
        assertEquals(listOf<Reference>(), oncologyAppointment.slot)
        assertNull(oncologyAppointment.created)
        assertNull(oncologyAppointment.patientInstruction)
        assertEquals(listOf<Reference>(), oncologyAppointment.basedOn)
        assertEquals(listOf<Period>(), oncologyAppointment.requestedPeriod)
    }

    @Test
    fun `transforms real AppOrchard data`() {
        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns epicAppointmentSearchJson
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)

        oncologyAppointment!!
        assertEquals(Id(value = "test-22792"), oncologyAppointment.id)
        assertEquals(
            listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                    value = DynamicValue(
                        DynamicValueType.REFERENCE,
                        Reference(reference = "Organization/test-10501205")
                    )
                )
            ),
            oncologyAppointment.extension
        )
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_ID, system = CodeSystem.RONIN_ID.uri, value = "test-22792"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.NOSHOW, oncologyAppointment.status)
        assertEquals(
            CodeableConcept(text = "TRANSPLANT EVALUATION"),
            oncologyAppointment.appointmentType
        )
        assertEquals(Instant("2015-04-30T20:30:00Z"), oncologyAppointment.start)
        assertEquals(Instant("2015-04-30T21:00:00Z"), oncologyAppointment.end)
        assertEquals(30, oncologyAppointment.minutesDuration)
        assertNull(oncologyAppointment.comment)
        assertEquals(
            listOf(
                Participant(
                    actor = listOf(
                        Reference(
                            reference = "Patient/test-Z5660",
                            display = "LMRTESTING,HERMIONE"
                        ),
                    ),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor = listOf(
                        Reference(
                            reference = "Practitioner/test-E400019",
                            display = "Coordinator Phoenix, RN"
                        ),
                    ),
                    status = ParticipationStatus.ACCEPTED,
                    period = Period(
                        start = DateTime(value = "2015-04-30T20:30:00Z"),
                        end = DateTime(value = "2015-04-30T20:45:00Z")
                    )
                )
            ),
            oncologyAppointment.participant
        )
    }

    @Test
    fun `transform appointments with all appointment statuses`() {
        val completedAppointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "completed",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val completedAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns completedAppointmentJson
        }

        val completedOncologyAppointment = transformer.transformAppointment(completedAppointment, tenant)!!
        assertEquals(AppointmentStatus.FULFILLED, completedOncologyAppointment.status)

        val scheduledAppointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "scheduled",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val scheduledAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns scheduledAppointmentJson
        }

        val scheduledOncologyAppointment = transformer.transformAppointment(scheduledAppointment, tenant)!!
        assertEquals(AppointmentStatus.PENDING, scheduledOncologyAppointment.status)

        val noShowAppointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "no show",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val noShowAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns noShowAppointmentJson
        }

        val noShowOncologyAppointment = transformer.transformAppointment(noShowAppointment, tenant)!!
        assertEquals(AppointmentStatus.NOSHOW, noShowOncologyAppointment.status)

        val arrivedAppointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "arrived",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val arrivedAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns arrivedAppointmentJson
        }

        val arrivedOncologyAppointment = transformer.transformAppointment(arrivedAppointment, tenant)!!
        assertEquals(AppointmentStatus.ARRIVED, arrivedOncologyAppointment.status)

        val unknownAppointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "unknown",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val unknownAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns unknownAppointmentJson
        }

        val unknownOncologyAppointment = transformer.transformAppointment(unknownAppointment, tenant)!!
        assertEquals(AppointmentStatus.ENTERED_IN_ERROR, unknownOncologyAppointment.status)
    }

    @Test
    fun `non AppOrchard appointment`() {
        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformAppointment(appointment, tenant)
        }

        assertEquals("Appointment is not an Epic AppOrchard resource", exception.message)
    }

    @Test
    fun `fails for appointment with missing id`() {
        val appointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "No Show",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "not ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderIDs" : [ {
                  "ID" : "9876",
                  "Type" : "Internal"
                } ],
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on missing partnerReference extension`() {
        val appointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "No Show",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Bad Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "BAD-Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderIDs" : [ {
                  "ID" : "9876",
                  "Type" : "Internal"
                } ],
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on no participants`() {
        val appointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "No Show",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "BAD-Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderIDs" : [ {
                  "ID" : "9876",
                  "Type" : "BAD-Internal"
                } ],
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.FHIR_R4
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformAppointments(bundle, tenant)
        }

        assertEquals("Bundle is not an Epic AppOrchard resource", exception.message)
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns "{}"
        }

        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns "{}"
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(0, oncologyAppointments.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns "{}"
        }

        val appointment2Json = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "No Show",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()
        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns appointment2Json
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(1, oncologyAppointments.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val appointmentJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "No Show",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "15",
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()

        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns appointmentJson
        }

        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { raw } returns appointmentJson
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(2, oncologyAppointments.size)
    }
}
