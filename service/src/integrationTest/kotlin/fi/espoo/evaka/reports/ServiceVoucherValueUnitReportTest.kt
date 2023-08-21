// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.sendVoucherValueDecisions
import fi.espoo.evaka.invoicing.createVoucherValueDecisionFixture
import fi.espoo.evaka.invoicing.data.annulVoucherValueDecisions
import fi.espoo.evaka.invoicing.data.getValueDecisionsByIds
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionEndDates
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reports.VoucherReportRowType.CORRECTION
import fi.espoo.evaka.reports.VoucherReportRowType.ORIGINAL
import fi.espoo.evaka.reports.VoucherReportRowType.REFUND
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toValueDecisionServiceNeed
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ServiceVoucherValueUnitReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.execute(
                "INSERT INTO holiday (date, description) VALUES (?, ?)",
                janFirst,
                "New Year"
            )
            it.execute(
                "INSERT INTO holiday (date, description) VALUES (?, ?)",
                janFirst.plusDays(5),
                "Epiphany"
            )
        }
    }

    private val janFirst = LocalDate.of(2020, 1, 1)
    private val febFirst = LocalDate.of(2020, 2, 1)
    private val marFirst = LocalDate.of(2020, 3, 1)
    private val aprFirst = LocalDate.of(2020, 4, 1)

    private val janFreeze = HelsinkiDateTime.of(LocalDateTime.of(2020, 1, 21, 22, 0))
    private val febFreeze = HelsinkiDateTime.of(LocalDateTime.of(2020, 2, 21, 22, 0))
    private val marFreeze = HelsinkiDateTime.of(LocalDateTime.of(2020, 3, 21, 22, 0))
    private val aprFreeze = HelsinkiDateTime.of(LocalDateTime.of(2020, 4, 21, 22, 0))

    @Test
    fun `unfrozen service voucher report includes value decisions that begin in the beginning of reports month`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(
            ORIGINAL,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            28800,
            58200
        )
    }

    @Test
    fun `service voucher unit report counts fee alterations correctly`() {
        createVoucherDecision(
            validFrom = janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 28800,
            feeAlterations =
                listOf(
                    FeeAlterationWithEffect(
                        type = FeeAlteration.Type.DISCOUNT,
                        amount = 50,
                        isAbsolute = false,
                        effect = -14400
                    )
                )
        )

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(
            ORIGINAL,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            14400,
            72600
        )
    }

    @Test
    fun `unfrozen service voucher report does not include value decisions to other units`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        val janReport = getUnitReport(testDaycare2.id, janFirst.year, janFirst.monthValue)
        assertEquals(0, janReport.size)
    }

    @Test
    fun `frozen service voucher report includes value decisions that begin in the beginning of reports month`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 28800)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(
            ORIGINAL,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            28800,
            58200
        )
    }

    @Test
    fun `frozen service voucher report keeps previously frozen rows intact`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 10000)

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(ORIGINAL, janFirst, janFirst.toEndOfMonth(), 87000, 0, 87000)
    }

    @Test
    fun `new value decisions appear as corrections in next months report after freezing`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 10000,
            approvedAt = janFreeze.plusSeconds(3600)
        )

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(3, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
        febReport.assertContainsRow(
            CORRECTION,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            10000,
            77000
        )
        febReport.assertContainsRow(
            ORIGINAL,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            10000,
            77000
        )
    }

    @Test
    fun `service voucher report takes operational days into account`() {
        // Jan 2nd is the first operational day of the month so the report should have the
        // decision's whole value
        val janSecond = janFirst.plusDays(1)
        createVoucherDecision(janSecond, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(
            ORIGINAL,
            janSecond,
            janSecond.toEndOfMonth(),
            87000,
            28800,
            58200
        )
    }

    @Test
    fun `part month decisions are multiplied according to their portion of operational days`() {
        // Jan 6th excludes first two operational days of the month, Jan 2nd and 3rd
        val janSixth = janFirst.plusDays(5)
        createVoucherDecision(janSixth, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        // multiplier is 19 / 21
        janReport.assertContainsRow(
            ORIGINAL,
            janSixth,
            janSixth.toEndOfMonth(),
            87000,
            28800,
            52657
        )
    }

    @Test
    fun `if there is a change on part of the month, the whole month gets refunded and corrected`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        val middleOfMonth = janFirst.plusDays(14)
        createVoucherDecision(
            middleOfMonth,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 10000,
            approvedAt = janFreeze.plusSeconds(3600)
        )

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(4, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
        // 8 operational days -> (9/21) * 87000 = ~33143
        febReport.assertContainsRow(
            CORRECTION,
            janFirst,
            middleOfMonth.minusDays(1),
            87000,
            0,
            33143
        )
        // 13 operational days -> (12/21) * 77000 = ~47667
        febReport.assertContainsRow(
            CORRECTION,
            middleOfMonth,
            middleOfMonth.toEndOfMonth(),
            87000,
            10000,
            47667
        )
        febReport.assertContainsRow(
            ORIGINAL,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            10000,
            77000
        )
    }

    @Test
    fun `new value decisions to a different unit cause refunds in the old unit but corrections in the new unit`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        createVoucherDecision(
            janFirst,
            unitId = testDaycare2.id,
            value = 87000,
            coPayment = 10000,
            approvedAt = janFreeze.plusSeconds(3600)
        )

        val febReportInOldUnit = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportInOldUnit.size)
        febReportInOldUnit.assertContainsRow(
            REFUND,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            0,
            -87000
        )

        val febReportInNewUnit = getUnitReport(testDaycare2.id, febFirst.year, febFirst.monthValue)
        assertEquals(2, febReportInNewUnit.size)
        febReportInNewUnit.assertContainsRow(
            CORRECTION,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            10000,
            77000
        )
        febReportInNewUnit.assertContainsRow(
            ORIGINAL,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            10000,
            77000
        )
    }

    @Test
    fun `value decision double corrections refund the latest corrected value`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 10000,
            approvedAt = janFreeze.plusSeconds(3600)
        )
        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }
        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 5000,
            approvedAt = febFreeze.plusSeconds(3600)
        )

        val marchReport = getUnitReport(testDaycare.id, marFirst.year, marFirst.monthValue)
        assertEquals(5, marchReport.size)
        marchReport.assertContainsRow(
            REFUND,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            10000,
            -77000
        )
        marchReport.assertContainsRow(
            CORRECTION,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            5000,
            82000
        )
        marchReport.assertContainsRow(
            REFUND,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            10000,
            -77000
        )
        marchReport.assertContainsRow(
            CORRECTION,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            5000,
            82000
        )
        marchReport.assertContainsRow(
            ORIGINAL,
            marFirst,
            marFirst.toEndOfMonth(),
            87000,
            5000,
            82000
        )
    }

    @Test
    fun `value decision corrections refund multiple months`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }
        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 5000,
            approvedAt = febFreeze.plusSeconds(3600)
        )

        val marchReport = getUnitReport(testDaycare.id, marFirst.year, marFirst.monthValue)
        assertEquals(5, marchReport.size)
        marchReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
        marchReport.assertContainsRow(
            CORRECTION,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            5000,
            82000
        )
        marchReport.assertContainsRow(REFUND, febFirst, febFirst.toEndOfMonth(), 87000, 0, -87000)
        marchReport.assertContainsRow(
            CORRECTION,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            5000,
            82000
        )
        marchReport.assertContainsRow(
            ORIGINAL,
            marFirst,
            marFirst.toEndOfMonth(),
            87000,
            5000,
            82000
        )
    }

    @Test
    fun `decisions not included in previous months report are included as corrections in the next`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(2, febReport.size)
        febReport.assertContainsRow(CORRECTION, janFirst, janFirst.toEndOfMonth(), 87000, 0, 87000)
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 0, 87000)
    }

    @Test
    fun `split old decisions are both refunded`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        val middleOfMonth = janFirst.plusDays(14)
        createVoucherDecision(
            middleOfMonth,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 28800
        )
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 10000,
            approvedAt = janFreeze.plusSeconds(3600)
        )

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(4, febReport.size)
        // 8 operational days -> (9/21) * 87000 = ~33143
        febReport.assertContainsRow(REFUND, janFirst, middleOfMonth.minusDays(1), 87000, 0, -33143)
        // 13 operational days -> (12/21) * 58200 = ~36029
        febReport.assertContainsRow(
            REFUND,
            middleOfMonth,
            middleOfMonth.toEndOfMonth(),
            87000,
            28800,
            -36029
        )
        febReport.assertContainsRow(
            CORRECTION,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            10000,
            77000
        )
        febReport.assertContainsRow(
            ORIGINAL,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            10000,
            77000
        )
    }

    @Test
    fun `split new decisions are both corrected`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 10000)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 0,
            approvedAt = janFreeze.plusSeconds(3600)
        )
        val middleOfMonth = janFirst.plusDays(14)
        createVoucherDecision(
            middleOfMonth,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 28800,
            approvedAt = janFreeze.plusSeconds(3600)
        )

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(4, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 10000, -77000)
        // 8 operational days -> (9/21) * 87000 = ~33143
        febReport.assertContainsRow(
            CORRECTION,
            janFirst,
            middleOfMonth.minusDays(1),
            87000,
            0,
            33143
        )
        // 13 operational days -> (12/21) * 58200 = ~36029
        febReport.assertContainsRow(
            CORRECTION,
            middleOfMonth,
            middleOfMonth.toEndOfMonth(),
            87000,
            28800,
            36029
        )
        febReport.assertContainsRow(
            ORIGINAL,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            28800,
            58200
        )
    }

    @Test
    fun `isNew property is properly deduced for frozen and unfrozen rows`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 10000)

        val janReportBeforeFreeze =
            getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReportBeforeFreeze.size)
        assertTrue(janReportBeforeFreeze.first().isNew)

        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        val janReportAfterFreeze = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReportAfterFreeze.size)
        assertTrue(janReportAfterFreeze.first().isNew)

        val febReportBeforeFreeze =
            getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportBeforeFreeze.size)
        assertFalse(febReportBeforeFreeze.first().isNew)

        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }
        val febReportAfterFreeze = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportAfterFreeze.size)
        assertFalse(febReportAfterFreeze.first().isNew)

        val janReportAfterFebFreeze =
            getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReportAfterFebFreeze.size)
        assertTrue(janReportAfterFebFreeze.first().isNew)
    }

    @Test
    fun `future service voucher report does not include unfrozen months as corrections`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 28800)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }

        val marchReport = getUnitReport(testDaycare.id, marFirst.year, marFirst.monthValue)
        assertEquals(1, marchReport.size)
        marchReport.assertContainsRow(
            ORIGINAL,
            marFirst,
            marFirst.toEndOfMonth(),
            87000,
            28800,
            58200
        )
    }

    @Test
    fun `annulled decisions are refunded`() {
        val decision =
            createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        db.transaction {
            it.annulVoucherValueDecisions(listOf(decision.id), janFreeze.plusSeconds(3600))
        }

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
    }

    @Test
    fun `annulled decisions are not refunded twice`() {
        val decision =
            createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        db.transaction {
            it.annulVoucherValueDecisions(listOf(decision.id), janFreeze.plusSeconds(3600))
        }
        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)

        val marReport = getUnitReport(testDaycare.id, marFirst.year, marFirst.monthValue)
        assertEquals(0, marReport.size)
    }

    @Test
    fun `multiple decision annulments are refunded only once`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 1000,
            janFreeze.plusSeconds(3600)
        )
        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 2000,
            janFreeze.plusSeconds(7200)
        )

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(3, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
        febReport.assertContainsRow(
            CORRECTION,
            janFirst,
            janFirst.toEndOfMonth(),
            87000,
            2000,
            85000
        )
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 2000, 85000)
    }

    @Test
    fun `multiple decision replacements are refunded only once`() {
        createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 1000,
            janFreeze.plusSeconds(3600)
        )
        val middleOfMonth = janFirst.plusDays(14)
        createVoucherDecision(
            middleOfMonth,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 2000,
            janFreeze.plusSeconds(3600)
        )

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(4, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
        // 8 operational days -> (8/21) * 86000 = ~32762
        febReport.assertContainsRow(
            CORRECTION,
            janFirst,
            middleOfMonth.minusDays(1),
            87000,
            1000,
            32762
        )
        // 13 operational days -> (13/21) * 85000 = ~52619
        febReport.assertContainsRow(
            CORRECTION,
            middleOfMonth,
            janFirst.toEndOfMonth(),
            87000,
            2000,
            52619
        )
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 2000, 85000)
    }

    @Test
    fun `ended value decisions are refunded next month`() {
        val decision =
            createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        val endedDecision = decision.copy(validTo = janFirst.plusDays(13))
        db.transaction {
            it.updateVoucherValueDecisionEndDates(
                listOf(endedDecision),
                janFreeze.plusSeconds(3600)
            )
        }

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(2, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
        // 8 operational days -> (8/21) * 87000 = ~33143
        febReport.assertContainsRow(CORRECTION, janFirst, janFirst.plusDays(13), 87000, 0, 33143)

        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }
        val marReport = getUnitReport(testDaycare.id, marFirst.year, marFirst.monthValue)
        assertEquals(0, marReport.size)
    }

    @Test
    fun `continued value decisions are corrected after original end`() {
        val decision =
            createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            it.updateVoucherValueDecisionEndDates(
                listOf(decision.copy(validTo = janFirst.plusDays(13))),
                janFreeze.plusSeconds(3600)
            )
        }
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(ORIGINAL, janFirst, janFirst.plusDays(13), 87000, 0, 33143)

        // move decision end date to end of february
        db.transaction {
            it.updateVoucherValueDecisionEndDates(
                listOf(decision.copy(validTo = marFirst.toEndOfMonth())),
                janFreeze.plusSeconds(3600)
            )
        }

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(3, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.plusDays(13), 87000, 0, -33143)
        febReport.assertContainsRow(CORRECTION, janFirst, janFirst.toEndOfMonth(), 87000, 0, 87000)
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 0, 87000)

        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }
        val marReport = getUnitReport(testDaycare.id, marFirst.year, marFirst.monthValue)
        assertEquals(1, marReport.size)
        marReport.assertContainsRow(ORIGINAL, marFirst, marFirst.toEndOfMonth(), 87000, 0, 87000)
    }

    @Test
    fun `when one of part month decisions is annulled it is refunded correctly`() {
        val decision =
            createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        val middleOfMonth = janFirst.plusDays(14)
        createVoucherDecision(
            middleOfMonth,
            unitId = testDaycare.id,
            value = 87000,
            coPayment = 10000
        )
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(2, janReport.size)
        // 8 operational days -> (9/21) * 87000 = ~33143
        janReport.assertContainsRow(ORIGINAL, janFirst, middleOfMonth.minusDays(1), 87000, 0, 33143)
        // 13 operational days -> (12/21) * 77000 = ~47667
        janReport.assertContainsRow(
            ORIGINAL,
            middleOfMonth,
            middleOfMonth.toEndOfMonth(),
            87000,
            10000,
            47667
        )

        db.transaction {
            it.annulVoucherValueDecisions(listOf(decision.id), janFreeze.plusSeconds(3600))
        }
        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(2, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, middleOfMonth.minusDays(1), 87000, 0, -33143)
        febReport.assertContainsRow(
            ORIGINAL,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            10000,
            77000
        )
    }

    @Test
    fun `when a continued decision is ended and new one created retroactively, the refund and correction are correct`() {
        val decision =
            createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }

        val endedDecision = decision.copy(validTo = janFirst.toEndOfMonth())
        db.transaction {
            it.updateVoucherValueDecisionEndDates(listOf(endedDecision), febFreeze.plusDays(1))
        }

        createVoucherDecision(febFirst, unitId = testDaycare.id, value = 87000, coPayment = 28800)
        db.transaction {
            freezeVoucherValueReportRows(it, marFirst.year, marFirst.monthValue, marFreeze)
        }

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(ORIGINAL, janFirst, janFirst.toEndOfMonth(), 87000, 0, 87000)

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReport.size)
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 0, 87000)

        val marReport = getUnitReport(testDaycare.id, marFirst.year, marFirst.monthValue)
        assertEquals(3, marReport.size)
        marReport.assertContainsRow(REFUND, febFirst, febFirst.toEndOfMonth(), 87000, 0, -87000)
        marReport.assertContainsRow(
            CORRECTION,
            febFirst,
            febFirst.toEndOfMonth(),
            87000,
            28800,
            58200
        )
        marReport.assertContainsRow(
            ORIGINAL,
            marFirst,
            marFirst.toEndOfMonth(),
            87000,
            28800,
            58200
        )
    }

    @Test
    fun `when a decision is first reduced in validity and annulled in a later month the refunds and corrections are correct`() {
        val decision =
            createVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }

        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }

        val endedDecision = decision.copy(validTo = janFirst.toEndOfMonth())
        db.transaction {
            it.updateVoucherValueDecisionEndDates(listOf(endedDecision), febFreeze.plusWeeks(2))
        }

        createVoucherDecision(febFirst, unitId = testDaycare.id, value = 86000, coPayment = 0)

        db.transaction {
            freezeVoucherValueReportRows(it, marFirst.year, marFirst.monthValue, marFreeze)
        }

        db.transaction { it.annulVoucherValueDecisions(listOf(decision.id), marFreeze.plusDays(7)) }

        createVoucherDecision(
            janFirst,
            unitId = testDaycare.id,
            value = 85000,
            coPayment = 0,
            validTo = janFirst.toEndOfMonth()
        )

        db.transaction {
            freezeVoucherValueReportRows(it, aprFirst.year, aprFirst.monthValue, aprFreeze)
        }

        val aprReport = getUnitReport(testDaycare.id, aprFirst.year, aprFirst.monthValue)

        assertEquals(3, aprReport.size)
        aprReport.assertContainsRow(ORIGINAL, aprFirst, aprFirst.toEndOfMonth(), 86000, 0, 86000)
        aprReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
        aprReport.assertContainsRow(CORRECTION, janFirst, janFirst.toEndOfMonth(), 85000, 0, 85000)
    }

    private fun List<ServiceVoucherValueRow>.assertContainsRow(
        type: VoucherReportRowType,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        value: Int,
        finalCoPayment: Int,
        realizedValue: Int
    ) {
        val row =
            this.find {
                it.type == type &&
                    it.realizedPeriod.start == periodStart &&
                    it.realizedPeriod.end == periodEnd
            }
        assertNotNull(row)
        assertEquals(value, row.serviceVoucherValue)
        assertEquals(finalCoPayment, row.serviceVoucherFinalCoPayment)
        assertEquals(realizedValue, row.realizedAmount)
    }

    private fun LocalDate.toEndOfMonth() = this.plusMonths(1).withDayOfMonth(1).minusDays(1)

    private val adminUser =
        AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.ADMIN))

    private fun getUnitReport(
        unitId: DaycareId,
        year: Int,
        month: Int
    ): List<ServiceVoucherValueRow> {
        val (_, response, data) =
            http
                .get(
                    "/reports/service-voucher-value/units/$unitId",
                    listOf("year" to year, "month" to month)
                )
                .asUser(adminUser)
                .responseObject<ServiceVoucherValueReportController.ServiceVoucherUnitReport>(
                    jsonMapper
                )
        assertEquals(200, response.statusCode)

        return data.get().rows
    }

    private val financeUser =
        AuthenticatedUser.Employee(
            id = testDecisionMaker_1.id,
            roles = setOf(UserRole.FINANCE_ADMIN)
        )

    private fun createVoucherDecision(
        validFrom: LocalDate,
        unitId: DaycareId,
        value: Int,
        coPayment: Int,
        approvedAt: HelsinkiDateTime = HelsinkiDateTime.of(validFrom, LocalTime.of(15, 0)),
        alwaysUseDaycareFinanceDecisionHandler: Boolean = false,
        feeAlterations: List<FeeAlterationWithEffect> = listOf(),
        validTo: LocalDate? = null
    ): VoucherValueDecision {
        val id =
            db.transaction {
                val decision =
                    createVoucherValueDecisionFixture(
                        status = VoucherValueDecisionStatus.DRAFT,
                        validFrom = validFrom,
                        validTo = validTo,
                        headOfFamilyId = testAdult_1.id,
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        unitId = unitId,
                        value = value,
                        coPayment = coPayment,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
                        feeAlterations = feeAlterations
                    )
                it.upsertValueDecisions(listOf(decision))

                sendVoucherValueDecisions(
                    tx = it,
                    asyncJobRunner = asyncJobRunner,
                    user = financeUser,
                    evakaEnv = evakaEnv,
                    now = approvedAt,
                    ids = listOf(decision.id),
                    decisionHandlerId = null,
                    alwaysUseDaycareFinanceDecisionHandler
                )
                decision.id
            }

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        return db.read { it.getValueDecisionsByIds(listOf(id)).first() }
    }
}
