// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FinanceDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFeeDecision
import evaka.core.shared.dev.DevFeeDecisionChild
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevVoucherValueDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.MockEvakaClock
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
        val daycare = DevDaycare(areaId = area.id, providerType = ProviderType.MUNICIPAL)
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
                        status = FeeDecisionStatus.SENT,
                    )
                )
            tx.insert(
                DevFeeDecisionChild(
                    feeDecisionId = feeDecisionId,
                    childId = child1,
                    placementUnitId = daycare.id,
                    finalFee = 28800,
                )
            )
            tx.insert(
                DevFeeDecisionChild(
                    feeDecisionId = feeDecisionId,
                    childId = child2,
                    placementUnitId = daycare.id,
                    finalFee = 28800,
                )
            )
            tx.insert(
                DevFeeDecisionChild(
                    feeDecisionId = feeDecisionId,
                    childId = child3,
                    placementUnitId = daycare.id,
                    finalFee = 14400,
                )
            )
        }

        val expected =
            listOf(
                CustomerFeesReport.CustomerFeesReportRow(14400, 1),
                CustomerFeesReport.CustomerFeesReportRow(28800, 2),
            )
        assertEquals(expected, getReport(FinanceDecisionType.FEE_DECISION))
        assertEquals(expected, getReport(FinanceDecisionType.FEE_DECISION, areaId = area.id))
        assertEquals(expected, getReport(FinanceDecisionType.FEE_DECISION, unitId = daycare.id))
        assertEquals(
            expected,
            getReport(FinanceDecisionType.FEE_DECISION, providerType = ProviderType.MUNICIPAL),
        )
        assertEquals(
            expected,
            getReport(FinanceDecisionType.FEE_DECISION, placementType = PlacementType.DAYCARE),
        )
        assertEquals(emptyList(), getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION))
        assertEquals(
            emptyList(),
            getReport(FinanceDecisionType.FEE_DECISION, date = date.minusYears(1)),
        )
        assertEquals(
            emptyList(),
            getReport(FinanceDecisionType.FEE_DECISION, areaId = AreaId(UUID.randomUUID())),
        )
        assertEquals(
            emptyList(),
            getReport(FinanceDecisionType.FEE_DECISION, unitId = DaycareId(UUID.randomUUID())),
        )
        assertEquals(
            emptyList(),
            getReport(FinanceDecisionType.FEE_DECISION, providerType = ProviderType.PRIVATE),
        )
        assertEquals(
            emptyList(),
            getReport(
                FinanceDecisionType.FEE_DECISION,
                placementType = PlacementType.PRESCHOOL_DAYCARE,
            ),
        )
    }

    @Test
    fun `it works with voucher value decisions`() {
        val date = clock.today()
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id, providerType = ProviderType.PRIVATE)
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
                    finalCoPayment = 28800,
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
                    finalCoPayment = 28800,
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
                    finalCoPayment = 14400,
                )
            )
        }

        val expected =
            listOf(
                CustomerFeesReport.CustomerFeesReportRow(14400, 1),
                CustomerFeesReport.CustomerFeesReportRow(28800, 2),
            )
        assertEquals(expected, getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION))
        assertEquals(
            expected,
            getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION, areaId = area.id),
        )
        assertEquals(
            expected,
            getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION, unitId = daycare.id),
        )
        assertEquals(
            expected,
            getReport(
                FinanceDecisionType.VOUCHER_VALUE_DECISION,
                providerType = ProviderType.PRIVATE,
            ),
        )
        assertEquals(
            expected,
            getReport(
                FinanceDecisionType.VOUCHER_VALUE_DECISION,
                placementType = PlacementType.DAYCARE,
            ),
        )
        assertEquals(emptyList(), getReport(FinanceDecisionType.FEE_DECISION))
        assertEquals(
            emptyList(),
            getReport(FinanceDecisionType.VOUCHER_VALUE_DECISION, date = date.minusYears(1)),
        )
        assertEquals(
            emptyList(),
            getReport(
                FinanceDecisionType.VOUCHER_VALUE_DECISION,
                areaId = AreaId(UUID.randomUUID()),
            ),
        )
        assertEquals(
            emptyList(),
            getReport(
                FinanceDecisionType.VOUCHER_VALUE_DECISION,
                unitId = DaycareId(UUID.randomUUID()),
            ),
        )
        assertEquals(
            emptyList(),
            getReport(
                FinanceDecisionType.VOUCHER_VALUE_DECISION,
                providerType = ProviderType.MUNICIPAL,
            ),
        )
        assertEquals(
            emptyList(),
            getReport(
                FinanceDecisionType.VOUCHER_VALUE_DECISION,
                placementType = PlacementType.PRESCHOOL_DAYCARE,
            ),
        )
    }

    private fun getReport(
        decisionType: FinanceDecisionType,
        date: LocalDate = clock.today(),
        areaId: AreaId? = null,
        unitId: DaycareId? = null,
        providerType: ProviderType? = null,
        placementType: PlacementType? = null,
    ) =
        controller
            .getCustomerFeesReport(
                db = dbInstance(),
                user = admin.user,
                clock = clock,
                date = date,
                areaId = areaId,
                unitId = unitId,
                decisionType = decisionType,
                providerType = providerType,
                placementType = placementType,
            )
            .sortedBy { it.feeAmount }
}
