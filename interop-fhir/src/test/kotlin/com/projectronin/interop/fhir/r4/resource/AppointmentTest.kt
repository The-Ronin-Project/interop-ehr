package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
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
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AppointmentTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner")),
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED,
                div = "div"
            ),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id")),
            status = AppointmentStatus.CANCELLED,
            cancellationReason = CodeableConcept(text = "cancel reason"),
            serviceCategory = listOf(CodeableConcept(text = "service category")),
            serviceType = listOf(CodeableConcept(text = "service type")),
            specialty = listOf(CodeableConcept(text = "specialty")),
            appointmentType = CodeableConcept(text = "appointment type"),
            reasonCode = listOf(CodeableConcept(text = "reason code")),
            reasonReference = listOf(Reference(display = "reason reference")),
            priority = 1,
            description = "appointment test",
            supportingInformation = listOf(Reference(display = "supporting info")),
            start = Instant(value = "2017-01-01T00:00:00Z"),
            end = Instant(value = "2017-01-01T01:00:00Z"),
            minutesDuration = 15,
            slot = listOf(Reference(display = "slot")),
            created = DateTime(value = "2021-11-16"),
            comment = "comment",
            patientInstruction = "patient instruction",
            basedOn = listOf(Reference(display = "based on")),
            participant = listOf(
                Participant(
                    actor = listOf(Reference(display = "actor")),
                    status = ParticipationStatus.ACCEPTED
                )
            ),
            requestedPeriod = listOf(Period(start = DateTime(value = "2021-11-16")))
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appointment)

        val expectedJson = """
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
                "url" : "http://localhost/extension",
                "valueString" : "Value"
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
        assertEquals(expectedJson, json)

        val deserializedAppointment = JacksonManager.objectMapper.readValue<Appointment>(json)
        assertEquals(appointment, deserializedAppointment)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val appointment = Appointment(
            status = AppointmentStatus.CANCELLED,
            participant = listOf(
                Participant(
                    actor = listOf(Reference(display = "actor")),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appointment)

        val expectedJson = """
            {
              "resourceType" : "Appointment",
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "resourceType" : "Appointment",
              "status" : "cancelled",
              "participant" : [ {
                "actor" : [ {
                  "display" : "actor"
                } ],
                "status" : "accepted"
              } ]
            }
        """.trimIndent()
        val appointment = JacksonManager.objectMapper.readValue<Appointment>(json)

        assertNull(appointment.id)
        assertNull(appointment.meta)
        assertNull(appointment.implicitRules)
        assertNull(appointment.language)
        assertNull(appointment.text)
        assertEquals(listOf<ContainedResource>(), appointment.contained)
        assertEquals(listOf<Extension>(), appointment.extension)
        assertEquals(listOf<Extension>(), appointment.modifierExtension)
        assertEquals(listOf<Identifier>(), appointment.identifier)
        assertNull(appointment.cancellationReason)
        assertEquals(listOf<CodeableConcept>(), appointment.serviceCategory)
        assertEquals(listOf<CodeableConcept>(), appointment.serviceType)
        assertEquals(listOf<CodeableConcept>(), appointment.specialty)
        assertNull(appointment.appointmentType)
        assertEquals(listOf<CodeableConcept>(), appointment.reasonCode)
        assertEquals(listOf<Reference>(), appointment.reasonReference)
        assertNull(appointment.priority)
        assertNull(appointment.description)
        assertEquals(listOf<Reference>(), appointment.supportingInformation)
        assertNull(appointment.start)
        assertNull(appointment.end)
        assertNull(appointment.minutesDuration)
        assertEquals(listOf<Reference>(), appointment.slot)
        assertNull(appointment.created)
        assertNull(appointment.comment)
        assertNull(appointment.patientInstruction)
        assertEquals(listOf<Reference>(), appointment.basedOn)
        assertEquals(listOf<Period>(), appointment.requestedPeriod)
    }

    @Test
    fun `fails if minutesDuration is not positive`() {
        val exception = assertThrows<IllegalArgumentException> {
            Appointment(
                minutesDuration = 0,
                status = AppointmentStatus.CANCELLED,
                participant = listOf(
                    Participant(
                        actor = listOf(Reference(display = "actor")),
                        status = ParticipationStatus.ACCEPTED
                    )
                )
            )
        }
        assertEquals("Appointment duration must be positive", exception.message)
    }

    @Test
    fun `fails if priority is negative`() {
        val exception = assertThrows<IllegalArgumentException> {
            Appointment(
                priority = -1,
                status = AppointmentStatus.CANCELLED,
                participant = listOf(
                    Participant(
                        actor = listOf(Reference(display = "actor")),
                        status = ParticipationStatus.ACCEPTED
                    )
                )
            )
        }
        assertEquals("Priority cannot be negative", exception.message)
    }

    @Test
    fun `fails if no participant`() {
        val exception = assertThrows<IllegalArgumentException> {
            Appointment(
                status = AppointmentStatus.CANCELLED,
                participant = listOf()
            )
        }
        assertEquals("At least one participant must be provided", exception.message)
    }

    @Test
    fun `fails if participant doesn't have type or actor`() {
        val exception = assertThrows<IllegalArgumentException> {
            Appointment(
                status = AppointmentStatus.CANCELLED,
                participant = listOf(Participant(status = ParticipationStatus.ACCEPTED))
            )
        }
        assertEquals(
            "[app-1](https://www.hl7.org/fhir/R4/appointment.html#invs): Either the type or actor on the participant SHALL be specified",
            exception.message
        )
    }

    @Test
    fun `fails if appointment has start without end`() {
        val exception = assertThrows<IllegalArgumentException> {
            Appointment(
                status = AppointmentStatus.CANCELLED,
                participant = listOf(
                    Participant(
                        actor = listOf(Reference(display = "actor")),
                        status = ParticipationStatus.ACCEPTED
                    )
                ),
                start = Instant(value = "2017-01-01T00:00:00Z")
            )
        }
        assertEquals(
            "[app-2](https://www.hl7.org/fhir/R4/appointment.html#invs): Either start and end are specified, or neither",
            exception.message
        )
    }

    @Test
    fun `fails if no start or end and status is not cancelled or proposed`() {
        val exception = assertThrows<IllegalArgumentException> {
            Appointment(
                status = AppointmentStatus.BOOKED,
                participant = listOf(
                    Participant(
                        actor = listOf(Reference(display = "actor")),
                        status = ParticipationStatus.ACCEPTED
                    )
                )
            )
        }
        assertEquals(
            "[app-3](https://www.hl7.org/fhir/R4/appointment.html#invs): Only proposed or cancelled appointments can be missing start/end dates",
            exception.message
        )
    }

    @Test
    fun `fails if cancellationReason is sent and status is not cancelled or noshow`() {
        val exception = assertThrows<IllegalArgumentException> {
            Appointment(
                status = AppointmentStatus.PROPOSED,
                participant = listOf(
                    Participant(
                        actor = listOf(Reference(display = "actor")),
                        status = ParticipationStatus.ACCEPTED
                    )
                ),
                cancellationReason = CodeableConcept(text = "cancel reason")
            )
        }
        assertEquals(
            "[app-4](https://www.hl7.org/fhir/R4/appointment.html#invs): Cancellation reason is only used for appointments that have been cancelled, or no-show",
            exception.message
        )
    }
}
