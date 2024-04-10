// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ServiceVoucherValueReportTests {
    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut cancels correctly`() {
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, 500),
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, 500),
                    row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 500),
                    row(child1, unit1, range2, VoucherReportRowType.REFUND, -500),
                    row(child1, unit1, range2, VoucherReportRowType.CORRECTION, 500),
                    row(child2, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child2, unit1, range1, VoucherReportRowType.CORRECTION, 600),
                    row(child2, unit1, range2, VoucherReportRowType.REFUND, -600),
                    row(child2, unit1, range2, VoucherReportRowType.CORRECTION, 500),
                )
                .shuffled()

        assertEquals(
            setOf(
                row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 500),
                row(child2, unit1, range1, VoucherReportRowType.REFUND, -500),
                row(child2, unit1, range1, VoucherReportRowType.CORRECTION, 600),
                row(child2, unit1, range2, VoucherReportRowType.REFUND, -600),
                row(child2, unit1, range2, VoucherReportRowType.CORRECTION, 500),
            ),
            removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet()
        )
    }

    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut cancels when child, unit, range and amounts match`() {
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 500)
                )
                .shuffled()

        assertEquals(emptySet(), removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet())
    }

    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut does not cancel for different child`() {
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child2, unit1, range1, VoucherReportRowType.CORRECTION, 500)
                )
                .shuffled()

        assertEquals(
            input.toSet(),
            removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet()
        )
    }

    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut does not cancel for different unit`() {
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child1, unit2, range1, VoucherReportRowType.CORRECTION, 500)
                )
                .shuffled()

        assertEquals(
            input.toSet(),
            removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet()
        )
    }

    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut does not cancel for different range`() {
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child1, unit1, range2, VoucherReportRowType.CORRECTION, 500)
                )
                .shuffled()

        assertEquals(
            input.toSet(),
            removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet()
        )
    }

    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut does not cancel for different amount`() {
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 501)
                )
                .shuffled()

        assertEquals(
            input.toSet(),
            removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet()
        )
    }

    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut does not cancel for same sign of amount`() {
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, 500),
                    row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 500)
                )
                .shuffled()

        assertEquals(
            input.toSet(),
            removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet()
        )
    }

    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut does not cancel originals`() {
        // not really realistic that same period would have both refund and original
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child1, unit1, range1, VoucherReportRowType.ORIGINAL, 500)
                )
                .shuffled()

        assertEquals(
            input.toSet(),
            removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet()
        )
    }

    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut does not cancel twice #1`() {
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, 500),
                    row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 500),
                )
                .shuffled()

        assertEquals(
            setOf(row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 500)),
            removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet()
        )
    }

    @Test
    fun `removeRefundsAndCorrectionsThatCancelEachOthersOut does not cancel twice #2`() {
        val input =
            listOf(
                    row(child1, unit1, range1, VoucherReportRowType.REFUND, -500),
                    row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 500),
                    row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 500),
                )
                .shuffled()

        assertEquals(
            setOf(row(child1, unit1, range1, VoucherReportRowType.CORRECTION, 500)),
            removeRefundsAndCorrectionsThatCancelEachOthersOut(input).toSet()
        )
    }

    private val child1 = ChildId(UUID.randomUUID())
    private val child2 = ChildId(UUID.randomUUID())
    private val unit1 = DaycareId(UUID.randomUUID())
    private val unit2 = DaycareId(UUID.randomUUID())
    private val range1 = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 15))
    private val range2 = FiniteDateRange(LocalDate.of(2021, 1, 16), LocalDate.of(2021, 1, 31))
    private val areaId = AreaId(UUID.randomUUID())
    private val serviceVoucherDecisionId = VoucherValueDecisionId(UUID.randomUUID())

    private fun row(
        childId: ChildId,
        unitId: DaycareId,
        range: FiniteDateRange,
        type: VoucherReportRowType,
        amount: Int
    ): ServiceVoucherValueRow {
        return ServiceVoucherValueRow(
            childId = childId,
            unitId = unitId,
            realizedPeriod = range,
            type = type,
            realizedAmount = amount,
            // irrelevant fields
            childFirstName = "",
            childLastName = "",
            childDateOfBirth = LocalDate.of(2020, 1, 1),
            childGroupName = null,
            areaId = areaId,
            areaName = "",
            unitName = "",
            serviceVoucherDecisionId = serviceVoucherDecisionId,
            serviceVoucherValue = 0,
            serviceVoucherCoPayment = 0,
            serviceVoucherFinalCoPayment = 0,
            serviceNeedDescription = "",
            assistanceNeedCoefficient = BigDecimal.ZERO,
            numberOfDays = 0,
            isNew = false
        )
    }
}
