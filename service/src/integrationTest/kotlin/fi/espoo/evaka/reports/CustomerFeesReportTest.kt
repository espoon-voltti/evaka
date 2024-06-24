// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFeeDecision
import fi.espoo.evaka.shared.dev.DevFeeDecisionChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevVoucherValueDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CustomerFeesReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: CustomerFeesReport

    val clock = MockEvakaClock(2024, 3, 1, 15, 0)
    val admin = DevEmployee(roles = setOf(UserRole.ADMIN))

    @BeforeEach
    fun setUp() {
        db.transaction { tx -> tx.insert(admin) }
    }

    @Test
    fun `it works with fee decisions`() {
        val date = clock.today()
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            val headOfFamily = tx.insert(DevPerson(), DevPersonType.ADULT)
            val child1 = tx.insert(DevPerson(), DevPersonType.CHILD)
            val child2 = tx.insert(DevPerson(), DevPersonType.CHILD)
            val child3 = tx.insert(DevPerson(), DevPersonType.CHILD)
            val range = FiniteDateRange(date.minusDays(10), date.plusDays(10))

            val feeDecisionId =
                tx.insert(
                    DevFeeDecision(
                        headOfFamilyId = headOfFamily,
                        validDuring = range,
                        status = FeeDecisionStatus.SENT
                    )
                )
            tx.insert(
                DevFeeDecisionChild(
                    feeDecisionId = feeDecisionId,
                    childId = child1,
                    placementUnitId = daycare.id,
                    finalFee = 28800
                )
            )
            tx.insert(
                DevFeeDecisionChild(
                    feeDecisionId = feeDecisionId,
                    childId = child2,
                    placementUnitId = daycare.id,
                    finalFee = 28800
                )
            )
            tx.insert(
                DevFeeDecisionChild(
                    feeDecisionId = feeDecisionId,
                    childId = child3,
                    placementUnitId = daycare.id,
                    finalFee = 14400
                )
            )
        }

        val expected =
            listOf(
                CustomerFeesReport.CustomerFeesReportRow(14400, 1),
                CustomerFeesReport.CustomerFeesReportRow(28800, 2)
            )
        assertEquals(expected, getReport(FinanceDecisionType.FEE_DECISION))
        assertEquals(expected, getReport(FinanceDecisionType.FEE_DECISION, areaId = area.id))
        assertEquals(expected, getReport(FinanceDecisionType.FEE_DECISION, unitId = daycare.id))
        assertEquals(emptyList(), getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION))
        assertEquals(
            emptyList(),
            getReport(FinanceDecisionType.FEE_DECISION, date = date.minusYears(1))
        )
        assertEquals(
            emptyList(),
            getReport(FinanceDecisionType.FEE_DECISION, areaId = AreaId(UUID.randomUUID()))
        )
        assertEquals(
            emptyList(),
            getReport(FinanceDecisionType.FEE_DECISION, unitId = DaycareId(UUID.randomUUID()))
        )
    }

    @Test
    fun `it works with voucher value decisions`() {
        val date = clock.today()
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            val headOfFamily = tx.insert(DevPerson(), DevPersonType.ADULT)
            val child1 = tx.insert(DevPerson(), DevPersonType.CHILD)
            val child2 = tx.insert(DevPerson(), DevPersonType.CHILD)
            val child3 = tx.insert(DevPerson(), DevPersonType.CHILD)
            val range = FiniteDateRange(date.minusDays(10), date.plusDays(10))

            tx.insert(
                DevVoucherValueDecision(
                    headOfFamilyId = headOfFamily,
                    childId = child1,
                    validFrom = range.start,
                    validTo = range.end,
                    status = VoucherValueDecisionStatus.SENT,
                    placementUnitId = daycare.id,
                    finalCoPayment = 28800
                )
            )
            tx.insert(
                DevVoucherValueDecision(
                    headOfFamilyId = headOfFamily,
                    childId = child2,
                    validFrom = range.start,
                    validTo = range.end,
                    status = VoucherValueDecisionStatus.SENT,
                    placementUnitId = daycare.id,
                    finalCoPayment = 28800
                )
            )
            tx.insert(
                DevVoucherValueDecision(
                    headOfFamilyId = headOfFamily,
                    childId = child3,
                    validFrom = range.start,
                    validTo = range.end,
                    status = VoucherValueDecisionStatus.SENT,
                    placementUnitId = daycare.id,
                    finalCoPayment = 14400
                )
            )
        }

        val expected =
            listOf(
                CustomerFeesReport.CustomerFeesReportRow(14400, 1),
                CustomerFeesReport.CustomerFeesReportRow(28800, 2)
            )
        assertEquals(expected, getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION))
        assertEquals(
            expected,
            getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION, areaId = area.id)
        )
        assertEquals(
            expected,
            getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION, unitId = daycare.id)
        )
        assertEquals(emptyList(), getReport(FinanceDecisionType.FEE_DECISION))
        assertEquals(
            emptyList(),
            getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION, date = date.minusYears(1))
        )
        assertEquals(
            emptyList(),
            getReport(
                FinanceDecisionType.VOUCHER_VALUE_DECISION,
                areaId = AreaId(UUID.randomUUID())
            )
        )
        assertEquals(
            emptyList(),
            getReport(
                FinanceDecisionType.VOUCHER_VALUE_DECISION,
                unitId = DaycareId(UUID.randomUUID())
            )
        )
    }

    private fun getReport(
        decisionType: FinanceDecisionType,
        date: LocalDate = clock.today(),
        areaId: AreaId? = null,
        unitId: DaycareId? = null
    ) = controller.getCustomerFeesReport(
        db = dbInstance(),
        user = admin.user,
        clock = clock,
        date = date,
        areaId = areaId,
        unitId = unitId,
        decisionType = decisionType
    )
}
