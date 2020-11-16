// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.Absence
import fi.espoo.evaka.daycare.service.AbsenceService
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createFeeDecisionAlterationFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.createFeeDecisionPartFixture
import fi.espoo.evaka.invoicing.data.flatten
import fi.espoo.evaka.invoicing.data.invoiceQueryBase
import fi.espoo.evaka.invoicing.data.toInvoice
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.Product
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class InvoiceGeneratorIntegrationTest : FullApplicationTest() {
    @Autowired
    private lateinit var absenceService: AbsenceService

    private val areaCode = 200
    private val costCenter = "31500"
    private val subCostCenter = "00"

    @BeforeEach
    fun beforeEach() {
        jdbi.handle(::insertGeneralTestFixtures)
        jdbi.handle { h ->
            h.createUpdate("INSERT INTO holiday (date) VALUES (:date1), (:date2)")
                .bind("date1", LocalDate.of(2019, 1, 1))
                .bind("date2", LocalDate.of(2019, 1, 6))
                .execute()
        }
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    @Test
    fun `invoice generation for child with a day long temporary placement`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = Period(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        jdbi.handle(insertPlacement(testChild_1.id, placementPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, period))

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(areaCode, invoice.agreementType)
            assertEquals(2900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(costCenter, invoiceRow.costCenter)
                assertEquals(subCostCenter, invoiceRow.subCostCenter)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for child with one day long part day temporary placement`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = Period(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        jdbi.handle(insertPlacement(testChild_1.id, placementPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, period, partDay = true))

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(1500, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for child with a three day long temporary placement`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = Period(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
        jdbi.handle(insertPlacement(testChild_1.id, placementPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, period))

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(8700, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(8700, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for child with a two day long temporary placement and a day long part day temporary placement`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = Period(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
        jdbi.handle(insertPlacement(testChild_1.id, placementPeriod))
        jdbi.handle(
            insertTemporaryServiceNeed(
                testChild_1.id,
                placementPeriod.copy(end = placementPeriod.end!!.minusDays(1))
            )
        )
        jdbi.handle(
            insertTemporaryServiceNeed(
                testChild_1.id,
                placementPeriod.copy(start = placementPeriod.end!!),
                partDay = true
            )
        )

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(7300, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(5800, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for two children with temporary placements at the same time`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = Period(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        jdbi.handle(insertPlacement(testChild_1.id, placementPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, period))

        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        jdbi.handle(insertPlacement(testChild_2.id, placementPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_2.id, period))

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(4400, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for two children with part day temporary placements at the same time`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = Period(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        jdbi.handle(insertPlacement(testChild_1.id, placementPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, period, partDay = true))

        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        jdbi.handle(insertPlacement(testChild_2.id, placementPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_2.id, period, partDay = true))

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(2300, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(800, invoiceRow.unitPrice)
                assertEquals(800, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for child with a two day long temporary placement that changes head of family during placement`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        val placementPeriod = Period(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 3))
        jdbi.handle(insertPlacement(testChild_1.id, placementPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, period))

        jdbi.handle(
            insertChildParentRelation(
                testAdult_1.id,
                testChild_1.id,
                placementPeriod.copy(end = placementPeriod.start)
            )
        )
        jdbi.handle(
            insertChildParentRelation(
                testAdult_2.id,
                testChild_1.id,
                placementPeriod.copy(start = placementPeriod.end!!)
            )
        )

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(2, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(2900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
            }
        }
        result.last().let { invoice ->
            assertEquals(testAdult_2.id, invoice.headOfFamily.id)
            assertEquals(2900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for child with a day long temporary placement that has no family configured`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        val placementPeriod = Period(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        jdbi.handle(insertPlacement(testChild_1.id, placementPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, period))

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `invoice generation for temporary placements does not pick non-temporary placements`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val temporaryPeriod = Period(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 3))
        jdbi.handle(insertPlacement(testChild_1.id, temporaryPeriod))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, temporaryPeriod))
        val nonTemporaryPeriod = Period(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 5))
        jdbi.handle(insertPlacement(testChild_1.id, nonTemporaryPeriod))
        jdbi.handle { h ->
            insertTestServiceNeed(
                h,
                testChild_1.id,
                startDate = nonTemporaryPeriod.start,
                endDate = nonTemporaryPeriod.end!!,
                temporary = false,
                updatedBy = testDecisionMaker_1.id
            )
        }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(5800, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(5800, invoiceRow.price())
                assertEquals(temporaryPeriod.start, invoiceRow.periodStart)
                assertEquals(temporaryPeriod.end, invoiceRow.periodEnd)
            }
        }
    }

    @Test
    fun `invoice generation for temporary placements where service needs and placements cross each other`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementStart = LocalDate.of(2019, 1, 2)
        jdbi.handle(insertPlacement(testChild_1.id, Period(placementStart, placementStart.plusDays(1))))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, Period(placementStart, placementStart)))
        jdbi.handle(insertPlacement(testChild_1.id, Period(placementStart.plusDays(2), placementStart.plusDays(2))))
        jdbi.handle(
            insertTemporaryServiceNeed(
                testChild_1.id,
                Period(placementStart.plusDays(1), placementStart.plusDays(2)),
                true
            )
        )

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(5900, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
                assertEquals(placementStart, invoiceRow.periodStart)
                assertEquals(placementStart, invoiceRow.periodEnd)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(3000, invoiceRow.price())
                assertEquals(placementStart.plusDays(1), invoiceRow.periodStart)
                assertEquals(placementStart.plusDays(2), invoiceRow.periodEnd)
            }
        }
    }

    @Test
    fun `invoice generation for temporary placements where service needs are separate and over a placement`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        jdbi.handle(insertPlacement(testChild_1.id, period))
        val serviceNeedDate_1 = LocalDate.of(2019, 1, 7)
        val serviceNeedDate_2 = serviceNeedDate_1.plusWeeks(1)
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, Period(serviceNeedDate_1, serviceNeedDate_1)))
        jdbi.handle(insertTemporaryServiceNeed(testChild_1.id, Period(serviceNeedDate_2, serviceNeedDate_2)))

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(5800, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
                assertEquals(serviceNeedDate_1, invoiceRow.periodStart)
                assertEquals(serviceNeedDate_1, invoiceRow.periodEnd)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
                assertEquals(serviceNeedDate_2, invoiceRow.periodStart)
                assertEquals(serviceNeedDate_2, invoiceRow.periodEnd)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with same price results in one invoice row`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900
                )
            )
        )
        jdbi.handle { h ->
            insertDecisionsAndPlacements(
                h,
                listOf(
                    decision.copy(validTo = period.start.plusDays(7)),
                    decision.copy(id = UUID.randomUUID(), validFrom = period.start.plusDays(8))
                )
            )
        }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with same price and same fee alterations results in one invoice row`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf(
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.DISCOUNT,
                            amount = 20,
                            isAbsolute = false,
                            effect = -5780
                        )
                    )
                )
            )
        )
        jdbi.handle { h ->
            insertDecisionsAndPlacements(
                h,
                listOf(
                    decision.copy(validTo = period.start.plusDays(7)),
                    decision.copy(id = UUID.randomUUID(), validFrom = period.start.plusDays(8))
                )
            )
        }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(23120, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with the second one having another child results in one invoice row for the first child`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900
                )
            )
        )
        jdbi.handle { h ->
            insertDecisionsAndPlacements(
                h,
                listOf(
                    decision.copy(validTo = period.start.plusDays(7)),
                    decision.copy(
                        id = UUID.randomUUID(),
                        validFrom = period.start.plusDays(8),
                        familySize = decision.familySize + 1,
                        parts = decision.parts + createFeeDecisionPartFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            daycareId = testDaycare.id,
                            baseFee = 28900,
                            siblingDiscount = 50,
                            fee = 14500
                        )
                    )
                )
            )
        }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(40103, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(17, invoiceRow.amount)
                assertEquals(659, invoiceRow.unitPrice)
                assertEquals(11203, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions makes sure the sum is at most the monthly fee`() {
        // January 2019 has 22 operational days which results in daily price being rounded up
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(end = period.start.plusDays(6)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionPartFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        daycareId = testDaycare.id,
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            ),
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(start = period.start.plusDays(7)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionPartFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        daycareId = testDaycare2.id,
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            )
        )
        jdbi.handle { h -> insertDecisionsAndPlacements(h, decisions) }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(4, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(5256, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(18, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(23652, invoiceRow.price())
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-8, invoiceRow.unitPrice)
                assertEquals(-8, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions makes sure the sum is at least the monthly fee`() {
        // March 2019 has 21 operational days which results in daily price being rounded down
        val period = Period(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 3, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(end = period.start.plusDays(6)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionPartFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        daycareId = testDaycare.id,
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            ),
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(start = period.start.plusDays(7)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionPartFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        daycareId = testDaycare2.id,
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            )
        )
        jdbi.handle { h -> insertDecisionsAndPlacements(h, decisions) }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(1376, invoiceRow.unitPrice)
                assertEquals(6880, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(16, invoiceRow.amount)
                assertEquals(1376, invoiceRow.unitPrice)
                assertEquals(22016, invoiceRow.price())
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(4, invoiceRow.unitPrice)
                assertEquals(4, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions with cleanly split daily prices does not result in a rounding row`() {
        // February 2019 has 20 operational days which results in daily price being split evenly
        val period = Period(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(end = period.start.plusDays(6)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionPartFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        daycareId = testDaycare.id,
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            ),
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(start = period.start.plusDays(7)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionPartFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        daycareId = testDaycare2.id,
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            )
        )
        jdbi.handle { h -> insertDecisionsAndPlacements(h, decisions) }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(1445, invoiceRow.unitPrice)
                assertEquals(7225, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(15, invoiceRow.amount)
                assertEquals(1445, invoiceRow.unitPrice)
                assertEquals(21675, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with the second one having changed fee for second child results in one invoice row for the first child`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900
                ),
                createFeeDecisionPartFixture(
                    childId = testChild_2.id,
                    dateOfBirth = testChild_2.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    siblingDiscount = 50,
                    fee = 14500
                )
            )
        )
        jdbi.handle { h ->
            insertDecisionsAndPlacements(
                h,
                listOf(
                    decision.copy(validTo = period.start.plusDays(7)),
                    decision.copy(
                        id = UUID.randomUUID(),
                        validFrom = period.start.plusDays(8),
                        parts = listOf(decision.parts[0], decision.parts[1].copy(fee = 10000))
                    )
                )
            )
        }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(39930, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(659, invoiceRow.unitPrice)
                assertEquals(3295, invoiceRow.price())
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(17, invoiceRow.amount)
                assertEquals(455, invoiceRow.unitPrice)
                assertEquals(7735, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation from fee decision with one fee alteration creates additional invoice row`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf(
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.DISCOUNT,
                            amount = 20,
                            isAbsolute = false,
                            effect = -5780
                        )
                    )
                )
            )
        )
        jdbi.handle { h -> insertDecisionsAndPlacements(h, listOf(decision)) }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(23120, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation from fee decision with multiple fee alterations creates additional invoice rows`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf(
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.DISCOUNT,
                            amount = 20,
                            isAbsolute = false,
                            effect = -5780
                        ),
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.INCREASE,
                            amount = 93,
                            isAbsolute = false,
                            effect = 9300
                        )
                    )
                )
            )
        )
        jdbi.handle { h -> insertDecisionsAndPlacements(h, listOf(decision)) }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(32420, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price())
            }

            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_INCREASE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(9300, invoiceRow.unitPrice)
                assertEquals(9300, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation from fee decision with a 95% discount fee alteration`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf(
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.DISCOUNT,
                            amount = 95,
                            isAbsolute = false,
                            effect = -27455
                        )
                    )
                )
            )
        )
        jdbi.handle { h -> insertDecisionsAndPlacements(h, listOf(decision)) }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(1445, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-27455, invoiceRow.unitPrice)
                assertEquals(-27455, invoiceRow.price())
            }
        }
    }

    @Test
    fun `when two people have active fee decisions for the same child only one of them is invoiced`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        jdbi.handle { h ->
            insertTestPlacement(
                h,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = period.start,
                endDate = period.end!!
            )
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(decision, decision.copy(id = UUID.randomUUID(), headOfFamily = PersonData.JustId(testAdult_2.id)))
            )
        }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `when a placement ends before the fee decision only the placement period is invoiced`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        val placementPeriod = period.copy(end = period.start.plusDays(7))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        jdbi.handle { h ->
            insertTestPlacement(
                h,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementPeriod.start,
                endDate = placementPeriod.end!!
            )
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(decision, decision.copy(id = UUID.randomUUID(), headOfFamily = PersonData.JustId(testAdult_2.id)))
            )
        }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(6570, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(placementPeriod.start, invoiceRow.periodStart)
                assertEquals(placementPeriod.end, invoiceRow.periodEnd)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(6570, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with sick leave absences covering period`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= period.end }
            .map { it to AbsenceType.SICKLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_100, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with a lot sick leave absences`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= period.end }
            .filter { it.dayOfWeek != DayOfWeek.MONDAY }
            .map { it to AbsenceType.SICKLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_50, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with some unknown absences`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= period.end }
            .filter { it.dayOfWeek != DayOfWeek.MONDAY }
            .map { it to AbsenceType.UNKNOWN_ABSENCE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with unknown absences covering period`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= period.end }
            .map { it to AbsenceType.UNKNOWN_ABSENCE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.ABSENCE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave absences`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 4) }
            .map { it to AbsenceType.PARENTLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(24958, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice)
                assertEquals(-3942, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave and sickleave absences`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 4) }
            .map { it to AbsenceType.PARENTLEAVE }
            .plus(
                generateSequence(LocalDate.of(2019, 1, 10)) { it.plusDays(1) }
                    .takeWhile { it <= LocalDate.of(2019, 1, 31) }
                    .map { it to AbsenceType.SICKLEAVE }
            )
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(12479, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice)
                assertEquals(-3942, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_50, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-12479, invoiceRow.unitPrice)
                assertEquals(-12479, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave absences for a too old child`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 4) }
            .map { it to AbsenceType.PARENTLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays, child = testChild_2)

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with 2 decisions plus sick leave absences`() {
        val periods = listOf(
            Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 11)),
            Period(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 31))
        )

        val absenceDays = generateSequence(LocalDate.of(2019, 1, 1)) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 31) }
            .filter { it.dayOfWeek != DayOfWeek.MONDAY }
            .map { it to AbsenceType.SICKLEAVE }
            .toMap()

        initDataForAbsences(periods, absenceDays)

        jdbi.transaction {
            createAllDraftInvoices(
                it,
                objectMapper,
                Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
            )
        }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(11262, invoice.totalPrice())
            assertEquals(5, invoice.rows.size)

            invoice.rows[0].let { invoiceRow ->
                assertEquals(8, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(10512, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_50, invoiceRow.product)
                assertEquals(8, invoiceRow.amount)
                assertEquals(-657, invoiceRow.unitPrice)
                assertEquals(-5256, invoiceRow.price())
            }

            invoice.rows[2].let { invoiceRow ->
                assertEquals(14, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(18396, invoiceRow.price())
            }
            invoice.rows[3].let { invoiceRow ->
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(14, invoiceRow.amount)
                assertEquals(-455, invoiceRow.unitPrice)
                assertEquals(-6370, invoiceRow.price())
            }
            invoice.rows[4].let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_50, invoiceRow.product)
                assertEquals(14, invoiceRow.amount)
                assertEquals(-430, invoiceRow.unitPrice)
                assertEquals(-6020, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with 2 decisions plus parent leave and force majeure absences`() {
        val periods = listOf(
            Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 11)),
            Period(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 31))
        )

        val absenceDays = mapOf(
            LocalDate.of(2019, 1, 7) to AbsenceType.PARENTLEAVE,
            LocalDate.of(2019, 1, 14) to AbsenceType.PARENTLEAVE,
            LocalDate.of(2019, 1, 21) to AbsenceType.FORCE_MAJEURE,
            LocalDate.of(2019, 1, 28) to AbsenceType.FORCE_MAJEURE
        )

        initDataForAbsences(periods, absenceDays)

        jdbi.transaction {
            createAllDraftInvoices(
                it,
                objectMapper,
                Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
            )
        }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(18647, invoice.totalPrice())
            assertEquals(5, invoice.rows.size)

            invoice.rows[0].let { invoiceRow ->
                assertEquals(8, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(10512, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice)
                assertEquals(-1314, invoiceRow.price())
            }

            invoice.rows[2].let { invoiceRow ->
                assertEquals(14, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(18396, invoiceRow.price())
            }
            invoice.rows[3].let { invoiceRow ->
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(14, invoiceRow.amount)
                assertEquals(-455, invoiceRow.unitPrice)
                assertEquals(-6370, invoiceRow.price())
            }
            invoice.rows[4].let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-859, invoiceRow.unitPrice)
                assertEquals(-2577, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with full month of force majeure absences`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 31) }
            .map { it to AbsenceType.FORCE_MAJEURE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation when fee decision is valid only during weekend`() {
        val period = Period(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31))
        val weekEnd = Period(LocalDate.of(2020, 5, 2), LocalDate.of(2020, 5, 3))
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            weekEnd,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        jdbi.handle { h -> insertDecisionsAndPlacements(h, listOf(decision)) }

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `invoice generation when fee decision is valid only during weekend and there are absences for the whole month`() {
        val period = Period(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31))
        val weekEnd = Period(LocalDate.of(2020, 5, 2), LocalDate.of(2020, 5, 3))
        val absenceDays = generateSequence(LocalDate.of(2020, 5, 1)) { date -> date.plusDays(1) }
            .takeWhile { date -> date < LocalDate.of(2020, 6, 1) }
            .map { date -> date to AbsenceType.SICKLEAVE }
            .toMap()
        initDataForAbsences(listOf(weekEnd), absenceDays)

        jdbi.transaction { createAllDraftInvoices(it, objectMapper, period) }

        val result = jdbi.handle(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed every month since Sep 2019 but not april or may`() {
        initFreeJulyTestData(
            Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                Period(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                Period(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                Period(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                Period(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                Period(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                Period(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29)),
                Period(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                Period(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31))
            ),
            PlacementType.DAYCARE_PART_TIME
        )
        val result = jdbi.handle(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `no free july 2021 if child has been placed every month since Sep 2019 but not april or may`() {
        initFreeJulyTestData(
            Period(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31)),
            listOf(
                Period(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 30)),
                Period(LocalDate.of(2020, 10, 1), LocalDate.of(2020, 10, 31)),
                Period(LocalDate.of(2020, 11, 1), LocalDate.of(2020, 11, 30)),
                Period(LocalDate.of(2020, 12, 1), LocalDate.of(2020, 12, 31)),
                Period(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31)),
                Period(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 28)),
                Period(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 31)),
                Period(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 6, 30)),
                Period(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31))
            )
        )
        val result = jdbi.handle(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed every month since Sep 2019, also in april or may`() {
        initFreeJulyTestData(
            Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                Period(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                Period(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                Period(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                Period(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                Period(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                Period(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29)),
                Period(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                Period(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30)),
                Period(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)),
                Period(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31))
            )
        )
        val result = jdbi.handle(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed even for one day every month since Sep 2019`() {
        initFreeJulyTestData(
            Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                Period(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 1)),
                Period(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 1)),
                Period(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 1)),
                Period(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 1)),
                Period(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
                Period(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 1)),
                Period(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 1)),
                Period(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 1)),
                Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 1))
            )
        )
        val result = jdbi.handle(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed all the time`() {
        initFreeJulyTestData(
            Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(Period(LocalDate.of(2018, 7, 1), LocalDate.of(2021, 7, 31)))
        )
        val result = jdbi.handle(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2021 if child has been placed all the time`() {
        initFreeJulyTestData(
            Period(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31)),
            listOf(Period(LocalDate.of(2018, 7, 1), LocalDate.of(2021, 7, 31)))
        )
        val result = jdbi.handle(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `no free july 2020 if even one mandatory month has no placement`() {
        initFreeJulyTestData(
            Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                Period(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                Period(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                Period(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                Period(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                Period(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 28)),
                Period(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                Period(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31))
            )
        )

        val result = jdbi.handle(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `free july 2020 applies only on july`() {
        initFreeJulyTestData(
            Period(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
            listOf(
                Period(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                Period(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                Period(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                Period(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                Period(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                Period(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 28)),
                Period(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                Period(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                Period(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31))
            )
        )
        val result = jdbi.handle(getAllInvoices)
        assertEquals(1, result.size)
    }

    private fun initFreeJulyTestData(invoicingPeriod: Period, placementPeriods: List<Period>, placementType: PlacementType = PlacementType.DAYCARE) {
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            invoicingPeriod,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id,
                    baseFee = 28900,
                    fee = 28900
                )
            )
        )
        jdbi.handle(insertChildParentRelation(testAdult_1.id, testChild_1.id, invoicingPeriod))
        jdbi.handle { h ->
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(
                    decision.copy(validTo = invoicingPeriod.start.plusDays(7))
                )
            )
        }

        placementPeriods.forEach { period -> jdbi.handle(insertPlacement(testChild_1.id, period, placementType)) }
        jdbi.transaction { createAllDraftInvoices(it, objectMapper, invoicingPeriod) }
    }

    private fun initDataForAbsences(
        periods: List<Period>,
        absenceDays: Map<LocalDate, AbsenceType>,
        child: PersonData.Detailed = testChild_1
    ) =
        periods.forEachIndexed { index, period ->
            val decision = createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period,
                testAdult_1.id,
                listOf(
                    createFeeDecisionPartFixture(
                        childId = child.id,
                        dateOfBirth = child.dateOfBirth,
                        daycareId = testDaycare.id,
                        baseFee = 28900,
                        feeAlterations = if (index > 0) {
                            listOf(
                                createFeeDecisionAlterationFixture(
                                    amount = -100,
                                    isAbsolute = true,
                                    effect = -10000
                                )
                            )
                        } else listOf()
                    )
                )
            )

            jdbi.handle(insertChildParentRelation(testAdult_1.id, child.id, period))
            jdbi.handle { h -> upsertFeeDecisions(h, objectMapper, listOf(decision)) }

            val placementId = jdbi.handle(insertPlacement(child.id, period))
            val groupId = jdbi.handle { it.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id)) }
            jdbi.handle { h ->
                insertTestDaycareGroupPlacement(
                    h,
                    daycarePlacementId = placementId,
                    groupId = groupId,
                    startDate = period.start,
                    endDate = period.end!!
                )
            }
            db.transaction { tx ->
                absenceService.upsertAbsences(
                    tx,
                    absenceDays.map { (date, type) ->
                        Absence(
                            absenceType = type,
                            childId = child.id,
                            date = date,
                            careType = CareType.DAYCARE
                        )
                    },
                    groupId, testDecisionMaker_1.id
                )
            }
        }

    private fun insertDecisionsAndPlacements(h: Handle, feeDecisions: List<FeeDecision>) {
        upsertFeeDecisions(h, objectMapper, feeDecisions)
        feeDecisions.forEach { decision ->
            decision.parts.forEach { part ->
                insertTestPlacement(
                    h,
                    childId = part.child.id,
                    unitId = part.placement.unit,
                    startDate = decision.validFrom,
                    endDate = decision.validTo!!,
                    type = when (part.placement.type) {
                        fi.espoo.evaka.invoicing.domain.PlacementType.CLUB -> PlacementType.CLUB
                        fi.espoo.evaka.invoicing.domain.PlacementType.DAYCARE,
                        fi.espoo.evaka.invoicing.domain.PlacementType.FIVE_YEARS_OLD_DAYCARE -> PlacementType.DAYCARE
                        fi.espoo.evaka.invoicing.domain.PlacementType.PRESCHOOL,
                        fi.espoo.evaka.invoicing.domain.PlacementType.PRESCHOOL_WITH_DAYCARE -> PlacementType.PRESCHOOL_DAYCARE
                        fi.espoo.evaka.invoicing.domain.PlacementType.PREPARATORY,
                        fi.espoo.evaka.invoicing.domain.PlacementType.PREPARATORY_WITH_DAYCARE -> PlacementType.PREPARATORY_DAYCARE
                    }
                )
            }
        }
    }

    private fun insertPlacement(childId: UUID, period: Period, type: PlacementType = PlacementType.DAYCARE) = { h: Handle ->
        insertTestPlacement(
            h,
            childId = childId,
            unitId = testDaycare.id,
            startDate = period.start,
            endDate = period.end!!,
            type = type
        )
    }

    private fun insertTemporaryServiceNeed(childId: UUID, period: Period, partDay: Boolean = false) = { h: Handle ->
        insertTestServiceNeed(
            h,
            childId,
            startDate = period.start,
            endDate = period.end!!,
            temporary = true,
            partDay = partDay,
            updatedBy = testDecisionMaker_1.id
        )
    }

    private fun insertChildParentRelation(headOfFamilyId: UUID, childId: UUID, period: Period) = { h: Handle ->
        insertTestParentship(
            h,
            headOfFamilyId,
            childId,
            startDate = period.start,
            endDate = period.end!!
        )
    }

    private val getAllInvoices: (Handle) -> List<Invoice> = { h ->
        h.createQuery(invoiceQueryBase)
            .map(toInvoice)
            .list()
            .let(::flatten)
            .map { it.copy(rows = it.rows.sortedByDescending { it.child.dateOfBirth }) }
    }
}
