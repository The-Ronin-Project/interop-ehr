package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class R4AppointmentTransformerTest {
    private val transformer = R4AppointmentTransformer()

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `transforms appointment with all attributes`() {
        val appointmentJson = """
            {
              "resourceType" : "Appointment",
              "id" : "12345",
              "meta" : {
                "profile" : [ "http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner" ]
              },
              "implicitRules" : "implicit-rules",
              "language" : "en-US",
              "text" : {
                "status" : "generated",
                "div" : "div"
              },
              "contained" : [ {"resourceType":"Banana","id":"24680"} ],
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "modifierExtension" : [ {
                "url" : "http://localhost/modifier-extension",
                "valueString" : "Value"
              } ],
              "identifier" : [ {
                "value" : "id"
              } ],
              "status" : "cancelled",
              "cancellationReason" : {
                "text" : "cancel reason"
              },
              "serviceCategory" : [ {
                "text" : "service category"
              } ],
              "serviceType" : [ {
                "text" : "service type"
              } ],
              "specialty" : [ {
                "text" : "specialty"
              } ],
              "appointmentType" : {
                "text" : "appointment type"
              },
              "reasonCode" : [ {
                "text" : "reason code"
              } ],
              "reasonReference" : [ {
                "display" : "reason reference"
              } ],
              "priority" : 1,
              "description" : "appointment test",
              "supportingInformation" : [ {
                "display" : "supporting info"
              } ],
              "start" : "2017-01-01T00:00:00Z",
              "end" : "2017-01-01T01:00:00Z",
              "minutesDuration" : 15,
              "slot" : [ {
                "display" : "slot"
              } ],
              "created" : "2021-11-16",
              "comment" : "comment",
              "patientInstruction" : "patient instruction",
              "basedOn" : [ {
                "display" : "based on"
              } ],
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ],
              "requestedPeriod" : [ {
                "start" : "2021-11-16"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)

        oncologyAppointment!! // Force it to be treated as non-null
        assertEquals("Appointment", oncologyAppointment.resourceType)
        assertEquals(Id(value = "test-12345"), oncologyAppointment.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner"))),
            oncologyAppointment.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyAppointment.implicitRules)
        assertEquals(Code("en-US"), oncologyAppointment.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), oncologyAppointment.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            oncologyAppointment.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            oncologyAppointment.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyAppointment.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.CANCELLED, oncologyAppointment.status)
        assertEquals(CodeableConcept(text = "cancel reason"), oncologyAppointment.cancellationReason)
        assertEquals((listOf(CodeableConcept(text = "service category"))), oncologyAppointment.serviceCategory)
        assertEquals((listOf(CodeableConcept(text = "service type"))), oncologyAppointment.serviceType)
        assertEquals((listOf(CodeableConcept(text = "specialty"))), oncologyAppointment.specialty)
        assertEquals(CodeableConcept(text = "appointment type"), oncologyAppointment.appointmentType)
        assertEquals(listOf(CodeableConcept(text = "reason code")), oncologyAppointment.reasonCode)
        assertEquals(listOf(Reference(display = "reason reference")), oncologyAppointment.reasonReference)
        assertEquals(1, oncologyAppointment.priority)
        assertEquals("appointment test", oncologyAppointment.description)
        assertEquals(listOf(Reference(display = "supporting info")), oncologyAppointment.supportingInformation)
        assertEquals(Instant(value = "2017-01-01T00:00:00Z"), oncologyAppointment.start)
        assertEquals(Instant(value = "2017-01-01T01:00:00Z"), oncologyAppointment.end)
        assertEquals(15, oncologyAppointment.minutesDuration)
        assertEquals(listOf(Reference(display = "slot")), oncologyAppointment.slot)
        assertEquals(DateTime(value = "2021-11-16"), oncologyAppointment.created)
        assertEquals("patient instruction", oncologyAppointment.patientInstruction)
        assertEquals(listOf(Reference(display = "based on")), oncologyAppointment.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = listOf(Reference(display = "actor")),
                    status = ParticipationStatus.ACCEPTED
                )
            ),
            oncologyAppointment.participant
        )
        assertEquals(listOf(Period(start = DateTime(value = "2021-11-16"))), oncologyAppointment.requestedPeriod)
    }

    @Test
    fun `transform appointment with only required attributes`() {
        val appointmentJson = """
            {
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)

        oncologyAppointment!! // Force it to be treated as non-null
        assertEquals("Appointment", oncologyAppointment.resourceType)
        assertEquals(Id(value = "test-12345"), oncologyAppointment.id)
        assertNull(oncologyAppointment.meta)
        assertNull(oncologyAppointment.implicitRules)
        assertNull(oncologyAppointment.language)
        assertNull(oncologyAppointment.text)
        assertEquals(listOf<ContainedResource>(), oncologyAppointment.contained)
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            oncologyAppointment.extension
        )
        assertEquals(listOf<Extension>(), oncologyAppointment.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.CANCELLED, oncologyAppointment.status)
        assertNull(oncologyAppointment.cancellationReason)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.serviceCategory)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.serviceType)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.specialty)
        assertNull(oncologyAppointment.appointmentType)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.reasonCode)
        assertEquals(listOf<Reference>(), oncologyAppointment.reasonReference)
        assertNull(oncologyAppointment.priority)
        assertNull(oncologyAppointment.description)
        assertEquals(listOf<Reference>(), oncologyAppointment.supportingInformation)
        assertNull(oncologyAppointment.start)
        assertNull(oncologyAppointment.end)
        assertNull(oncologyAppointment.minutesDuration)
        assertEquals(listOf<Reference>(), oncologyAppointment.slot)
        assertNull(oncologyAppointment.created)
        assertNull(oncologyAppointment.patientInstruction)
        assertEquals(listOf<Reference>(), oncologyAppointment.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = listOf(Reference(display = "actor")),
                    status = ParticipationStatus.ACCEPTED
                )
            ),
            oncologyAppointment.participant
        )
        assertEquals(listOf<Period>(), oncologyAppointment.requestedPeriod)
    }

    @Test
    fun `non R4 appointment`() {
        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformAppointment(appointment, tenant)
        }

        assertEquals("Appointment is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `fails for appointment with missing id`() {
        val appointmentJson = """
            {
              "resourceType" : "Appointment",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on having end without start`() {
        val appointmentJson = """
            {
              "end" : "2017-01-01T01:00:00Z",
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on missing start and end dates without proposed or cancelled status`() {
        val appointmentJson = """
            {
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "booked",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on cancellationReason without cancelled or noshow status`() {
        val appointmentJson = """
            {
              "cancellationReason" : {
                "text" : "cancel reason"
              },
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "booked",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on missing partnerReference extension`() {
        val appointmentJson = """
            {
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ ],
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails partnerReference extension not being a Reference`() {
        val appointmentJson = """
            {
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueBoolean" : false
              } ],
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on minutesDuration not positive`() {
        val appointmentJson = """
            {
              "minutesDuration" : 0,
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on negative priority`() {
        val appointmentJson = """
            {
              "priority" : -1,
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on no participants`() {
        val appointmentJson = """
            {
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "cancelled",
              "participant" : [ ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails participant without type or actor`() {
        val appointmentJson = """
            {
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "cancelled",
              "participant" : [ {
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformAppointments(bundle, tenant)
        }

        assertEquals("Bundle is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }

        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(0, oncologyAppointments.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }

        val appointment2Json = """
            {
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()
        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointment2Json
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(1, oncologyAppointments.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val appointmentJson = """
            {
              "resourceType" : "Appointment",
              "id" : "12345",
              "extension" : [ {
                "url" : "http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference",
                "valueReference" : {
                  "reference" : "reference"
                }
              } ],
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()

        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns appointmentJson
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(2, oncologyAppointments.size)
    }
}
