package com.projectronin.interop.transform.fhir.r4.util

import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Age
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.AvailableTime
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Communication
import com.projectronin.interop.fhir.r4.datatype.Contact
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.NotAvailable
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.PatientLink
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Qualification
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Time
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import com.projectronin.interop.fhir.r4.valueset.AddressType
import com.projectronin.interop.fhir.r4.valueset.AddressUse
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.NameUse
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipantRequired
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalizersTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val nonLocalizableExtensions = listOf(
        Extension(
            url = Uri("http://localhost/extension"),
            value = DynamicValue(DynamicValueType.STRING, "Value")
        )
    )
    private val localizableExtensions = listOf(
        Extension(
            url = Uri("http://localhost/extension"),
            value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/1234"))
        )
    )
    private val localizedExtensions = listOf(
        Extension(
            url = Uri("http://localhost/extension"),
            value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/test-1234"))
        )
    )

    @Test
    fun `prepends tenant mnemonic to string`() {
        assertEquals("test-some string", "some string".localize(tenant))
    }

    @Test
    fun `prepends tenant mnemonic to id`() {
        assertEquals(Id("test-id-value"), Id("id-value").localize(tenant))
    }

    @Test
    fun `non-reference String is not localized`() {
        assertEquals("http://espn.com", "http://espn.com".localizeReference(tenant))
    }

    @Test
    fun `localizes reference String with full URL and no history`() {
        assertEquals(
            "StructureDefinition/test-c8973a22-2b5b-4e76-9c66-00639c99e61b",
            "http://fhir.hl7.org/svc/StructureDefinition/c8973a22-2b5b-4e76-9c66-00639c99e61b".localizeReference(tenant)
        )
    }

    @Test
    fun `localizes reference String with full URL and history`() {
        assertEquals(
            "Observation/test-1x2/_history/2",
            "http://example.org/fhir/Observation/1x2/_history/2".localizeReference(tenant)
        )
    }

    @Test
    fun `localizes reference String with relative URL and no history`() {
        assertEquals("Patient/test-034AB16", "Patient/034AB16".localizeReference(tenant))
    }

    @Test
    fun `localizes reference String with relative URL and history`() {
        assertEquals("Patient/test-034AB16/_history/100", "Patient/034AB16/_history/100".localizeReference(tenant))
    }

    @Test
    fun `pairs indicate updates if at least one pair has an update`() {
        val element1 = mockk<Age>()
        val element2 = mockk<Age>()
        val pairs = listOf(Pair(element1, false), Pair(element2, true))
        assertTrue(pairs.hasUpdates())
    }

    @Test
    fun `pairs indicate no updates if no pair has an update`() {
        val element1 = mockk<Age>()
        val element2 = mockk<Age>()
        val pairs = listOf(Pair(element1, false), Pair(element2, false))
        assertFalse(pairs.hasUpdates())
    }

    @Test
    fun `pairs returns all values regardless of update status`() {
        val element1 = mockk<Age>()
        val element2 = mockk<Age>()
        val pairs = listOf(Pair(element1, false), Pair(element2, false))
        assertEquals(listOf(element1, element2), pairs.values())
    }

    @Test
    fun `returns current address if address has no localizable information`() {
        val address = Address(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = AddressUse.HOME,
            type = AddressType.POSTAL,
            text = "Address",
            line = listOf("Line"),
            city = "City",
            district = "District",
            state = "State",
            postalCode = "PostalCode",
            country = "Country",
            period = Period(start = DateTime("2021"))
        )
        val localizedAddress = address.localize(tenant)
        assertTrue(address === localizedAddress)
    }

    @Test
    fun `localizes address with localizable extension`() {
        val address = Address(
            id = "12345",
            extension = localizableExtensions,
            use = AddressUse.HOME,
            type = AddressType.POSTAL,
            text = "Address",
            line = listOf("Line"),
            city = "City",
            district = "District",
            state = "State",
            postalCode = "PostalCode",
            country = "Country",
            period = Period(start = DateTime("2021"))
        )

        val localizedAddress = address.localize(tenant)
        assertTrue(address !== localizedAddress)

        val expectedAddress = Address(
            id = "12345",
            extension = localizedExtensions,
            use = AddressUse.HOME,
            type = AddressType.POSTAL,
            text = "Address",
            line = listOf("Line"),
            city = "City",
            district = "District",
            state = "State",
            postalCode = "PostalCode",
            country = "Country",
            period = Period(start = DateTime("2021"))
        )
        assertEquals(expectedAddress, localizedAddress)
    }

    @Test
    fun `localizes address with localizable period`() {
        val address = Address(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = AddressUse.HOME,
            type = AddressType.POSTAL,
            text = "Address",
            line = listOf("Line"),
            city = "City",
            district = "District",
            state = "State",
            postalCode = "PostalCode",
            country = "Country",
            period = Period(extension = localizableExtensions, start = DateTime("2021"))
        )

        val localizedAddress = address.localize(tenant)
        assertTrue(address !== localizedAddress)

        val expectedAddress = Address(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = AddressUse.HOME,
            type = AddressType.POSTAL,
            text = "Address",
            line = listOf("Line"),
            city = "City",
            district = "District",
            state = "State",
            postalCode = "PostalCode",
            country = "Country",
            period = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedAddress, localizedAddress)
    }

    @Test
    fun `localizes address with localizable extension and period`() {
        val address = Address(
            id = "12345",
            extension = localizableExtensions,
            use = AddressUse.HOME,
            type = AddressType.POSTAL,
            text = "Address",
            line = listOf("Line"),
            city = "City",
            district = "District",
            state = "State",
            postalCode = "PostalCode",
            country = "Country",
            period = Period(extension = localizableExtensions, start = DateTime("2021"))
        )

        val localizedAddress = address.localize(tenant)
        assertTrue(address !== localizedAddress)

        val expectedAddress = Address(
            id = "12345",
            extension = localizedExtensions,
            use = AddressUse.HOME,
            type = AddressType.POSTAL,
            text = "Address",
            line = listOf("Line"),
            city = "City",
            district = "District",
            state = "State",
            postalCode = "PostalCode",
            country = "Country",
            period = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedAddress, localizedAddress)
    }

    @Test
    fun `returns current attachment if attachment has no localizable information`() {
        val attachment = Attachment(
            id = "12345",
            extension = nonLocalizableExtensions,
            contentType = Code("contentType"),
            language = Code("language"),
            data = Base64Binary("abcd"),
            url = Url("url"),
            size = UnsignedInt(4),
            hash = Base64Binary("efgh"),
            title = "Title",
            creation = DateTime("2021")
        )
        val localizedAttachment = attachment.localize(tenant)
        assertTrue(attachment === localizedAttachment)
    }

    @Test
    fun `localizes attachment with localizable extension`() {
        val attachment = Attachment(
            id = "12345",
            extension = localizableExtensions,
            contentType = Code("contentType"),
            language = Code("language"),
            data = Base64Binary("abcd"),
            url = Url("url"),
            size = UnsignedInt(4),
            hash = Base64Binary("efgh"),
            title = "Title",
            creation = DateTime("2021")
        )
        val localizedAttachment = attachment.localize(tenant)
        assertTrue(attachment !== localizedAttachment)

        val expectedAttachment = Attachment(
            id = "12345",
            extension = localizedExtensions,
            contentType = Code("contentType"),
            language = Code("language"),
            data = Base64Binary("abcd"),
            url = Url("url"),
            size = UnsignedInt(4),
            hash = Base64Binary("efgh"),
            title = "Title",
            creation = DateTime("2021")
        )
        assertEquals(expectedAttachment, localizedAttachment)
    }

    @Test
    fun `returns current available time if available time has no localizable information`() {
        val availableTime = AvailableTime(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            daysOfWeek = listOf(Code("day")),
            allDay = true,
            availableStartTime = Time("08:00:00"),
            availableEndTime = Time("20:00:00")
        )
        val localizedAvailableTime = availableTime.localize(tenant)
        assertTrue(availableTime === localizedAvailableTime)
    }

    @Test
    fun `localizes available time with localizable extension`() {
        val availableTime = AvailableTime(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            daysOfWeek = listOf(Code("day")),
            allDay = true,
            availableStartTime = Time("08:00:00"),
            availableEndTime = Time("20:00:00")
        )
        val localizedAvailableTime = availableTime.localize(tenant)
        assertTrue(availableTime !== localizedAvailableTime)

        val expectedAvailableTime = AvailableTime(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = nonLocalizableExtensions,
            daysOfWeek = listOf(Code("day")),
            allDay = true,
            availableStartTime = Time("08:00:00"),
            availableEndTime = Time("20:00:00")
        )
        assertEquals(expectedAvailableTime, localizedAvailableTime)
    }

    @Test
    fun `localizes available time with localizable modifier extension`() {
        val availableTime = AvailableTime(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizableExtensions,
            daysOfWeek = listOf(Code("day")),
            allDay = true,
            availableStartTime = Time("08:00:00"),
            availableEndTime = Time("20:00:00")
        )
        val localizedAvailableTime = availableTime.localize(tenant)
        assertTrue(availableTime !== localizedAvailableTime)

        val expectedAvailableTime = AvailableTime(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizedExtensions,
            daysOfWeek = listOf(Code("day")),
            allDay = true,
            availableStartTime = Time("08:00:00"),
            availableEndTime = Time("20:00:00")
        )
        assertEquals(expectedAvailableTime, localizedAvailableTime)
    }

    @Test
    fun `localizes available time with localizable extension and modifier extension`() {
        val availableTime = AvailableTime(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = localizableExtensions,
            daysOfWeek = listOf(Code("day")),
            allDay = true,
            availableStartTime = Time("08:00:00"),
            availableEndTime = Time("20:00:00")
        )
        val localizedAvailableTime = availableTime.localize(tenant)
        assertTrue(availableTime !== localizedAvailableTime)

        val expectedAvailableTime = AvailableTime(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = localizedExtensions,
            daysOfWeek = listOf(Code("day")),
            allDay = true,
            availableStartTime = Time("08:00:00"),
            availableEndTime = Time("20:00:00")
        )
        assertEquals(expectedAvailableTime, localizedAvailableTime)
    }

    @Test
    fun `returns current codeable concept if codeable concept has no localizable information`() {
        val codeableConcept = CodeableConcept(
            id = "12345",
            extension = nonLocalizableExtensions,
            coding = listOf(Coding(display = "coding")),
            text = "Text"
        )
        val localizedCodeableConcept = codeableConcept.localize(tenant)
        assertTrue(codeableConcept === localizedCodeableConcept)
    }

    @Test
    fun `localizes codeable concept with localizable extension`() {
        val codeableConcept = CodeableConcept(
            id = "12345",
            extension = localizableExtensions,
            coding = listOf(Coding(display = "coding")),
            text = "Text"
        )
        val localizedCodeableConcept = codeableConcept.localize(tenant)
        assertTrue(codeableConcept !== localizedCodeableConcept)

        val expectedCodeableConcept = CodeableConcept(
            id = "12345",
            extension = localizedExtensions,
            coding = listOf(Coding(display = "coding")),
            text = "Text"
        )
        assertEquals(expectedCodeableConcept, localizedCodeableConcept)
    }

    @Test
    fun `returns current codeable concept as not updated if codeable concept has no localizable information`() {
        val codeableConcept = CodeableConcept(
            id = "12345",
            extension = nonLocalizableExtensions,
            coding = listOf(Coding(display = "coding")),
            text = "Text"
        )
        val (localizedCodeableConcept, updated) = codeableConcept.localizePair(tenant)
        assertTrue(codeableConcept === localizedCodeableConcept)
        assertFalse(updated)
    }

    @Test
    fun `localizes codeable concept and sets as updated with localizable extension`() {
        val codeableConcept = CodeableConcept(
            id = "12345",
            extension = localizableExtensions,
            coding = listOf(Coding(display = "coding")),
            text = "Text"
        )
        val (localizedCodeableConcept, updated) = codeableConcept.localizePair(tenant)
        assertTrue(codeableConcept !== localizedCodeableConcept)
        assertTrue(updated)

        val expectedCodeableConcept = CodeableConcept(
            id = "12345",
            extension = localizedExtensions,
            coding = listOf(Coding(display = "coding")),
            text = "Text"
        )
        assertEquals(expectedCodeableConcept, localizedCodeableConcept)
    }

    @Test
    fun `localizes codeable concept and sets as updated with localizable coding`() {
        val codeableConcept = CodeableConcept(
            id = "12345",
            extension = nonLocalizableExtensions,
            coding = listOf(Coding(extension = localizableExtensions, display = "coding")),
            text = "Text"
        )
        val (localizedCodeableConcept, updated) = codeableConcept.localizePair(tenant)
        assertTrue(codeableConcept !== localizedCodeableConcept)
        assertTrue(updated)

        val expectedCodeableConcept = CodeableConcept(
            id = "12345",
            extension = nonLocalizableExtensions,
            coding = listOf(Coding(extension = localizedExtensions, display = "coding")),
            text = "Text"
        )
        assertEquals(expectedCodeableConcept, localizedCodeableConcept)
    }

    @Test
    fun `localizes codeable concept and sets as updated with localizable extension and coding`() {
        val codeableConcept = CodeableConcept(
            id = "12345",
            extension = localizableExtensions,
            coding = listOf(Coding(extension = localizableExtensions, display = "coding")),
            text = "Text"
        )
        val (localizedCodeableConcept, updated) = codeableConcept.localizePair(tenant)
        assertTrue(codeableConcept !== localizedCodeableConcept)
        assertTrue(updated)

        val expectedCodeableConcept = CodeableConcept(
            id = "12345",
            extension = localizedExtensions,
            coding = listOf(Coding(extension = localizedExtensions, display = "coding")),
            text = "Text"
        )
        assertEquals(expectedCodeableConcept, localizedCodeableConcept)
    }

    @Test
    fun `returns current coding as not updated if coding has no localizable information`() {
        val coding = Coding(
            id = "12345",
            extension = nonLocalizableExtensions,
            system = Uri("system"),
            version = "version",
            code = Code("code"),
            display = "Display",
            userSelected = true
        )
        val (localizedCoding, updated) = coding.localizePair(tenant)
        assertTrue(coding === localizedCoding)
        assertFalse(updated)
    }

    @Test
    fun `localizes coding and sets as updated with localizable extension`() {
        val coding = Coding(
            id = "12345",
            extension = localizableExtensions,
            system = Uri("system"),
            version = "version",
            code = Code("code"),
            display = "Display",
            userSelected = true
        )
        val (localizedCoding, updated) = coding.localizePair(tenant)
        assertTrue(coding !== localizedCoding)
        assertTrue(updated)

        val expectedCoding = Coding(
            id = "12345",
            extension = localizedExtensions,
            system = Uri("system"),
            version = "version",
            code = Code("code"),
            display = "Display",
            userSelected = true
        )
        assertEquals(expectedCoding, localizedCoding)
    }

    @Test
    fun `returns current communication if communication has no localizable information`() {
        val communication = Communication(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            language = CodeableConcept(extension = nonLocalizableExtensions)
        )
        val localizedCommunication = communication.localize(tenant)
        assertTrue(localizedCommunication === communication)
    }

    @Test
    fun `localizes communication with localizable extension`() {
        val communication = Communication(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            language = CodeableConcept(extension = nonLocalizableExtensions)
        )
        val localizedCommunication = communication.localize(tenant)
        assertTrue(localizedCommunication !== communication)

        val expectedCommunication = Communication(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = nonLocalizableExtensions,
            language = CodeableConcept(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedCommunication, localizedCommunication)
    }

    @Test
    fun `localizes communication with localizable modifierExtension`() {
        val communication = Communication(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizableExtensions,
            language = CodeableConcept(extension = nonLocalizableExtensions)
        )
        val localizedCommunication = communication.localize(tenant)
        assertTrue(localizedCommunication !== communication)

        val expectedCommunication = Communication(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizedExtensions,
            language = CodeableConcept(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedCommunication, localizedCommunication)
    }

    @Test
    fun `localizes communication with localizable language`() {
        val communication = Communication(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            language = CodeableConcept(extension = localizableExtensions)
        )
        val localizedCommunication = communication.localize(tenant)
        assertTrue(localizedCommunication !== communication)

        val expectedCommunication = Communication(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            language = CodeableConcept(extension = localizedExtensions)
        )
        assertEquals(expectedCommunication, localizedCommunication)
    }

    @Test
    fun `localizes communication with all localizable values`() {
        val communication = Communication(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = localizableExtensions,
            language = CodeableConcept(extension = localizableExtensions)
        )
        val localizedCommunication = communication.localize(tenant)
        assertTrue(localizedCommunication !== communication)

        val expectedCommunication = Communication(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = localizedExtensions,
            language = CodeableConcept(extension = localizedExtensions)
        )
        assertEquals(expectedCommunication, localizedCommunication)
    }

    @Test
    fun `returns current contact if contact has no localizable information`() {
        val contact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedContact = contact.localize(tenant)
        assertTrue(localizedContact === contact)
    }

    @Test
    fun `localizes contact with localizable extension`() {
        val contact = Contact(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedContact = contact.localize(tenant)
        assertTrue(localizedContact !== contact)

        val expectedContact = Contact(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedContact, localizedContact)
    }

    @Test
    fun `localizes contact with localizable modifierExtension`() {
        val contact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedContact = contact.localize(tenant)
        assertTrue(localizedContact !== contact)

        val expectedContact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizedExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedContact, localizedContact)
    }

    @Test
    fun `localizes contact with localizable name`() {
        val contact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = localizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedContact = contact.localize(tenant)
        assertTrue(localizedContact !== contact)

        val expectedContact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = localizedExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedContact, localizedContact)
    }

    @Test
    fun `localizes contact with localizable telecom`() {
        val contact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = localizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedContact = contact.localize(tenant)
        assertTrue(localizedContact !== contact)

        val expectedContact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = localizedExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedContact, localizedContact)
    }

    @Test
    fun `localizes contact with localizable organization`() {
        val contact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = localizableExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedContact = contact.localize(tenant)
        assertTrue(localizedContact !== contact)

        val expectedContact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = localizedExtensions),
            period = Period(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedContact, localizedContact)
    }

    @Test
    fun `localizes contact with localizable period`() {
        val contact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = localizableExtensions)
        )
        val localizedContact = contact.localize(tenant)
        assertTrue(localizedContact !== contact)

        val expectedContact = Contact(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = nonLocalizableExtensions),
            telecom = listOf(ContactPoint(extension = nonLocalizableExtensions)),
            organization = Reference(extension = nonLocalizableExtensions),
            period = Period(extension = localizedExtensions)
        )
        assertEquals(expectedContact, localizedContact)
    }

    @Test
    fun `localizes contact with all localizable values`() {
        val contact = Contact(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = localizableExtensions,
            relationship = listOf(),
            name = HumanName(extension = localizableExtensions),
            telecom = listOf(ContactPoint(extension = localizableExtensions)),
            organization = Reference(extension = localizableExtensions),
            period = Period(extension = localizableExtensions)
        )
        val localizedContact = contact.localize(tenant)
        assertTrue(localizedContact !== contact)

        val expectedContact = Contact(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = localizedExtensions,
            relationship = listOf(),
            name = HumanName(extension = localizedExtensions),
            telecom = listOf(ContactPoint(extension = localizedExtensions)),
            organization = Reference(extension = localizedExtensions),
            period = Period(extension = localizedExtensions)
        )
        assertEquals(expectedContact, localizedContact)
    }

    @Test
    fun `localizes contact with null name, organization and period`() {
        val contact = Contact(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = localizableExtensions,
            relationship = listOf(),
            telecom = listOf(ContactPoint(extension = localizableExtensions))
        )
        val localizedContact = contact.localize(tenant)
        assertTrue(localizedContact !== contact)

        val expectedContact = Contact(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = localizedExtensions,
            relationship = listOf(),
            telecom = listOf(ContactPoint(extension = localizedExtensions))
        )
        assertEquals(expectedContact, localizedContact)
    }

    @Test
    fun `returns current contact point if contact point has no localizable information`() {
        val contactPoint = ContactPoint(
            id = "12345",
            extension = nonLocalizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(start = DateTime("2021"))
        )
        val localizedContactPoint = contactPoint.localize(tenant)
        assertTrue(contactPoint === localizedContactPoint)
    }

    @Test
    fun `localizes contact point with localizable extension`() {
        val contactPoint = ContactPoint(
            id = "12345",
            extension = localizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(start = DateTime("2021"))
        )
        val localizedContactPoint = contactPoint.localize(tenant)
        assertTrue(contactPoint !== localizedContactPoint)

        val expectedContactPoint = ContactPoint(
            id = "12345",
            extension = localizedExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(start = DateTime("2021"))
        )
        assertEquals(expectedContactPoint, localizedContactPoint)
    }

    @Test
    fun `localizes contact point with localizable period`() {
        val contactPoint = ContactPoint(
            id = "12345",
            extension = nonLocalizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(extension = localizableExtensions, start = DateTime("2021"))
        )
        val localizedContactPoint = contactPoint.localize(tenant)
        assertTrue(contactPoint !== localizedContactPoint)

        val expectedContactPoint = ContactPoint(
            id = "12345",
            extension = nonLocalizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedContactPoint, localizedContactPoint)
    }

    @Test
    fun `localizes contact point with localizable extension and period`() {
        val contactPoint = ContactPoint(
            id = "12345",
            extension = localizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(extension = localizableExtensions, start = DateTime("2021"))
        )
        val localizedContactPoint = contactPoint.localize(tenant)
        assertTrue(contactPoint !== localizedContactPoint)

        val expectedContactPoint = ContactPoint(
            id = "12345",
            extension = localizedExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedContactPoint, localizedContactPoint)
    }

    @Test
    fun `returns current contact point as not updated if contact point has no localizable information`() {
        val contactPoint = ContactPoint(
            id = "12345",
            extension = nonLocalizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(start = DateTime("2021"))
        )
        val (localizedContactPoint, updated) = contactPoint.localizePair(tenant)

        assertTrue(contactPoint === localizedContactPoint)
        assertFalse(updated)
    }

    @Test
    fun `localizes contact point with localizable extension and sets as updated`() {
        val contactPoint = ContactPoint(
            id = "12345",
            extension = localizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(start = DateTime("2021"))
        )
        val (localizedContactPoint, updated) = contactPoint.localizePair(tenant)
        assertTrue(contactPoint !== localizedContactPoint)
        assertTrue(updated)

        val expectedContactPoint = ContactPoint(
            id = "12345",
            extension = localizedExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(start = DateTime("2021"))
        )
        assertEquals(expectedContactPoint, localizedContactPoint)
    }

    @Test
    fun `localizes contact point with localizable period and sets as updated`() {
        val contactPoint = ContactPoint(
            id = "12345",
            extension = nonLocalizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(extension = localizableExtensions, start = DateTime("2021"))
        )
        val (localizedContactPoint, updated) = contactPoint.localizePair(tenant)
        assertTrue(contactPoint !== localizedContactPoint)
        assertTrue(updated)

        val expectedContactPoint = ContactPoint(
            id = "12345",
            extension = nonLocalizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedContactPoint, localizedContactPoint)
    }

    @Test
    fun `localizes contact point with localizable extension and period and sets as updated`() {
        val contactPoint = ContactPoint(
            id = "12345",
            extension = localizableExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(extension = localizableExtensions, start = DateTime("2021"))
        )
        val (localizedContactPoint, updated) = contactPoint.localizePair(tenant)
        assertTrue(contactPoint !== localizedContactPoint)
        assertTrue(updated)

        val expectedContactPoint = ContactPoint(
            id = "12345",
            extension = localizedExtensions,
            system = ContactPointSystem.PHONE,
            value = "8675309",
            use = ContactPointUse.OLD,
            rank = PositiveInt(3),
            period = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedContactPoint, localizedContactPoint)
    }

    @Test
    fun `returns current extension if extension has no localizable information`() {
        val extension = Extension(
            id = "12345",
            extension = nonLocalizableExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.STRING, "Value")
        )
        val localizedExtension = extension.localize(tenant)
        assertTrue(extension === localizedExtension)
    }

    @Test
    fun `localizes extension with localizable value`() {
        val extension = Extension(
            id = "12345",
            extension = nonLocalizableExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/123"))
        )
        val localizedExtension = extension.localize(tenant)
        assertTrue(extension !== localizedExtension)

        val expectedExtension = Extension(
            id = "12345",
            extension = nonLocalizableExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/test-123"))
        )
        assertEquals(expectedExtension, localizedExtension)
    }

    @Test
    fun `returns current extension as not updated if extension has no localizable information`() {
        val extension = Extension(
            id = "12345",
            extension = nonLocalizableExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.STRING, "Value")
        )
        val (localizedExtension, updated) = extension.localizePair(tenant)
        assertTrue(extension === localizedExtension)
        assertFalse(updated)
    }

    @Test
    fun `localizes extension and sets as updated with localizable extension`() {
        val extension = Extension(
            id = "12345",
            extension = localizableExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.STRING, "Value")
        )
        val (localizedExtension, updated) = extension.localizePair(tenant)
        assertTrue(extension !== localizedExtension)
        assertTrue(updated)

        val expectedExtension = Extension(
            id = "12345",
            extension = localizedExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.STRING, "Value")
        )
        assertEquals(expectedExtension, localizedExtension)
    }

    @Test
    fun `localizes extension and sets as updated with localizable value`() {
        val extension = Extension(
            id = "12345",
            extension = nonLocalizableExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/123"))
        )
        val (localizedExtension, updated) = extension.localizePair(tenant)
        assertTrue(extension !== localizedExtension)
        assertTrue(updated)

        val expectedExtension = Extension(
            id = "12345",
            extension = nonLocalizableExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/test-123"))
        )
        assertEquals(expectedExtension, localizedExtension)
    }

    @Test
    fun `localizes extension and sets as updated with localizable extension and value`() {
        val extension = Extension(
            id = "12345",
            extension = localizableExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/123"))
        )
        val (localizedExtension, updated) = extension.localizePair(tenant)
        assertTrue(extension !== localizedExtension)
        assertTrue(updated)

        val expectedExtension = Extension(
            id = "12345",
            extension = localizedExtensions,
            url = Uri("url"),
            value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/test-123"))
        )
        assertEquals(expectedExtension, localizedExtension)
    }

    @Test
    fun `returns current human name if human name has no localizable information`() {
        val humanName = HumanName(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = NameUse.OFFICIAL,
            text = "Text",
            family = "Family",
            given = listOf("Given"),
            prefix = listOf("Prefix"),
            suffix = listOf("Suffix"),
            period = Period(start = DateTime("2021"))
        )
        val localizedHumanName = humanName.localize(tenant)
        assertTrue(humanName === localizedHumanName)
    }

    @Test
    fun `localizes human name with localizable extension`() {
        val humanName = HumanName(
            id = "12345",
            extension = localizableExtensions,
            use = NameUse.OFFICIAL,
            text = "Text",
            family = "Family",
            given = listOf("Given"),
            prefix = listOf("Prefix"),
            suffix = listOf("Suffix"),
            period = Period(start = DateTime("2021"))
        )
        val localizedHumanName = humanName.localize(tenant)
        assertTrue(humanName !== localizedHumanName)

        val expectedHumanName = HumanName(
            id = "12345",
            extension = localizedExtensions,
            use = NameUse.OFFICIAL,
            text = "Text",
            family = "Family",
            given = listOf("Given"),
            prefix = listOf("Prefix"),
            suffix = listOf("Suffix"),
            period = Period(start = DateTime("2021"))
        )
        assertEquals(expectedHumanName, localizedHumanName)
    }

    @Test
    fun `localizes human name with localizable period`() {
        val humanName = HumanName(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = NameUse.OFFICIAL,
            text = "Text",
            family = "Family",
            given = listOf("Given"),
            prefix = listOf("Prefix"),
            suffix = listOf("Suffix"),
            period = Period(extension = localizableExtensions, start = DateTime("2021"))
        )
        val localizedHumanName = humanName.localize(tenant)
        assertTrue(humanName !== localizedHumanName)

        val expectedHumanName = HumanName(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = NameUse.OFFICIAL,
            text = "Text",
            family = "Family",
            given = listOf("Given"),
            prefix = listOf("Prefix"),
            suffix = listOf("Suffix"),
            period = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedHumanName, localizedHumanName)
    }

    @Test
    fun `localizes human name with localizable extension and period`() {
        val humanName = HumanName(
            id = "12345",
            extension = localizableExtensions,
            use = NameUse.OFFICIAL,
            text = "Text",
            family = "Family",
            given = listOf("Given"),
            prefix = listOf("Prefix"),
            suffix = listOf("Suffix"),
            period = Period(extension = localizableExtensions, start = DateTime("2021"))
        )
        val localizedHumanName = humanName.localize(tenant)
        assertTrue(humanName !== localizedHumanName)

        val expectedHumanName = HumanName(
            id = "12345",
            extension = localizedExtensions,
            use = NameUse.OFFICIAL,
            text = "Text",
            family = "Family",
            given = listOf("Given"),
            prefix = listOf("Prefix"),
            suffix = listOf("Suffix"),
            period = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedHumanName, localizedHumanName)
    }

    @Test
    fun `returns current identifier if identifier has no localizable information`() {
        val identifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        val localizedIdentifier = identifier.localize(tenant)
        assertTrue(identifier === localizedIdentifier)
    }

    @Test
    fun `localizes identifier with localizable period`() {
        val identifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(extension = localizableExtensions, start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        val localizedIdentifier = identifier.localize(tenant)
        assertTrue(identifier !== localizedIdentifier)

        val expectedIdentifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(extension = localizedExtensions, start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        assertEquals(expectedIdentifier, localizedIdentifier)
    }

    @Test
    fun `returns current identifier as not updated if identifier has no localizable information`() {
        val identifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        val (localizedIdentifier, updated) = identifier.localizePair(tenant)
        assertTrue(identifier === localizedIdentifier)
        assertFalse(updated)
    }

    @Test
    fun `localizes identifier and sets as updated with localizable extension`() {
        val identifier = Identifier(
            id = "12345",
            extension = localizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        val (localizedIdentifier, updated) = identifier.localizePair(tenant)
        assertTrue(identifier !== localizedIdentifier)
        assertTrue(updated)

        val expectedIdentifier = Identifier(
            id = "12345",
            extension = localizedExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        assertEquals(expectedIdentifier, localizedIdentifier)
    }

    @Test
    fun `localizes identifier and sets as updated with localizable type`() {
        val identifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(extension = localizableExtensions, text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        val (localizedIdentifier, updated) = identifier.localizePair(tenant)
        assertTrue(identifier !== localizedIdentifier)
        assertTrue(updated)

        val expectedIdentifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(extension = localizedExtensions, text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        assertEquals(expectedIdentifier, localizedIdentifier)
    }

    @Test
    fun `localizes identifier and sets as updated with localizable period`() {
        val identifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(extension = localizableExtensions, start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        val (localizedIdentifier, updated) = identifier.localizePair(tenant)
        assertTrue(identifier !== localizedIdentifier)
        assertTrue(updated)

        val expectedIdentifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(extension = localizedExtensions, start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        assertEquals(expectedIdentifier, localizedIdentifier)
    }

    @Test
    fun `localizes identifier and sets as updated with localizable assigner`() {
        val identifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(reference = "Organization/123")
        )
        val (localizedIdentifier, updated) = identifier.localizePair(tenant)
        assertTrue(identifier !== localizedIdentifier)
        assertTrue(updated)

        val expectedIdentifier = Identifier(
            id = "12345",
            extension = nonLocalizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(reference = "Organization/test-123")
        )
        assertEquals(expectedIdentifier, localizedIdentifier)
    }

    @Test
    fun `localizes identifier and sets as updated with localizable extension, type, period and assigner`() {
        val identifier = Identifier(
            id = "12345",
            extension = localizableExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(extension = localizableExtensions, text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(extension = localizableExtensions, start = DateTime("2021")),
            assigner = Reference(reference = "Organization/123")
        )
        val (localizedIdentifier, updated) = identifier.localizePair(tenant)
        assertTrue(identifier !== localizedIdentifier)
        assertTrue(updated)

        val expectedIdentifier = Identifier(
            id = "12345",
            extension = localizedExtensions,
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(extension = localizedExtensions, text = "type"),
            system = Uri("system"),
            value = "value",
            period = Period(extension = localizedExtensions, start = DateTime("2021")),
            assigner = Reference(reference = "Organization/test-123")
        )
        assertEquals(expectedIdentifier, localizedIdentifier)
    }

    @Test
    fun `returns current link if link has no localizable information`() {
        val link = PatientLink(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            other = Reference(extension = nonLocalizableExtensions),
            type = LinkType.REPLACES
        )
        val localizedLink = link.localize(tenant)
        assertTrue(localizedLink === link)
    }

    @Test
    fun `localizes link with localizable extension`() {
        val link = PatientLink(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            other = Reference(extension = nonLocalizableExtensions),
            type = LinkType.REPLACES
        )
        val localizedLink = link.localize(tenant)
        assertTrue(localizedLink !== link)

        val expectedLink = PatientLink(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = nonLocalizableExtensions,
            other = Reference(extension = nonLocalizableExtensions),
            type = LinkType.REPLACES
        )
        assertEquals(expectedLink, localizedLink)
    }

    @Test
    fun `localizes link with localizable modifierExtension`() {
        val link = PatientLink(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizableExtensions,
            other = Reference(extension = nonLocalizableExtensions),
            type = LinkType.REPLACES
        )
        val localizedLink = link.localize(tenant)
        assertTrue(localizedLink !== link)

        val expectedLink = PatientLink(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizedExtensions,
            other = Reference(extension = nonLocalizableExtensions),
            type = LinkType.REPLACES
        )
        assertEquals(expectedLink, localizedLink)
    }

    @Test
    fun `localizes link with localizable other`() {
        val link = PatientLink(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            other = Reference(extension = localizableExtensions),
            type = LinkType.REPLACES
        )
        val localizedLink = link.localize(tenant)
        assertTrue(localizedLink !== link)

        val expectedLink = PatientLink(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            other = Reference(extension = localizedExtensions),
            type = LinkType.REPLACES
        )
        assertEquals(expectedLink, localizedLink)
    }

    @Test
    fun `localizes link with all localizable values`() {
        val link = PatientLink(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = localizableExtensions,
            other = Reference(extension = localizableExtensions),
            type = LinkType.REPLACES
        )
        val localizedLink = link.localize(tenant)
        assertTrue(localizedLink !== link)

        val expectedLink = PatientLink(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = localizedExtensions,
            other = Reference(extension = localizedExtensions),
            type = LinkType.REPLACES
        )
        assertEquals(expectedLink, localizedLink)
    }

    @Test
    fun `returns current meta if meta has no localizable information`() {
        val meta = Meta(
            id = "12345",
            extension = nonLocalizableExtensions,
            versionId = Id("versionId"),
            lastUpdated = Instant("2015-02-07T13:28:17.239+02:00"),
            source = Uri("source"),
            profile = listOf(Canonical("profile")),
            security = listOf(Coding(display = "security")),
            tag = listOf(Coding(display = "tag"))
        )
        val localizedMeta = meta.localize(tenant)
        assertTrue(meta === localizedMeta)
    }

    @Test
    fun `localizes meta with localizable extension`() {
        val meta = Meta(
            id = "12345",
            extension = localizableExtensions,
            versionId = Id("versionId"),
            lastUpdated = Instant("2015-02-07T13:28:17.239+02:00"),
            source = Uri("source"),
            profile = listOf(Canonical("profile")),
            security = listOf(Coding(display = "security")),
            tag = listOf(Coding(display = "tag"))
        )
        val localizedMeta = meta.localize(tenant)
        assertTrue(meta !== localizedMeta)

        val expectedMeta = Meta(
            id = "12345",
            extension = localizedExtensions,
            versionId = Id("versionId"),
            lastUpdated = Instant("2015-02-07T13:28:17.239+02:00"),
            source = Uri("source"),
            profile = listOf(Canonical("profile")),
            security = listOf(Coding(display = "security")),
            tag = listOf(Coding(display = "tag"))
        )
        assertEquals(expectedMeta, localizedMeta)
    }

    @Test
    fun `localizes meta with localizable security`() {
        val meta = Meta(
            id = "12345",
            extension = nonLocalizableExtensions,
            versionId = Id("versionId"),
            lastUpdated = Instant("2015-02-07T13:28:17.239+02:00"),
            source = Uri("source"),
            profile = listOf(Canonical("profile")),
            security = listOf(Coding(extension = localizableExtensions, display = "security")),
            tag = listOf(Coding(display = "tag"))
        )
        val localizedMeta = meta.localize(tenant)
        assertTrue(meta !== localizedMeta)

        val expectedMeta = Meta(
            id = "12345",
            extension = nonLocalizableExtensions,
            versionId = Id("versionId"),
            lastUpdated = Instant("2015-02-07T13:28:17.239+02:00"),
            source = Uri("source"),
            profile = listOf(Canonical("profile")),
            security = listOf(Coding(extension = localizedExtensions, display = "security")),
            tag = listOf(Coding(display = "tag"))
        )
        assertEquals(expectedMeta, localizedMeta)
    }

    @Test
    fun `localizes meta with localizable tag`() {
        val meta = Meta(
            id = "12345",
            extension = nonLocalizableExtensions,
            versionId = Id("versionId"),
            lastUpdated = Instant("2015-02-07T13:28:17.239+02:00"),
            source = Uri("source"),
            profile = listOf(Canonical("profile")),
            security = listOf(Coding(display = "security")),
            tag = listOf(Coding(extension = localizableExtensions, display = "tag"))
        )
        val localizedMeta = meta.localize(tenant)
        assertTrue(meta !== localizedMeta)

        val expectedMeta = Meta(
            id = "12345",
            extension = nonLocalizableExtensions,
            versionId = Id("versionId"),
            lastUpdated = Instant("2015-02-07T13:28:17.239+02:00"),
            source = Uri("source"),
            profile = listOf(Canonical("profile")),
            security = listOf(Coding(display = "security")),
            tag = listOf(Coding(extension = localizedExtensions, display = "tag"))
        )
        assertEquals(expectedMeta, localizedMeta)
    }

    @Test
    fun `localizes meta with localizable extension, security and tag`() {
        val meta = Meta(
            id = "12345",
            extension = localizableExtensions,
            versionId = Id("versionId"),
            lastUpdated = Instant("2015-02-07T13:28:17.239+02:00"),
            source = Uri("source"),
            profile = listOf(Canonical("profile")),
            security = listOf(Coding(extension = localizableExtensions, display = "security")),
            tag = listOf(Coding(extension = localizableExtensions, display = "tag"))
        )
        val localizedMeta = meta.localize(tenant)
        assertTrue(meta !== localizedMeta)

        val expectedMeta = Meta(
            id = "12345",
            extension = localizedExtensions,
            versionId = Id("versionId"),
            lastUpdated = Instant("2015-02-07T13:28:17.239+02:00"),
            source = Uri("source"),
            profile = listOf(Canonical("profile")),
            security = listOf(Coding(extension = localizedExtensions, display = "security")),
            tag = listOf(Coding(extension = localizedExtensions, display = "tag"))
        )
        assertEquals(expectedMeta, localizedMeta)
    }

    @Test
    fun `returns current narrative if narrative has no localizable information`() {
        val narrative = Narrative(
            id = "12345",
            extension = nonLocalizableExtensions,
            status = NarrativeStatus.GENERATED,
            div = "div"
        )
        val localizedNarrative = narrative.localize(tenant)
        assertTrue(narrative === localizedNarrative)
    }

    @Test
    fun `localizes narrative with localizable extension`() {
        val narrative = Narrative(
            id = "12345",
            extension = localizableExtensions,
            status = NarrativeStatus.GENERATED,
            div = "div"
        )
        val localizedNarrative = narrative.localize(tenant)
        assertTrue(narrative !== localizedNarrative)

        val expectedNarrative = Narrative(
            id = "12345",
            extension = localizedExtensions,
            status = NarrativeStatus.GENERATED,
            div = "div"
        )
        assertEquals(expectedNarrative, localizedNarrative)
    }

    @Test
    fun `returns current not available if not available has no localizable information`() {
        val notAvailable = NotAvailable(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            description = "description",
            during = Period(start = DateTime("2021"))
        )
        val localizedNotAvailable = notAvailable.localize(tenant)
        assertTrue(notAvailable === localizedNotAvailable)
    }

    @Test
    fun `localizes not available with localizable extension`() {
        val notAvailable = NotAvailable(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            description = "description",
            during = Period(start = DateTime("2021"))
        )
        val localizedNotAvailable = notAvailable.localize(tenant)
        assertTrue(notAvailable !== localizedNotAvailable)

        val expectedNotAvailable = NotAvailable(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = nonLocalizableExtensions,
            description = "description",
            during = Period(start = DateTime("2021"))
        )
        assertEquals(expectedNotAvailable, localizedNotAvailable)
    }

    @Test
    fun `localizes not available with localizable modifier extension`() {
        val notAvailable = NotAvailable(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizableExtensions,
            description = "description",
            during = Period(start = DateTime("2021"))
        )
        val localizedNotAvailable = notAvailable.localize(tenant)
        assertTrue(notAvailable !== localizedNotAvailable)

        val expectedNotAvailable = NotAvailable(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizedExtensions,
            description = "description",
            during = Period(start = DateTime("2021"))
        )
        assertEquals(expectedNotAvailable, localizedNotAvailable)
    }

    @Test
    fun `localizes not available with localizable during`() {
        val notAvailable = NotAvailable(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            description = "description",
            during = Period(extension = localizableExtensions, start = DateTime("2021"))
        )
        val localizedNotAvailable = notAvailable.localize(tenant)
        assertTrue(notAvailable !== localizedNotAvailable)

        val expectedNotAvailable = NotAvailable(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            description = "description",
            during = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedNotAvailable, localizedNotAvailable)
    }

    @Test
    fun `localizes not available with localizable extension, modifier extension and during`() {
        val notAvailable = NotAvailable(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = localizableExtensions,
            description = "description",
            during = Period(extension = localizableExtensions, start = DateTime("2021"))
        )
        val localizedNotAvailable = notAvailable.localize(tenant)
        assertTrue(notAvailable !== localizedNotAvailable)

        val expectedNotAvailable = NotAvailable(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = localizedExtensions,
            description = "description",
            during = Period(extension = localizedExtensions, start = DateTime("2021"))
        )
        assertEquals(expectedNotAvailable, localizedNotAvailable)
    }

    @Test
    fun `returns current participant if participant has no localizable information`() {
        val participant = Participant(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            type = listOf(CodeableConcept(extension = nonLocalizableExtensions)),
            actor = listOf(Reference(extension = nonLocalizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedParticipant = participant.localize(tenant)
        assertTrue(localizedParticipant === participant)
    }

    @Test
    fun `localizes pariticipant with localizable extension`() {
        val participant = Participant(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            type = listOf(CodeableConcept(extension = nonLocalizableExtensions)),
            actor = listOf(Reference(extension = nonLocalizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedParticipant = participant.localize(tenant)
        assertTrue(localizedParticipant !== participant)

        val expectedParticipant = Participant(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = nonLocalizableExtensions,
            type = listOf(CodeableConcept(extension = nonLocalizableExtensions)),
            actor = listOf(Reference(extension = nonLocalizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedParticipant, localizedParticipant)
    }

    @Test
    fun `localizes pariticipant with localizable modifierExtension`() {
        val participant = Participant(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizableExtensions,
            type = listOf(CodeableConcept(extension = nonLocalizableExtensions)),
            actor = listOf(Reference(extension = nonLocalizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedParticipant = participant.localize(tenant)
        assertTrue(localizedParticipant !== participant)

        val expectedParticipant = Participant(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizedExtensions,
            type = listOf(CodeableConcept(extension = nonLocalizableExtensions)),
            actor = listOf(Reference(extension = nonLocalizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedParticipant, localizedParticipant)
    }

    @Test
    fun `localizes pariticipant with localizable type`() {
        val participant = Participant(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            type = listOf(CodeableConcept(extension = localizableExtensions)),
            actor = listOf(Reference(extension = nonLocalizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedParticipant = participant.localize(tenant)
        assertTrue(localizedParticipant !== participant)

        val expectedParticipant = Participant(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            type = listOf(CodeableConcept(extension = localizedExtensions)),
            actor = listOf(Reference(extension = nonLocalizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedParticipant, localizedParticipant)
    }

    @Test
    fun `localizes pariticipant with localizable actor`() {
        val participant = Participant(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            type = listOf(CodeableConcept(extension = nonLocalizableExtensions)),
            actor = listOf(Reference(extension = localizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = nonLocalizableExtensions)
        )
        val localizedParticipant = participant.localize(tenant)
        assertTrue(localizedParticipant !== participant)

        val expectedParticipant = Participant(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            type = listOf(CodeableConcept(extension = nonLocalizableExtensions)),
            actor = listOf(Reference(extension = localizedExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = nonLocalizableExtensions)
        )
        assertEquals(expectedParticipant, localizedParticipant)
    }

    @Test
    fun `localizes pariticipant with localizable period`() {
        val participant = Participant(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            type = listOf(CodeableConcept(extension = nonLocalizableExtensions)),
            actor = listOf(Reference(extension = nonLocalizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = localizableExtensions)
        )
        val localizedParticipant = participant.localize(tenant)
        assertTrue(localizedParticipant !== participant)

        val expectedParticipant = Participant(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            type = listOf(CodeableConcept(extension = nonLocalizableExtensions)),
            actor = listOf(Reference(extension = nonLocalizableExtensions)),
            required = ParticipantRequired.REQUIRED,
            status = ParticipationStatus.ACCEPTED,
            period = Period(extension = localizedExtensions)
        )
        assertEquals(expectedParticipant, localizedParticipant)
    }

    @Test
    fun `returns current period if period has no localizable information`() {
        val period = Period(
            id = "12345",
            extension = nonLocalizableExtensions,
            start = DateTime("2020"),
            end = DateTime("2023")
        )
        val localizedPeriod = period.localize(tenant)
        assertTrue(period === localizedPeriod)
    }

    @Test
    fun `localizes period with localizable extension`() {
        val period = Period(
            id = "12345",
            extension = localizableExtensions,
            start = DateTime("2020"),
            end = DateTime("2023")
        )
        val localizedPeriod = period.localize(tenant)
        assertTrue(period !== localizedPeriod)

        val expectedPeriod = Period(
            id = "12345",
            extension = localizedExtensions,
            start = DateTime("2020"),
            end = DateTime("2023")
        )
        assertEquals(expectedPeriod, localizedPeriod)
    }

    @Test
    fun `returns current period as not updated if period has no localizable information`() {
        val period = Period(
            id = "12345",
            extension = nonLocalizableExtensions,
            start = DateTime("2020"),
            end = DateTime("2023")
        )
        val (localizedPeriod, updated) = period.localizePair(tenant)
        assertTrue(period === localizedPeriod)
        assertFalse(updated)
    }

    @Test
    fun `localizes period and sets as updated with localizable extension`() {
        val period = Period(
            id = "12345",
            extension = localizableExtensions,
            start = DateTime("2020"),
            end = DateTime("2023")
        )
        val (localizedPeriod, updated) = period.localizePair(tenant)
        assertTrue(period !== localizedPeriod)
        assertTrue(updated)

        val expectedPeriod = Period(
            id = "12345",
            extension = localizedExtensions,
            start = DateTime("2020"),
            end = DateTime("2023")
        )
        assertEquals(expectedPeriod, localizedPeriod)
    }

    @Test
    fun `returns current qualification if qualification has no localizable information`() {
        val qualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        val localizedQualification = qualification.localize(tenant)
        assertTrue(qualification === localizedQualification)
    }

    @Test
    fun `localizes qualification with localizable extension`() {
        val qualification = Qualification(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        val localizedQualification = qualification.localize(tenant)
        assertTrue(qualification !== localizedQualification)

        val expectedQualification = Qualification(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        assertEquals(expectedQualification, localizedQualification)
    }

    @Test
    fun `localizes qualification with localizable modifier extension`() {
        val qualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        val localizedQualification = qualification.localize(tenant)
        assertTrue(qualification !== localizedQualification)

        val expectedQualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = localizedExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        assertEquals(expectedQualification, localizedQualification)
    }

    @Test
    fun `localizes qualification with localizable identifier`() {
        val qualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(extension = localizableExtensions, value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        val localizedQualification = qualification.localize(tenant)
        assertTrue(qualification !== localizedQualification)

        val expectedQualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(extension = localizedExtensions, value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        assertEquals(expectedQualification, localizedQualification)
    }

    @Test
    fun `localizes qualification with localizable code`() {
        val qualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(extension = localizableExtensions, text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        val localizedQualification = qualification.localize(tenant)
        assertTrue(qualification !== localizedQualification)

        val expectedQualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(extension = localizedExtensions, text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        assertEquals(expectedQualification, localizedQualification)
    }

    @Test
    fun `localizes qualification with localizable period`() {
        val qualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(extension = localizableExtensions, start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        val localizedQualification = qualification.localize(tenant)
        assertTrue(qualification !== localizedQualification)

        val expectedQualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(extension = localizedExtensions, start = DateTime("2021")),
            issuer = Reference(display = "issuer")
        )
        assertEquals(expectedQualification, localizedQualification)
    }

    @Test
    fun `localizes qualification with localizable issuer`() {
        val qualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(extension = localizableExtensions, display = "issuer")
        )
        val localizedQualification = qualification.localize(tenant)
        assertTrue(qualification !== localizedQualification)

        val expectedQualification = Qualification(
            id = "12345",
            extension = nonLocalizableExtensions,
            modifierExtension = nonLocalizableExtensions,
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2021")),
            issuer = Reference(extension = localizedExtensions, display = "issuer")
        )
        assertEquals(expectedQualification, localizedQualification)
    }

    @Test
    fun `localizes qualification with localizable extension, modifier extension, identifier, code, period and issuer`() {
        val qualification = Qualification(
            id = "12345",
            extension = localizableExtensions,
            modifierExtension = localizableExtensions,
            identifier = listOf(Identifier(extension = localizableExtensions, value = "id")),
            code = CodeableConcept(extension = localizableExtensions, text = "code"),
            period = Period(extension = localizableExtensions, start = DateTime("2021")),
            issuer = Reference(extension = localizableExtensions, display = "issuer")
        )
        val localizedQualification = qualification.localize(tenant)
        assertTrue(qualification !== localizedQualification)

        val expectedQualification = Qualification(
            id = "12345",
            extension = localizedExtensions,
            modifierExtension = localizedExtensions,
            identifier = listOf(Identifier(extension = localizedExtensions, value = "id")),
            code = CodeableConcept(extension = localizedExtensions, text = "code"),
            period = Period(extension = localizedExtensions, start = DateTime("2021")),
            issuer = Reference(extension = localizedExtensions, display = "issuer")
        )
        assertEquals(expectedQualification, localizedQualification)
    }

    @Test
    fun `returns current reference if reference has no localizable information`() {
        val reference = Reference(
            id = "12345",
            extension = nonLocalizableExtensions,
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )

        val localizedReference = reference.localize(tenant)
        assertTrue(reference === localizedReference)
    }

    @Test
    fun `localizes reference with localizable reference string`() {
        val reference = Reference(
            id = "12345",
            extension = nonLocalizableExtensions,
            reference = "Patient/123",
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )

        val localizedReference = reference.localize(tenant)
        assertTrue(reference !== localizedReference)

        val expectedReference = Reference(
            id = "12345",
            extension = nonLocalizableExtensions,
            reference = "Patient/test-123",
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )
        assertEquals(expectedReference, localizedReference)
    }

    @Test
    fun `returns current reference as not updated if reference has no localizable information`() {
        val reference = Reference(
            id = "12345",
            extension = nonLocalizableExtensions,
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )

        val (localizedReference, updated) = reference.localizePair(tenant)
        assertTrue(reference === localizedReference)
        assertFalse(updated)
    }

    @Test
    fun `localizes reference and sets as updated with localizable extension`() {
        val reference = Reference(
            id = "12345",
            extension = localizableExtensions,
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )

        val (localizedReference, updated) = reference.localizePair(tenant)
        assertTrue(reference !== localizedReference)
        assertTrue(updated)

        val expectedReference = Reference(
            id = "12345",
            extension = localizedExtensions,
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )
        assertEquals(expectedReference, localizedReference)
    }

    @Test
    fun `localizes reference and sets as updated with localizable reference string`() {
        val reference = Reference(
            id = "12345",
            extension = nonLocalizableExtensions,
            reference = "Patient/123",
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )

        val (localizedReference, updated) = reference.localizePair(tenant)
        assertTrue(reference !== localizedReference)
        assertTrue(updated)

        val expectedReference = Reference(
            id = "12345",
            extension = nonLocalizableExtensions,
            reference = "Patient/test-123",
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )
        assertEquals(expectedReference, localizedReference)
    }

    @Test
    fun `localizes reference and sets as updated with localizable extension and reference string`() {
        val reference = Reference(
            id = "12345",
            extension = localizableExtensions,
            reference = "Patient/123",
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )

        val (localizedReference, updated) = reference.localizePair(tenant)
        assertTrue(reference !== localizedReference)
        assertTrue(updated)

        val expectedReference = Reference(
            id = "12345",
            extension = localizedExtensions,
            reference = "Patient/test-123",
            type = Uri("Patient"),
            identifier = Identifier(value = "123"),
            display = "Patient 123"
        )
        assertEquals(expectedReference, localizedReference)
    }
}
