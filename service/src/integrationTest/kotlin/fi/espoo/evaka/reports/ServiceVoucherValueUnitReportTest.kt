package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createVoucherValueDecisionFixture
import fi.espoo.evaka.invoicing.createVoucherValueDecisionPartFixture
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.reports.VoucherReportRowType.CORRECTION
import fi.espoo.evaka.reports.VoucherReportRowType.ORIGINAL
import fi.espoo.evaka.reports.VoucherReportRowType.REFUND
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.utils.EvakaClock
import fi.espoo.evaka.shared.utils.zoneId
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

class ServiceVoucherValueUnitReportTest : FullApplicationTest() {
    @Autowired
    private lateinit var asyncJobRunner: AsyncJobRunner

    private val janFirst = LocalDate.of(2020, 1, 1)
    private val febFirst = LocalDate.of(2020, 2, 1)
    private val marFirst = LocalDate.of(2020, 3, 1)

    private val janFreeze = ZonedDateTime.of(LocalDateTime.of(2020, 1, 21, 22, 0), zoneId)
    private val febFreeze = ZonedDateTime.of(LocalDateTime.of(2020, 2, 21, 22, 0), zoneId)

    @BeforeEach
    fun beforeEach() {
        db.transaction { insertGeneralTestFixtures(it.handle) }
        EvakaClock.set(janFirst)
    }

    @AfterEach
    fun afterEach() {
        db.transaction { it.resetDatabase() }
    }

    @AfterAll
    override fun afterAll() {
        super.afterAll()
        EvakaClock.useRealTime()
    }

