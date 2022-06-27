package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.exception.UnsupportedDynamicValueTypeException
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.ObservationReferenceRange
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.fhir.r4.datatype.Annotation as R4Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept as R4CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Quantity as R4Quantity
import com.projectronin.interop.fhir.r4.datatype.Range as R4Range
import com.projectronin.interop.fhir.r4.datatype.Ratio as R4Ratio
import com.projectronin.interop.fhir.r4.datatype.Reference as R4Reference
import com.projectronin.interop.fhir.r4.datatype.SimpleQuantity as R4SimpleQuantity
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime as R4DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id as R4Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant as R4Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown as R4Markdown
import com.projectronin.interop.fhir.r4.resource.Observation as R4Observation

class EpicObservationTest {

    val goodR4Observation = R4Observation(
        id = R4Id("OBS1"),
        basedOn = listOf(R4Reference("PROCEDURE1")),
        status = ObservationStatus.AMENDED,
        category = listOf(R4CodeableConcept(text = "CAT1")),
        code = R4CodeableConcept(text = "CODE1"),
        subject = R4Reference("PAT1"),
        encounter = R4Reference("ENCOUNTER1"),
        effective = DynamicValue(
            type = DynamicValueType.DATE_TIME,
            value = R4DateTime("2015-02-07T13:28:17-05:00")
        ),
        issued = R4Instant("2017-01-01T00:00:00Z"),
        performer = listOf(R4Reference("PRACT1")),
        value = DynamicValue(
            type = DynamicValueType.STRING,
            value = "ValueString"
        ),
        dataAbsentReason = null, // if value is populated, data isn't absent!
        interpretation = listOf(R4CodeableConcept(text = "INTERPRETATION1")),
        method = R4CodeableConcept(text = "METHOD1"),
        specimen = R4Reference("SPECIMEN1"),
        referenceRange = listOf(ObservationReferenceRange("REFRANGE1", high = R4SimpleQuantity(value = 10.0))),
        hasMember = listOf(R4Reference("HASMEMBER1")),
        derivedFrom = listOf(R4Reference("DERIVED1")),
        note = listOf(R4Annotation("ANNOTATION1", text = R4Markdown("Some sort of note."))),
    )

    @Test
    fun `can build from object`() {
        val epicObservation = EpicObservation(goodR4Observation)
        assertEquals(goodR4Observation.id?.value, epicObservation.id)
        assertEquals(goodR4Observation.code.text, epicObservation.code.text)
        assertEquals(goodR4Observation.category[0].text, epicObservation.category[0].text)
        assertEquals(goodR4Observation.basedOn[0].id, epicObservation.basedOn[0].id)
        assertEquals(goodR4Observation.status.toString(), epicObservation.status)
        assertEquals(goodR4Observation.subject?.id, epicObservation.subject.id)
        assertEquals(goodR4Observation.encounter?.id, epicObservation.encounter?.id)
        assertEquals((goodR4Observation.effective?.value as R4DateTime).value, epicObservation.effective?.value)
        assertEquals(goodR4Observation.issued?.value, epicObservation.issued)
        assertEquals(goodR4Observation.performer[0].id, epicObservation.performer[0].id)
        assertEquals(goodR4Observation.value?.value, epicObservation.value?.value)
        assertEquals(null, epicObservation.dataAbsentReason)
        assertEquals(goodR4Observation.interpretation[0].text, epicObservation.interpretation[0].text)
        assertEquals(goodR4Observation.method?.text, epicObservation.method?.text)
        assertEquals(goodR4Observation.specimen?.id, epicObservation.specimen?.id)
        assertEquals(goodR4Observation.referenceRange[0].high?.value, epicObservation.referenceRange[0].high?.value)
        assertEquals(goodR4Observation.hasMember[0].id, epicObservation.hasMember[0].id)
        assertEquals(goodR4Observation.note[0].text.value, epicObservation.note[0].text)
        assertEquals(goodR4Observation.derivedFrom[0].id, epicObservation.derivedFrom[0].id)
    }

