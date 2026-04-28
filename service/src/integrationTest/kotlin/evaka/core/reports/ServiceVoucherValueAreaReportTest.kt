// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.caseprocess.CaseProcessMetadataService
import evaka.core.invoicing.controller.sendVoucherValueDecisions
import evaka.core.invoicing.createVoucherValueDecisionFixture
import evaka.core.invoicing.data.upsertValueDecisions
import evaka.core.invoicing.domain.VoucherValueDecision
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.snDefaultDaycare
import evaka.core.toValueDecisionServiceNeed
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

    @Autowired
    private lateinit var serviceVoucherValueReportController: ServiceVoucherValueReportController

    private val clock = MockEvakaClock(2020, 2, 1, 12, 0)

    private val area1 = DevCareArea(name = "Area 1", shortName = "area1")
    private val area2 = DevCareArea(name = "Area 2", shortName = "area2")
    private val daycareInArea1 = DevDaycare(areaId = area1.id, name = "Daycare 1")
    private val daycareInArea2 = DevDaycare(areaId = area2.id, name = "Daycare 2")
    private val employee = DevEmployee()
    private val adult1 =
        DevPerson(
            ssn = "010180-1232",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val adult2 =
        DevPerson(
            ssn = "010279-123L",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val adult3 = DevPerson()
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))
    private val child3 = DevPerson(dateOfBirth = LocalDate.of(2018, 9, 1))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area1)
            tx.insert(area2)
            tx.insert(daycareInArea1)
            tx.insert(daycareInArea2)
            listOf(adult1, adult2, adult3).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(child1, child2, child3).forEach { tx.insert(it, DevPersonType.CHILD) }
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
            adult1.id,
            child1,
            janFreeze.plusSeconds(3600),
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
            adult1.id,
            child1,
            janFreeze.plusSeconds(3600),
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
        sum: Int,
    ) {
        val row = this.find { it.unit.id == unitId }
        assertNotNull(row)
        assertEquals(sum, row.monthlyPaymentSum)
    }

    private val adminUser =
        AuthenticatedUser.Employee(id = employee.id, roles = setOf(UserRole.ADMIN))

    private fun getAreaReport(
        areaId: AreaId,
        year: Int,
        month: Int,
    ): List<ServiceVoucherValueUnitAggregate> {
        return serviceVoucherValueReportController
            .getServiceVoucherReportForAllUnits(dbInstance(), adminUser, clock, year, month, areaId)
            .rows
    }

    private fun createTestSetOfDecisions(): Int {
        return listOf(
                createVoucherDecision(janFirst, daycareInArea1.id, 87000, 28800, adult1.id, child1),
                createVoucherDecision(janFirst, daycareInArea1.id, 52200, 28800, adult2.id, child2),
                createVoucherDecision(janFirst, daycareInArea1.id, 134850, 0, adult3.id, child3),
            )
            .sumOf { decision -> decision.voucherValue - decision.coPayment }
    }

    private val financeUser =
        AuthenticatedUser.Employee(id = employee.id, roles = setOf(UserRole.FINANCE_ADMIN))

    private fun createVoucherDecision(
        validFrom: LocalDate,
        unitId: DaycareId,
        value: Int,
        coPayment: Int,
        adultId: PersonId,
        child: DevPerson,
        approvedAt: HelsinkiDateTime = HelsinkiDateTime.of(validFrom, LocalTime.of(15, 0)),
    ): VoucherValueDecision {
        val decision = db.transaction {
            val decision =
                createVoucherValueDecisionFixture(
                    status = VoucherValueDecisionStatus.DRAFT,
                    validFrom = validFrom,
                    validTo = validFrom.plusYears(1),
                    headOfFamilyId = adultId,
                    childId = child.id,
                    dateOfBirth = child.dateOfBirth,
                    unitId = unitId,
                    value = value,
                    coPayment = coPayment,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
                )
            it.upsertValueDecisions(listOf(decision))

            sendVoucherValueDecisions(
                tx = it,
                asyncJobRunner = asyncJobRunner,
                user = financeUser,
                evakaEnv = evakaEnv,
                metadata = CaseProcessMetadataService(featureConfig),
                now = approvedAt,
                ids = listOf(decision.id),
                decisionHandlerId = null,
                false,
            )

            decision
        }

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        return decision
    }
}
