package com.projectronin.interop.fhir.r4

import com.projectronin.interop.fhir.r4.datatype.BundleEntry
import com.projectronin.interop.fhir.r4.datatype.BundleLink
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.Signature
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.valueset.BundleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BundleUtilTest {
    @Test
    fun `merge retains original metadata`() {
        val profileCanonical1 =
            Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner")
        val link1 = BundleLink(relation = "next", url = Uri("http://example.com"))
        val signature1 = Signature(
            type = listOf(Coding(display = "type")),
            `when` = Instant("2017-01-01T00:00:00Z"),
            who = Reference(reference = "who")
        )
        val bundle1 = Bundle(
            id = Id("123"),
            meta = Meta(profile = listOf(profileCanonical1)),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            identifier = Identifier(value = "identifier"),
            type = BundleType.SEARCHSET,
            timestamp = Instant("2017-01-01T00:00:00Z"),
            total = UnsignedInt(1),
            link = listOf(link1),
            entry = listOf(BundleEntry()),
            signature = signature1
        )

        val profileCanonical2 =
            Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner2")
        val link2 = BundleLink(relation = "next", url = Uri("http://example.com/2"))
        val signature2 = Signature(
            type = listOf(Coding(display = "type2")),
            `when` = Instant("2017-01-01T00:00:00Z"),
            who = Reference(reference = "who2")
        )
        val bundle2 = Bundle(
            id = Id("1232"),
            meta = Meta(profile = listOf(profileCanonical2)),
            implicitRules = Uri("implicit-rules2"),
            language = Code("en-US2"),
            identifier = Identifier(value = "identifier2"),
            type = BundleType.BATCH,
            timestamp = Instant("2017-01-02T00:00:00Z"),
            total = UnsignedInt(1),
            link = listOf(link2),
            entry = listOf(BundleEntry()),
            signature = signature2
        )

        val merged = mergeBundles(bundle1, bundle2)
        assertEquals(Id("123"), merged.id)
        assertEquals(Meta(profile = listOf(profileCanonical1)), merged.meta)
        assertEquals(Uri("implicit-rules"), merged.implicitRules)
        assertEquals(Code("en-US"), merged.language)
        assertEquals(Identifier(value = "identifier"), merged.identifier)
        assertEquals(BundleType.SEARCHSET, merged.type)
        assertEquals(Instant("2017-01-01T00:00:00Z"), merged.timestamp)
        assertEquals(listOf(link1), merged.link)
        assertEquals(signature1, merged.signature)
    }

    @Test
    fun `merge can combine empty bundles`() {
        val bundle1 = Bundle(
            id = Id("123"),
            type = BundleType.SEARCHSET
        )
        val bundle2 = Bundle(
            id = Id("1232"),
            type = BundleType.SEARCHSET
        )

        val merged = mergeBundles(bundle1, bundle2)
        assertEquals(0, merged.total?.value)
        assertEquals(listOf<BundleEntry>(), merged.entry)
    }

    @Test
    fun `merge can combine empty and populated bundle`() {
        val practitionerRole = PractitionerRole(
            identifier = listOf(Identifier(value = "id"))
        )

        val bundle1 = Bundle(
            id = Id("123"),
            type = BundleType.SEARCHSET
        )
        val bundle2 = Bundle(
            id = Id("1232"),
            type = BundleType.SEARCHSET,
            total = UnsignedInt(1),
            entry = listOf(BundleEntry(resource = practitionerRole))
        )

        val merged = mergeBundles(bundle1, bundle2)
        assertEquals(1, merged.total?.value)
        assertEquals(listOf(BundleEntry(resource = practitionerRole)), merged.entry)
    }

    @Test
    fun `merge can combine populated bundles`() {
        val practitionerRole = PractitionerRole(
            identifier = listOf(Identifier(value = "id"))
        )

        val bundle1 = Bundle(
            id = Id("123"),
            type = BundleType.SEARCHSET,
            total = UnsignedInt(1),
            entry = listOf(BundleEntry(resource = practitionerRole))
        )
        val bundle2 = Bundle(
            id = Id("1232"),
            type = BundleType.SEARCHSET,
            total = UnsignedInt(1),
            entry = listOf(BundleEntry(resource = practitionerRole))
        )

        val merged = mergeBundles(bundle1, bundle2)
        assertEquals(2, merged.total?.value)
        assertEquals(
            listOf(BundleEntry(resource = practitionerRole), BundleEntry(resource = practitionerRole)),
            merged.entry
        )
    }
}
