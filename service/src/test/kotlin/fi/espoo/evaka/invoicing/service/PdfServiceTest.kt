// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionPartDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.MailAddress
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.Pricing
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.testDecision1
import fi.espoo.evaka.invoicing.testDecisionIncome
import fi.espoo.evaka.shared.config.PDFConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class PdfServiceTest {
    private val service: PDFService = PDFService(PDFConfig.templateEngine())
    private val lang = "fi"

    val testPricing = Pricing(
        multiplier = BigDecimal(0.1070),
        maxThresholdDifference = 269700,
        minThreshold2 = 210200,
        minThreshold3 = 271300,
        minThreshold4 = 308000,
        minThreshold5 = 344700,
        minThreshold6 = 381300,
        thresholdIncrease6Plus = 14200
    )

    private val normalDecision = FeeDecisionDetailed(
        id = testDecision1.id,
        status = testDecision1.status,
        decisionNumber = testDecision1.decisionNumber,
        decisionType = FeeDecisionType.NORMAL,
        validFrom = testDecision1.validFrom,
        validTo = testDecision1.validTo,
        headOfFamily = PersonData.Detailed(
            id = UUID.randomUUID(),
            dateOfBirth = LocalDate.of(1980, 1, 1),
            firstName = "John",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false
        ),
        partner = PersonData.Detailed(
            id = UUID.randomUUID(),
            dateOfBirth = LocalDate.of(1980, 1, 1),
            firstName = "Joan",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false
        ),
        headOfFamilyIncome = testDecisionIncome.copy(total = 214159),
        partnerIncome = testDecisionIncome.copy(total = 413195),
        familySize = 3,
        pricing = testPricing,
        approvedAt = Instant.parse("2019-04-15T10:15:30.00Z"),
        approvedBy = PersonData.WithName(
            UUID.randomUUID(),
            "Pirkko",
            "Päättäjä"
        ),
        parts = testDecision1.parts.map {
            FeeDecisionPartDetailed(
                child = PersonData.Detailed(
                    id = UUID.randomUUID(),
                    dateOfBirth = LocalDate.of(2017, 1, 1),
                    firstName = "Johnny",
                    lastName = "Doe",
                    restrictedDetailsEnabled = false
                ),
                placement = it.placement,
                placementUnit = UnitData.Detailed(
                    id = UUID.randomUUID(),
                    name = "Test Daycare",
                    language = lang,
                    areaId = UUID.randomUUID(),
                    areaName = "Test Area"
                ),
                baseFee = it.baseFee,
                siblingDiscount = it.siblingDiscount,
                fee = it.fee
            )
        }
    )

    private val reliefDecision = normalDecision.copy(decisionType = FeeDecisionType.RELIEF_ACCEPTED)

    @Test
    fun `variables are ok with normal decision`() {
        val feeDecisionPdfData = FeeDecisionPdfData(
            decision = normalDecision,
            lang = lang
        )

        val simpleVariables = service.getFeeDecisionPdfVariables(feeDecisionPdfData).filterKeys {
            it != "parts"
        }

        val expectedSendAddress = MailAddress(
            "Kamreerintie 2",
            "02770",
            "Espoo"
        )

        val expected = mapOf(
            "approvedAt" to "15.04.2019",
            "decisionNumber" to 1010101010L,
            "decisionType" to "NORMAL",
            "isReliefDecision" to false,
            "hasPartner" to true,
            "hasPoBox" to false,
            "headFullName" to "John Doe",
            "headIncomeEffect" to "INCOME",
            "headIncomeTotal" to "2141,59",
            "partnerFullName" to "Joan Doe",
            "partnerIncomeEffect" to "INCOME",
            "partnerIncomeTotal" to "4131,95",
            "sendAddress" to expectedSendAddress,
            "totalFee" to "434,00",
            "validFor" to "01.05.2019 - 31.05.2019",
            "validFrom" to "01.05.2019",
            "totalIncome" to "6273,54",
            "feePercent" to "10,7",
            "familySize" to 3,
            "pricingMinThreshold" to "-2713,00",
            "showValidTo" to true,
            "approverFirstName" to "Pirkko",
            "approverLastName" to "Päättäjä",
            "showTotalIncome" to true
        )
        simpleVariables.forEach { key, item -> assertEquals(expected.getValue(key), item) }
    }

    @Test
    fun `variables are ok with relief decision`() {
        val feeDecisionPdfData = FeeDecisionPdfData(
            decision = reliefDecision,
            lang = lang
        )

        val simpleVariables = service.getFeeDecisionPdfVariables(feeDecisionPdfData).filterKeys {
            it != "parts"
        }

        val expectedSendAddress = MailAddress(
            "Kamreerintie 2",
            "02770",
            "Espoo"
        )

        val expected = mapOf(
            "approvedAt" to "15.04.2019",
            "decisionNumber" to 1010101010L,
            "decisionType" to "RELIEF_ACCEPTED",
            "isReliefDecision" to true,
            "hasPartner" to true,
            "hasPoBox" to false,
            "headFullName" to "John Doe",
            "headIncomeEffect" to "INCOME",
            "headIncomeTotal" to "2141,59",
            "partnerFullName" to "Joan Doe",
            "partnerIncomeEffect" to "INCOME",
            "partnerIncomeTotal" to "4131,95",
            "sendAddress" to expectedSendAddress,
            "totalFee" to "434,00",
            "validFor" to "01.05.2019 - 31.05.2019",
            "validFrom" to "01.05.2019",
            "totalIncome" to "6273,54",
            "feePercent" to "10,7",
            "familySize" to 3,
            "pricingMinThreshold" to "-2713,00",
            "showValidTo" to true,
            "approverFirstName" to "Pirkko",
            "approverLastName" to "Päättäjä",
            "showTotalIncome" to true
        )
        simpleVariables.forEach { key, item -> assertEquals(expected.getValue(key), item) }
    }

    @Test
    fun `generateFeeDecisionPdf smoke test`() {
        val feeDecisionPdfData = FeeDecisionPdfData(
            decision = normalDecision,
            lang = lang
        )
        val pdfBytes = service.generateFeeDecisionPdf(feeDecisionPdfData)

        // File("/tmp/fee_decision_test.pdf").writeBytes(pdfBytes)

        assertNotNull(pdfBytes)
    }
}
