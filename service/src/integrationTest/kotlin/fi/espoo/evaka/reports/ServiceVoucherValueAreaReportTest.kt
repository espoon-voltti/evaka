// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.invoicing.controller.sendVoucherValueDecisions
import fi.espoo.evaka.invoicing.createVoucherValueDecisionFixture
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toValueDecisionServiceNeed
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ServiceVoucherValueAreaReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val area1 = DevCareArea(name = "Area 1", shortName = "area1")
    private val area2 = DevCareArea(name = "Area 2", shortName = "area2")
    private val daycareInArea1 = DevDaycare(areaId = area1.id)
    private val daycareInArea2 = DevDaycare(areaId = area2.id)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(area1)
            tx.insert(area2)
            tx.insert(daycareInArea1)
            tx.insert(daycareInArea2)
            listOf(testAdult_1, testAdult_2, testAdult_3).forEach {
                tx.insert(it, DevPersonType.ADULT)
            }
            listOf(testChild_1, testChild_2, testChild_3).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
        }
    }

    private val janFirst = LocalDate.of(2020, 1, 1)
    private val febFirst = LocalDate.of(2020, 2, 1)

    private val janFreeze = HelsinkiDateTime.of(LocalDateTime.of(2020, 1, 21, 22, 0))

    @Test
    fun `unfrozen area voucher report includes value decisions that begin in the beginning of reports month`() {
        val sum = createTestSetOfDecisions()

        val janReport = getAreaReport(area1.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsSum(daycareInArea1.id, sum)
    }

    @Test
    fun `frozen area voucher report includes value decisions that begin in the beginning of reports month`() {
        val sum = createTestSetOfDecisions()
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }

        val janReport = getAreaReport(area1.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsSum(daycareInArea1.id, sum)
    }

    @Test
    fun `area voucher report includes corrections and refunds`() {
        val sum = createTestSetOfDecisions()
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        // co payment is dropped from 28800 to 0
        createVoucherDecision(
            janFirst,
            daycareInArea1.id,
            87000,
            0,
            testAdult_1.id,
            testChild_1,
            janFreeze.plusSeconds(3600)
        )

        val febReport = getAreaReport(area1.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReport.size)
        febReport.assertContainsSum(daycareInArea1.id, sum + 2 * 28800)
    }

    @Test
    fun `area voucher report includes refunds in old area and corrections in new`() {
        val sum = createTestSetOfDecisions()
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        // child is placed into another area
        createVoucherDecision(
            janFirst,
            daycareInArea2.id,
            87000,
            28800,
            testAdult_1.id,
            testChild_1,
            janFreeze.plusSeconds(3600)
        )

        val febReportOldArea = getAreaReport(area1.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportOldArea.size)
        febReportOldArea.assertContainsSum(daycareInArea1.id, sum - 2 * 58200)

        val febReportNewArea = getAreaReport(area2.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportNewArea.size)
        febReportNewArea.assertContainsSum(daycareInArea2.id, 2 * 58200)
    }

    private fun List<ServiceVoucherValueUnitAggregate>.assertContainsSum(
        unitId: DaycareId,
        sum: Int
    ) {
        val row = this.find { it.unit.id == unitId }
        assertNotNull(row)
        assertEquals(sum, row.monthlyPaymentSum)
    }

    private val adminUser =
        AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.ADMIN))

    private fun getAreaReport(
        areaId: AreaId,
        year: Int,
        month: Int
    ): List<ServiceVoucherValueUnitAggregate> {
        val (_, response, data) =
            http
                .get(
                    "/reports/service-voucher-value/units",
                    listOf("areaId" to areaId, "year" to year, "month" to month)
                )
                .asUser(adminUser)
                .responseObject<ServiceVoucherReport>(jsonMapper)
        assertEquals(200, response.statusCode)

        return data.get().rows
    }

    private fun createTestSetOfDecisions(): Int {
        return listOf(
                createVoucherDecision(
                    janFirst,
                    daycareInArea1.id,
                    87000,
                    28800,
                    testAdult_1.id,
                    testChild_1
                ),
                createVoucherDecision(
                    janFirst,
                    daycareInArea1.id,
                    52200,
                    28800,
                    testAdult_2.id,
                    testChild_2
                ),
                createVoucherDecision(
                    janFirst,
                    daycareInArea1.id,
                    134850,
                    0,
                    testAdult_3.id,
                    testChild_3
                )
            )
            .sumOf { decision -> decision.voucherValue - decision.coPayment }
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
        adultId: PersonId,
        child: DevPerson,
        approvedAt: HelsinkiDateTime = HelsinkiDateTime.of(validFrom, LocalTime.of(15, 0))
    ): VoucherValueDecision {
        val decision =
            db.transaction {
                val decision =
                    createVoucherValueDecisionFixture(
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
                        serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed()
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
                    false
                )

                decision
            }

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        return decision
    }
}
