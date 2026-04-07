// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.payment.service

import kotlin.test.Test
import kotlin.test.assertEquals

internal class BicMapperTest {
    val bicMapper = BicMapper()

    fun ibanWithId(id: Int): String = "FI42" + id.toString() + "12345678901"

    fun oneDigitTest(bankIdentifier: Int, correctBic: String) {
        for (i in bankIdentifier * 100..bankIdentifier * 100 + 99) {
            assertEquals(bicMapper.mapIban(ibanWithId(i)), correctBic)
        }
    }

    fun twoDigitTest(bankIdentifier: Int, correctBic: String) {
        for (i in bankIdentifier * 10..bankIdentifier * 10 + 9) {
            assertEquals(bicMapper.mapIban(ibanWithId(i)), correctBic)
        }
    }

    fun threeDigitTest(bankIdentifier: Int, correctBic: String) {
        assertEquals(bicMapper.mapIban(ibanWithId(bankIdentifier)), correctBic)
    }

    fun rangeTest(identifierRange: IntProgression, correctBic: String) {
        for (i in identifierRange) {
            assertEquals(bicMapper.mapIban(ibanWithId(i)), correctBic)
        }
    }

    @Test
    fun `should return INVALID  for invalid IBAN`() {
        assertEquals(bicMapper.mapIban("X"), "INVALID")
    }

    @Test
    fun `should return CITIFIHX for bank identifier 713`() {
        threeDigitTest(713, "CITIFIHX")
    }

    @Test
    fun `should return HELSFIHH for bank identifiers 405 and 497`() {
        threeDigitTest(405, "HELSFIHH")
        threeDigitTest(497, "HELSFIHH")
    }

    @Test
    fun `should return BIGKFIH1 for bank identifier 717`() {
        threeDigitTest(717, "BIGKFIH1")
    }

    @Test
    fun `should return DNBAFIHX for bank identifier 37`() {
        twoDigitTest(37, "DNBAFIHX")
    }

    @Test
    fun `should return OKOYFIHH for bank identifier 5`() {
        oneDigitTest(5, "OKOYFIHH")
    }

    @Test
    fun `should return POPFFI22 for bank identifiers 470-479`() {
        twoDigitTest(47, "POPFFI22")
    }

    @Test
    fun `should return DABAFIHH for bank identifiers 34 and 8`() {
        twoDigitTest(34, "DABAFIHH")
        oneDigitTest(8, "DABAFIHH")
    }

    @Test
    fun `should return HANDFIHH for bank identifier 31`() {
        twoDigitTest(31, "HANDFIHH")
    }

    @Test
    fun `should return HOLVFIHH for bank identifier 799`() {
        threeDigitTest(799, "HOLVFIHH")
    }

    @Test
    fun `should return NDEAFIHH for bank identifiers 1 and 2`() {
        oneDigitTest(1, "NDEAFIHH")
        oneDigitTest(2, "NDEAFIHH")
    }

    @Test
    fun `should return ESSEFIHX for bank identifier 33`() {
        twoDigitTest(33, "ESSEFIHX")
    }

    @Test
    fun `should return SBANFIHH for bank identifiers 36 and 39`() {
        twoDigitTest(36, "SBANFIHH")
        twoDigitTest(39, "SBANFIHH")
    }

    @Test
    fun `should return SWEDFIHH for bank identifier 38`() {
        twoDigitTest(38, "SWEDFIHH")
    }

    @Test
    fun `should return VPAYFIH2 for bank identifier 798`() {
        threeDigitTest(798, "VPAYFIH2")
    }

    @Test
    fun `should return AABAFI22 for bank identifier 6`() {
        oneDigitTest(6, "AABAFI22")
    }

    @Test
    fun `should return ITELFIHH for various bank identifiers`() {
        val bic = "ITELFIHH"
        var identifiers =
            listOf(
                402..403,
                406..408,
                410..412,
                414..421,
                423..432,
                435..452,
                454..464,
                483..493,
                495..496,
            )
        threeDigitTest(715, bic)
        threeDigitTest(400, bic)

        for (identifierRange in identifiers) {
            rangeTest(identifierRange, bic)
        }
    }

    @Test
    fun `should return UNKNOWN for undefined bank identifiers`() {
        // some random undefined identifiers
        val identifiers = listOf(401, 791, 499, 300)

        for (identifier in identifiers) {
            assertEquals(bicMapper.mapIban(ibanWithId(identifier)), "UNKNOWN")
        }
    }

    @Test
    fun `should work for unnormalized IBAN numbers`() {
        val iban = "Fi42 7991 2345 6789 00"
        assertEquals(bicMapper.mapIban(iban), "HOLVFIHH")
    }
}