    @Test
    fun `unfrozen service voucher report includes value decisions that begin in the beginning of reports month`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(ORIGINAL, janFirst, janFirst.toEndOfMonth(), 87000, 28800, 58200)
    }

    @Test
    fun `unfrozen service voucher report includes value decisions that begin in the middle of reports month`() {
        val middleOfMonth = LocalDate.of(2020, 1, 15)
        createAndApproveVoucherDecision(middleOfMonth, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        val janReport = getUnitReport(testDaycare.id, middleOfMonth.year, middleOfMonth.monthValue)
        assertEquals(1, janReport.size)
        // 31916 is 17 / 31 * 58200 rounded to closest integer
        janReport.assertContainsRow(ORIGINAL, middleOfMonth, middleOfMonth.toEndOfMonth(), 87000, 28800, 31916)
    }

    @Test
    fun `unfrozen service voucher report does not include value decisions to other units`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        val janReport = getUnitReport(testDaycare2.id, janFirst.year, janFirst.monthValue)
        assertEquals(0, janReport.size)
    }

    @Test
    fun `frozen service voucher report includes value decisions that begin in the beginning of reports month`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        EvakaClock.set(janFreeze)
        triggerFreeze()

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(ORIGINAL, janFirst, janFirst.toEndOfMonth(), 87000, 28800, 58200)
    }

    @Test
    fun `frozen service voucher report keeps previously frozen rows intact`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)

        EvakaClock.set(janFreeze)
        triggerFreeze()

        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 10000)

        val janReport = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReport.size)
        janReport.assertContainsRow(ORIGINAL, janFirst, janFirst.toEndOfMonth(), 87000, 0, 87000)
    }

    @Test
    fun `new value decisions appear as corrections in next months report after freezing`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)

        EvakaClock.set(janFreeze)
        triggerFreeze()

        EvakaClock.plusHours(1)
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 10000)

        EvakaClock.plusHours(1)
        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(3, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
        febReport.assertContainsRow(CORRECTION, janFirst, janFirst.toEndOfMonth(), 87000, 10000, 77000)
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 10000, 77000)
    }

    @Test
    fun `if there is a change on part of the month, the whole month gets refunded and corrected`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)

        EvakaClock.set(janFreeze)
        triggerFreeze()

        EvakaClock.plusHours(1)
        val middleOfMonth = janFirst.plusDays(15)
        createAndApproveVoucherDecision(middleOfMonth, unitId = testDaycare.id, value = 87000, coPayment = 10000)

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(4, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)
        // 15 days -> (15/31) * 87000 = ~42097
        febReport.assertContainsRow(CORRECTION, janFirst, middleOfMonth.minusDays(1), 87000, 0, 42097)
        // 16 days -> (16/31) * 77000 = ~39742
        febReport.assertContainsRow(CORRECTION, middleOfMonth, middleOfMonth.toEndOfMonth(), 87000, 10000, 39742)
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 10000, 77000)
    }

    @Test
    fun `new value decisions to a different unit cause refunds in the old unit but corrections in the new unit`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)

        EvakaClock.set(janFreeze)
        triggerFreeze()

        EvakaClock.plusHours(1)
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare2.id, value = 87000, coPayment = 10000)

        val febReportInOldUnit = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportInOldUnit.size)
        febReportInOldUnit.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 0, -87000)

        val febReportInNewUnit = getUnitReport(testDaycare2.id, febFirst.year, febFirst.monthValue)
        assertEquals(2, febReportInNewUnit.size)
        febReportInNewUnit.assertContainsRow(CORRECTION, janFirst, janFirst.toEndOfMonth(), 87000, 10000, 77000)
        febReportInNewUnit.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 10000, 77000)
    }

    @Test
    fun `value decision double corrections refund the latest corrected value`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)

        EvakaClock.set(janFreeze)
        triggerFreeze()

        EvakaClock.plusHours(1)
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 10000)

        EvakaClock.set(febFreeze)
        triggerFreeze()

        EvakaClock.plusHours(1)
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 5000)

        val marchReport = getUnitReport(testDaycare.id, marFirst.year, marFirst.monthValue)
        assertEquals(5, marchReport.size)
        marchReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 10000, -77000)
        marchReport.assertContainsRow(CORRECTION, janFirst, janFirst.toEndOfMonth(), 87000, 5000, 82000)
        marchReport.assertContainsRow(REFUND, febFirst, febFirst.toEndOfMonth(), 87000, 10000, -77000)
        marchReport.assertContainsRow(CORRECTION, febFirst, febFirst.toEndOfMonth(), 87000, 5000, 82000)
        marchReport.assertContainsRow(ORIGINAL, marFirst, marFirst.toEndOfMonth(), 87000, 5000, 82000)
    }

    @Test
    fun `decisions not included in previous months report are included as corrections in the next`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(2, febReport.size)
        febReport.assertContainsRow(CORRECTION, janFirst, janFirst.toEndOfMonth(), 87000, 0, 87000)
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 0, 87000)
    }

    @Test
    fun `split old decisions are both refunded`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        val middleOfMonth = LocalDate.of(2020, 1, 15)
        createAndApproveVoucherDecision(middleOfMonth, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        EvakaClock.set(janFreeze)
        triggerFreeze()

        EvakaClock.plusHours(1)
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 10000)

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(4, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, middleOfMonth.minusDays(1), 87000, 0, -39290)
        febReport.assertContainsRow(REFUND, middleOfMonth, middleOfMonth.toEndOfMonth(), 87000, 28800, -31916)
        febReport.assertContainsRow(CORRECTION, janFirst, janFirst.toEndOfMonth(), 87000, 10000, 77000)
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 10000, 77000)
    }

    @Test
    fun `split new decisions are both corrected`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 10000)

        EvakaClock.set(janFreeze)
        triggerFreeze()

        EvakaClock.plusHours(1)
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 0)
        val middleOfMonth = LocalDate.of(2020, 1, 15)
        createAndApproveVoucherDecision(middleOfMonth, unitId = testDaycare.id, value = 87000, coPayment = 28800)

        val febReport = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(4, febReport.size)
        febReport.assertContainsRow(REFUND, janFirst, janFirst.toEndOfMonth(), 87000, 10000, -77000)
        febReport.assertContainsRow(CORRECTION, janFirst, middleOfMonth.minusDays(1), 87000, 0, 39290)
        febReport.assertContainsRow(CORRECTION, middleOfMonth, middleOfMonth.toEndOfMonth(), 87000, 28800, 31916)
        febReport.assertContainsRow(ORIGINAL, febFirst, febFirst.toEndOfMonth(), 87000, 28800, 58200)
    }

    @Test
    fun `isNew property is properly deduced for frozen and unfrozen rows`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 10000)

        val janReportBeforeFreeze = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReportBeforeFreeze.size)
        assertTrue(janReportBeforeFreeze.first().isNew)

        EvakaClock.set(janFreeze)
        triggerFreeze()

        val janReportAfterFreeze = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReportAfterFreeze.size)
        assertTrue(janReportAfterFreeze.first().isNew)

        val febReportBeforeFreeze = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportBeforeFreeze.size)
        assertFalse(febReportBeforeFreeze.first().isNew)

        EvakaClock.set(febFreeze)
        triggerFreeze()

        val febReportAfterFreeze = getUnitReport(testDaycare.id, febFirst.year, febFirst.monthValue)
        assertEquals(1, febReportAfterFreeze.size)
        assertFalse(febReportAfterFreeze.first().isNew)

        val janReportAfterFebFreeze = getUnitReport(testDaycare.id, janFirst.year, janFirst.monthValue)
        assertEquals(1, janReportAfterFebFreeze.size)
        assertTrue(janReportAfterFebFreeze.first().isNew)
    }

    @Test
    fun `future service voucher report does not include unfrozen months as corrections`() {
        createAndApproveVoucherDecision(janFirst, unitId = testDaycare.id, value = 87000, coPayment = 28800)
        EvakaClock.set(janFreeze)
        triggerFreeze()

        val marchReport = getUnitReport(testDaycare.id, marFirst.year, marFirst.monthValue)
        assertEquals(1, marchReport.size)
        marchReport.assertContainsRow(ORIGINAL, marFirst, marFirst.toEndOfMonth(), 87000, 28800, 58200)
    }

    private fun List<ServiceVoucherValueRow>.assertContainsRow(
        type: VoucherReportRowType,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        value: Int,
        coPayment: Int,
        realizedValue: Int
    ) {
        val row = this.find {
            it.type == type && it.realizedPeriod.start == periodStart && it.realizedPeriod.end == periodEnd
        }
        assertNotNull(row)
        row!!.let {
            assertEquals(value, row.serviceVoucherValue)
            assertEquals(coPayment, row.serviceVoucherCoPayment)
            assertEquals(realizedValue, row.realizedAmount)
        }
    }

    private fun LocalDate.toEndOfMonth() = this.plusMonths(1).withDayOfMonth(1).minusDays(1)

    private val adminUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(UserRole.ADMIN))

    private fun getUnitReport(unitId: UUID, year: Int, month: Int): List<ServiceVoucherValueRow> {
        val (_, response, data) = http.get(
            "/reports/service-voucher-value/units/$unitId",
            listOf("year" to year, "month" to month)
        )
            .asUser(adminUser)
            .responseObject<ServiceVoucherValueReportController.ServiceVoucherUnitReport>(objectMapper)
        assertEquals(200, response.statusCode)

        return data.get().rows
    }

    private fun triggerFreeze() {
        val (_, response, _) = http.post("/scheduled/freeze-voucher-value-reports").response()
        assertEquals(204, response.statusCode)
    }

    private val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))

    private fun createAndApproveVoucherDecision(
        validFrom: LocalDate,
        unitId: UUID,
        value: Int,
        coPayment: Int
    ) {
        val decisionId = db.transaction { tx ->
            val parts = listOf(
                createVoucherValueDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    unitId = unitId,
                    value = value,
                    coPayment = coPayment
                )
            )

            val decision = createVoucherValueDecisionFixture(
                status = VoucherValueDecisionStatus.DRAFT,
                validFrom = validFrom,
                validTo = null,
                headOfFamilyId = testAdult_1.id,
                parts = parts
            )

            tx.handle.upsertValueDecisions(objectMapper, listOf(decision))

            decision.id
        }

        val (_, response, _) = http.post("/value-decisions/send")
            .jsonBody(objectMapper.writeValueAsString(listOf(decisionId)))
            .asUser(financeUser)
            .response()
        assertEquals(204, response.statusCode)

        asyncJobRunner.runPendingJobsSync()
    }
}