    @Test
    fun `data absent test`() {
        val copyR4 = goodR4Observation.copy(
            value = null,
            dataAbsentReason = R4CodeableConcept(text = "Bad Data")
        )
        assertEquals(EpicObservation(copyR4).dataAbsentReason?.text, "Bad Data")
    }

    @Test
    fun `value codeable concept test`() {
        val tempR4 = goodR4Observation.copy(
            value = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = R4CodeableConcept(text = "valueCodeableConcept")
            )
        )
        val tempEpic = EpicObservation(tempR4)
        assertEquals(
            (tempR4.value?.value as R4CodeableConcept).text,
            (tempEpic.value?.value as EpicCodeableConcept).text
        )
    }

    @Test
    fun `value range test`() {
        val tempR4 = goodR4Observation.copy(
            value = DynamicValue(
                type = DynamicValueType.RANGE,
                value = R4Range(high = R4SimpleQuantity(value = 10.0), low = R4SimpleQuantity(value = 1.0))
            )
        )
        val tempEpic = EpicObservation(tempR4)
        assertEquals(
            (tempR4.value?.value as R4Range).high?.value,
            (tempEpic.value?.value as EpicRange).high?.value
        )
    }

    @Test
    fun `value quantity test`() {
        val tempR4 = goodR4Observation.copy(
            value = DynamicValue(
                type = DynamicValueType.QUANTITY,
                value = R4SimpleQuantity(value = 10.0)
            )
        )
        val tempEpic = EpicObservation(tempR4)
        assertEquals(
            (tempR4.value?.value as R4SimpleQuantity).value,
            (tempEpic.value?.value as EpicSimpleQuantity).value
        )
    }

    @Test
    fun `value ratio test`() {
        val tempR4 = goodR4Observation.copy(
            value = DynamicValue(
                type = DynamicValueType.RATIO,
                value = R4Ratio(denominator = R4Quantity(value = 10.0), numerator = R4Quantity(value = 5.0))
            )
        )
        val tempEpic = EpicObservation(tempR4)
        assertEquals(
            (tempR4.value?.value as R4Ratio).denominator?.value,
            (tempEpic.value?.value as EpicRatio).denominator?.value
        )
        assertEquals(
            (tempR4.value?.value as R4Ratio).numerator?.value,
            (tempEpic.value?.value as EpicRatio).numerator?.value
        )
    }

    @Test
    fun `value not allowed type test`() {
        val tempR4 = goodR4Observation.copy(
            value = DynamicValue(
                type = DynamicValueType.BOOLEAN,
                value = true
            )
        )
        assertThrows<UnsupportedDynamicValueTypeException> {
            EpicObservation(tempR4).value
        }
    }

    @Test
    fun `effective not allowed type test`() {
        val tempR4 = goodR4Observation.copy(
            effective = DynamicValue(
                type = DynamicValueType.PERIOD,
                value = ""
            )
        )
        assertThrows<UnsupportedDynamicValueTypeException> {
            EpicObservation(tempR4).effective
        }
    }

    @Test
    fun `null values test`() {
        val nullR4Observation = R4Observation(
            id = R4Id("OBS1"),
            basedOn = listOf(),
            status = ObservationStatus.AMENDED,
            category = listOf(),
            code = R4CodeableConcept(text = "CODE1"),
            subject = R4Reference("PAT1"),
            encounter = null,
            effective = null,
            issued = null,
            performer = listOf(),
            value = null,
            dataAbsentReason = null, // if value is populated, data isn't absent!
            interpretation = listOf(),
            method = null,
            specimen = null,
            referenceRange = listOf(),
            hasMember = listOf(),
            derivedFrom = listOf(),
            note = listOf(),
        )
        val epicObs = EpicObservation(nullR4Observation)
        assertEquals(null, epicObs.encounter)
        assertEquals(null, epicObs.effective)
        assertEquals(null, epicObs.issued)
        assertEquals(null, epicObs.value)
        assertEquals(null, epicObs.dataAbsentReason)
        assertEquals(null, epicObs.method)
        assertEquals(null, epicObs.specimen)
    }
}
