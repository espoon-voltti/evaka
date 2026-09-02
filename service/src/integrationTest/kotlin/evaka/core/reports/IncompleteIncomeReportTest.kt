// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.PureJdbiTest
import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ServiceNeedOption
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.IncomeId
import evaka.core.shared.PlacementId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevFridgePartnership
import evaka.core.shared.dev.DevIncome
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertServiceNeedOption
import evaka.core.snDaycareFullDay35
import evaka.core.snDefaultDaycare
import evaka.core.snDefaultPreschool
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IncompleteIncomeReportTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today = LocalDate.of(2024, 10, 20)
    private val incomeValidFrom = LocalDate.of(2024, 10, 15)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val nonInvoicedDaycare = DevDaycare(areaId = area.id, invoicedByMunicipality = false)
    private val voucherDaycare =
        DevDaycare(
            areaId = area.id,
            invoicedByMunicipality = false,
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
        )
    private val employee = DevEmployee()
    private val headOfChild = DevPerson()
    private val partner = DevPerson()
    private val child = DevPerson(dateOfBirth = LocalDate.of(2020, 1, 1))
    private val snZeroFeeDaycare =
        snDaycareFullDay35.copy(
            id = ServiceNeedOptionId(UUID.randomUUID()),
            nameFi = "Maksuton varhaiskasvatus",
            feeCoefficient = BigDecimal("0.00"),
        )

    private val incompleteIncome =
        DevIncome(
            personId = headOfChild.id,
            effect = IncomeEffect.INCOMPLETE,
            validFrom = incomeValidFrom,
            validTo = null,
            modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(nonInvoicedDaycare)
            tx.insert(voucherDaycare)
            tx.insert(employee)
            tx.insert(headOfChild, DevPersonType.RAW_ROW)
            tx.insert(partner, DevPersonType.RAW_ROW)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertServiceNeedOption(snDefaultDaycare)
            tx.insertServiceNeedOption(snDefaultPreschool)
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insertServiceNeedOption(snZeroFeeDaycare)
            tx.insert(
                DevFridgeChild(
                    childId = child.id,
                    headOfChild = headOfChild.id,
                    startDate = child.dateOfBirth,
                    endDate = child.dateOfBirth.plusYears(18),
                )
            )
        }
    }

    @Test
    fun `head of child and partner with expired income and paid placement are found in report`() {
        db.transaction { tx ->
            tx.insertPartnership()
            tx.insert(incompleteIncome)
            tx.insert(
                incompleteIncome.copy(id = IncomeId(UUID.randomUUID()), personId = partner.id)
            )
            tx.insertPlacement()
        }
        assertEquals(
            setOf(headOfChild.id, partner.id),
            getIncompleteIncomeReport().map { it.personId }.toSet(),
        )
    }

    @Test
    fun `adults whose children do not have any placements are not found in report`() {
        db.transaction { tx -> tx.insert(incompleteIncome) }
        assertEquals(0, getIncompleteIncomeReport().size)
    }

    @Test
    fun `placement without service need is not counted if its default option has no fee`() {
        db.transaction { tx ->
            tx.insert(incompleteIncome)
            tx.insertPlacement(type = PlacementType.PRESCHOOL, option = null)
        }
        assertEquals(0, getIncompleteIncomeReport().size)
    }

    @Test
    fun `placement without service need is counted if its default option has a fee`() {
        db.transaction { tx ->
            tx.insert(incompleteIncome)
            tx.insertPlacement(type = PlacementType.DAYCARE, option = null)
        }
        assertEquals(1, getIncompleteIncomeReport().size)
    }

    @Test
    fun `placement that ended yesterday is not counted`() {
        db.transaction { tx ->
            tx.insert(incompleteIncome)
            tx.insertPlacement(endDate = today.minusDays(1))
        }
        assertEquals(0, getIncompleteIncomeReport().size)
    }

    @Test
    fun `service need with a fee that has ended does not make the placement paid`() {
        db.transaction { tx ->
            tx.insert(incompleteIncome)
            val placementId = tx.insertPlacement(option = null)
            tx.insertServiceNeed(placementId, snDaycareFullDay35, endDate = today.minusDays(1))
            tx.insertServiceNeed(placementId, snZeroFeeDaycare, startDate = today)
        }
        assertEquals(0, getIncompleteIncomeReport().size)
    }

    @Test
    fun `placement in unit not invoiced by municipality is not counted`() {
        db.transaction { tx ->
            tx.insert(incompleteIncome)
            tx.insertPlacement(unitId = nonInvoicedDaycare.id)
        }
        assertEquals(0, getIncompleteIncomeReport().size)
    }

    @Test
    fun `placement in service voucher unit is counted`() {
        db.transaction { tx ->
            tx.insert(incompleteIncome)
            tx.insertPlacement(unitId = voucherDaycare.id)
        }
        assertEquals(1, getIncompleteIncomeReport().size)
    }

    @Test
    fun `after editing expired income by evaka employee, person is not found in report`() {
        db.transaction { tx ->
            tx.insert(
                incompleteIncome.copy(
                    id = IncomeId(UUID.randomUUID()),
                    modifiedBy = employee.evakaUserId,
                )
            )
            tx.insertPlacement()
        }
        assertEquals(0, getIncompleteIncomeReport().size)
    }

    @Test
    fun `head of child without partner is found in report`() {
        db.transaction { tx ->
            tx.insert(incompleteIncome)
            tx.insertPlacement()
        }
        assertEquals(1, getIncompleteIncomeReport().size)
    }

    @Test
    fun `person whose partner has accepted max fee is not found in report`() {
        db.transaction { tx ->
            tx.insertPartnership()
            tx.insert(incompleteIncome)
            tx.insert(
                DevIncome(
                    personId = partner.id,
                    effect = IncomeEffect.MAX_FEE_ACCEPTED,
                    validFrom = today.minusYears(1),
                    validTo = today.minusDays(1),
                    modifiedBy = employee.evakaUserId,
                )
            )
            tx.insertPlacement()
        }
        assertEquals(1, getIncompleteIncomeReport().size)

        db.transaction { tx ->
            tx.insert(
                DevIncome(
                    personId = partner.id,
                    effect = IncomeEffect.MAX_FEE_ACCEPTED,
                    validFrom = today,
                    validTo = null,
                    modifiedBy = employee.evakaUserId,
                )
            )
        }
        assertEquals(0, getIncompleteIncomeReport().size)
    }

    @Test
    fun `partner whose head of child has accepted max fee is not found in report`() {
        db.transaction { tx ->
            tx.insertPartnership()
            tx.insert(
                incompleteIncome.copy(id = IncomeId(UUID.randomUUID()), personId = partner.id)
            )
            tx.insert(
                DevIncome(
                    personId = headOfChild.id,
                    effect = IncomeEffect.MAX_FEE_ACCEPTED,
                    validFrom = today,
                    validTo = null,
                    modifiedBy = employee.evakaUserId,
                )
            )
            tx.insertPlacement()
        }
        assertEquals(0, getIncompleteIncomeReport().size)
    }

    @Test
    fun `income with an end date is not counted`() {
        db.transaction { tx ->
            tx.insert(
                incompleteIncome.copy(
                    id = IncomeId(UUID.randomUUID()),
                    validTo = today.plusMonths(1),
                )
            )
            tx.insertPlacement()
        }
        assertEquals(0, getIncompleteIncomeReport().size)
    }

    @Test
    fun `adult with multiple placed children is reported only once`() {
        val child2 = DevPerson(dateOfBirth = LocalDate.of(2022, 1, 1))
        db.transaction { tx ->
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                DevFridgeChild(
                    childId = child2.id,
                    headOfChild = headOfChild.id,
                    startDate = child2.dateOfBirth,
                    endDate = child2.dateOfBirth.plusYears(18),
                )
            )
            tx.insert(incompleteIncome)
            tx.insertPlacement()
            tx.insertPlacement(childId = child2.id)
        }
        assertEquals(1, getIncompleteIncomeReport().size)
    }

    private fun Database.Transaction.insertPartnership() {
        insert(
            DevFridgePartnership(
                first = headOfChild.id,
                second = partner.id,
                startDate = child.dateOfBirth,
            )
        )
    }

    private fun Database.Transaction.insertPlacement(
        unitId: DaycareId = daycare.id,
        type: PlacementType = PlacementType.DAYCARE,
        option: ServiceNeedOption? = snDaycareFullDay35,
        childId: ChildId = child.id,
        startDate: LocalDate = today.minusMonths(1),
        endDate: LocalDate = today.plusMonths(1),
    ): PlacementId {
        val placementId =
            insert(
                DevPlacement(
                    childId = childId,
                    unitId = unitId,
                    type = type,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        if (option != null) insertServiceNeed(placementId, option, startDate, endDate)
        return placementId
    }

    private fun Database.Transaction.insertServiceNeed(
        placementId: PlacementId,
        option: ServiceNeedOption,
        startDate: LocalDate = today.minusMonths(1),
        endDate: LocalDate = today.plusMonths(1),
    ) {
        insert(
            DevServiceNeed(
                placementId = placementId,
                startDate = startDate,
                endDate = endDate,
                optionId = option.id,
                confirmedBy = employee.evakaUserId,
            )
        )
    }

    private fun getIncompleteIncomeReport(): List<IncompleteIncomeDbRow> = db.read {
        it.getIncompleteReport(today)
    }
}
