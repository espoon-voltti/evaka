// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.sendVoucherValueDecisions
import fi.espoo.evaka.invoicing.createVoucherValueDecisionFixture
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.utils.europeHelsinki
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testArea2Id
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ServiceVoucherValueAreaReportTest : FullApplicationTest() {
    @Autowired
    private lateinit var asyncJobRunner: AsyncJobRunner

    @BeforeEach
    fun beforeEach() {
        db.transaction { it.insertGeneralTestFixtures() }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { it.resetDatabase() }
    }

    private val janFirst = LocalDate.of(2020, 1, 1)
    private val febFirst = LocalDate.of(2020, 2, 1)

    private val janFreeze = ZonedDateTime.of(LocalDateTime.of(2020, 1, 21, 22, 0), europeHelsinki).toInstant()

    @Test
    fun `unfrozen area voucher report includes value decisions that begin in the beginning of reports month`() {
        val sum = createTestSetOfDecisions()

        val janReport = getAreaReport(testAreaId, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsSum(testDaycare.id, sum)
    }

    @Test
    fun `frozen area voucher report includes value decisions that begin in the beginning of reports month`() {
        val sum = createTestSetOfDecisions()
        db.transaction { freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze) }

        val janReport = getAreaReport(testAreaId, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsSum(testDaycare.id, sum)
    }

    @Test
    fun `area voucher report includes corrections and refunds`() {
        val sum = createTestSetOfDecisions()
        db.transaction { freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze) }
        // co payment is dropped from 28800 to 0
        createVoucherDecision(janFirst, testDaycare.id, 87000, 0, testAdult_1.id, testChild_1, janFreeze.plusSeconds(3600))

        val febReport = getAreaReport(testAreaId, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReport.size)
        febReport.assertContainsSum(testDaycare.id, sum + 2 * 28800)
    }

    @Test
    fun `area voucher report includes refunds in old area and corrections in new`() {
        val sum = createTestSetOfDecisions()
        db.transaction { freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze) }
        // child is placed into another area
        createVoucherDecision(janFirst, testDaycare2.id, 87000, 28800, testAdult_1.id, testChild_1, janFreeze.plusSeconds(3600))

        val febReportOldArea = getAreaReport(testAreaId, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportOldArea.size)
        febReportOldArea.assertContainsSum(testDaycare.id, sum - 2 * 58200)

        val febReportNewArea = getAreaReport(testArea2Id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportNewArea.size)
        febReportNewArea.assertContainsSum(testDaycare2.id, 2 * 58200)
    }

    private fun List<ServiceVoucherValueUnitAggregate>.assertContainsSum(unitId: DaycareId, sum: Int) {
        val row = this.find { it.unit.id == unitId }
        assertNotNull(row)
        assertEquals(sum, row!!.monthlyPaymentSum)
    }

    private val adminUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.ADMIN))

    private fun getAreaReport(areaId: UUID, year: Int, month: Int): List<ServiceVoucherValueUnitAggregate> {
        val (_, response, data) = http.get(
            "/reports/service-voucher-value/units",
            listOf("areaId" to areaId, "year" to year, "month" to month)
        )
            .asUser(adminUser)
            .responseObject<ServiceVoucherValueReportController.ServiceVoucherReport>(objectMapper)
        assertEquals(200, response.statusCode)

        return data.get().rows
    }

    private fun createTestSetOfDecisions(): Int {
        return listOf(
            createVoucherDecision(janFirst, testDaycare.id, 87000, 28800, testAdult_1.id, testChild_1),
            createVoucherDecision(janFirst, testDaycare.id, 52200, 28800, testAdult_2.id, testChild_2),
            createVoucherDecision(janFirst, testDaycare.id, 134850, 0, testAdult_3.id, testChild_3)
        ).sumOf { decision ->
            decision.voucherValue - decision.coPayment
        }
    }

    private val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))

    private fun createVoucherDecision(
        validFrom: LocalDate,
        unitId: DaycareId,
        value: Int,
        coPayment: Int,
        adultId: UUID,
        child: PersonData.Detailed,
        approvedAt: Instant = ZonedDateTime.of(validFrom, LocalTime.of(15, 0), europeHelsinki).toInstant()
    ): VoucherValueDecision {
        val decision = db.transaction {
            val decision = createVoucherValueDecisionFixture(
                status = VoucherValueDecisionStatus.DRAFT,
                validFrom = validFrom,
                validTo = null,
                headOfFamilyId = adultId,
                childId = child.id,
                dateOfBirth = child.dateOfBirth,
                unitId = unitId,
                value = value,
                coPayment = coPayment,
                placementType = PlacementType.DAYCARE,
                serviceNeed = VoucherValueDecisionServiceNeed(
                    snDefaultDaycare.feeCoefficient,
                    snDefaultDaycare.voucherValueCoefficient,
                    snDefaultDaycare.feeDescriptionFi,
                    snDefaultDaycare.feeDescriptionSv,
                    snDefaultDaycare.voucherValueDescriptionFi,
                    snDefaultDaycare.voucherValueDescriptionSv
                )
            )
            it.upsertValueDecisions(listOf(decision))

            sendVoucherValueDecisions(
                tx = it,
                asyncJobRunner = asyncJobRunner,
                user = financeUser,
                now = approvedAt,
                ids = listOf(decision.id)
            )

            decision
        }

        asyncJobRunner.runPendingJobsSync()

        return decision
    }
}
