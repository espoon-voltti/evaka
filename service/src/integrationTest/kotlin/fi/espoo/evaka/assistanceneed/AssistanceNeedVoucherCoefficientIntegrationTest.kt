// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficient
import fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController
import fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientRequest
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.insertServiceNeedOptionVoucherValues
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.shared.AssistanceNeedVoucherCoefficientId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testVoucherDaycare
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AssistanceNeedVoucherCoefficientIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var assistanceNeedVoucherCoefficientController:
        AssistanceNeedVoucherCoefficientController

    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val user = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    private val today = LocalDate.of(2021, 5, 5)
    private val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 1, 0)))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(feeThresholds)
            tx.insertServiceNeedOptions()
            tx.insertServiceNeedOptionVoucherValues()

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testVoucherDaycare.id,
                        startDate = today,
                        endDate = today.plusDays(30)
                    )
                )
            val period = FiniteDateRange(today, today.plusDays(30))
            tx.insert(
                DevServiceNeed(
                    placementId = placementId,
                    startDate = period.start,
                    endDate = period.end,
                    optionId = snDefaultDaycare.id,
                    confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    confirmedAt = HelsinkiDateTime.now()
                )
            )
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = testChild_1.dateOfBirth,
                    endDate = testChild_1.dateOfBirth.plusYears(18).minusDays(1)
                )
            )
        }
    }

    @Test
    fun works() {
        val created =
            createAssistanceNeedVoucherCoefficient(
                AssistanceNeedVoucherCoefficientRequest(
                    coefficient = 2.0,
                    validityPeriod = FiniteDateRange(today, today.plusDays(30))
                )
            )
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(testChild_1.id, created.childId)
        assertEquals(BigDecimal("2.00"), created.coefficient)
        assertEquals(FiniteDateRange(today, today.plusDays(30)), created.validityPeriod)
        assertEquals(listOf(BigDecimal("2.00")), readVoucherValueDecisionAssistanceCoefficients())

        val updated =
            updateAssistanceNeedVoucherCoefficient(
                created.id,
                AssistanceNeedVoucherCoefficientRequest(
                    coefficient = 3.0,
                    validityPeriod = FiniteDateRange(today.plusDays(10), today.plusDays(20))
                )
            )
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(testChild_1.id, updated.childId)
        assertEquals(BigDecimal("3.00"), updated.coefficient)
        assertEquals(
            FiniteDateRange(today.plusDays(10), today.plusDays(20)),
            updated.validityPeriod
        )
        assertEquals(
            listOf(BigDecimal("1.00"), BigDecimal("3.00"), BigDecimal("1.00")),
            readVoucherValueDecisionAssistanceCoefficients()
        )

        deleteAssistanceNeedVoucherCoefficient(created.id)
        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(listOf(BigDecimal("1.00")), readVoucherValueDecisionAssistanceCoefficients())
    }

    private fun createAssistanceNeedVoucherCoefficient(
        body: AssistanceNeedVoucherCoefficientRequest
    ): AssistanceNeedVoucherCoefficient {
        return assistanceNeedVoucherCoefficientController.createAssistanceNeedVoucherCoefficient(
            dbInstance(),
            user,
            clock,
            testChild_1.id,
            body,
        )
    }

    private fun updateAssistanceNeedVoucherCoefficient(
        id: AssistanceNeedVoucherCoefficientId,
        body: AssistanceNeedVoucherCoefficientRequest
    ): AssistanceNeedVoucherCoefficient {
        return assistanceNeedVoucherCoefficientController.updateAssistanceNeedVoucherCoefficient(
            dbInstance(),
            user,
            clock,
            id,
            body,
        )
    }

    private fun deleteAssistanceNeedVoucherCoefficient(id: AssistanceNeedVoucherCoefficientId) {
        assistanceNeedVoucherCoefficientController.deleteAssistanceNeedVoucherCoefficient(
            dbInstance(),
            user,
            clock,
            id,
        )
    }

    private fun readVoucherValueDecisionAssistanceCoefficients(): List<BigDecimal> {
        return db.read { tx ->
            @Suppress("DEPRECATION")
            tx.createQuery(
                    "SELECT assistance_need_coefficient FROM voucher_value_decision ORDER BY valid_from"
                )
                .toList<BigDecimal>()
        }
    }
}
