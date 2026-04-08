// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.assistanceneed

import evaka.core.FullApplicationTest
import evaka.core.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficient
import evaka.core.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController
import evaka.core.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientRequest
import evaka.core.daycare.domain.ProviderType
import evaka.core.feeThresholds
import evaka.core.insertServiceNeedOptionVoucherValues
import evaka.core.insertServiceNeedOptions
import evaka.core.shared.AssistanceNeedVoucherCoefficientId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.snDefaultDaycare
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

    private val area = DevCareArea()
    private val voucherDaycare =
        DevDaycare(areaId = area.id, providerType = ProviderType.PRIVATE_SERVICE_VOUCHER)
    private val employee = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adult = DevPerson()
    private val child = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val today = LocalDate.of(2021, 5, 5)
    private val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 1, 0)))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(voucherDaycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(feeThresholds)
            tx.insertServiceNeedOptions()
            tx.insertServiceNeedOptionVoucherValues()

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = voucherDaycare.id,
                        startDate = today,
                        endDate = today.plusDays(30),
                    )
                )
            val period = FiniteDateRange(today, today.plusDays(30))
            tx.insert(
                DevServiceNeed(
                    placementId = placementId,
                    startDate = period.start,
                    endDate = period.end,
                    optionId = snDefaultDaycare.id,
                    confirmedBy = employee.evakaUserId,
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
            tx.insert(
                DevParentship(
                    childId = child.id,
                    headOfChildId = adult.id,
                    startDate = child.dateOfBirth,
                    endDate = child.dateOfBirth.plusYears(18).minusDays(1),
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
                    validityPeriod = FiniteDateRange(today, today.plusDays(30)),
                )
            )
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(child.id, created.childId)
        assertEquals(BigDecimal("2.0000"), created.coefficient)
        assertEquals(FiniteDateRange(today, today.plusDays(30)), created.validityPeriod)
        assertEquals(listOf(BigDecimal("2.0000")), readVoucherValueDecisionAssistanceCoefficients())

        val updated =
            updateAssistanceNeedVoucherCoefficient(
                created.id,
                AssistanceNeedVoucherCoefficientRequest(
                    coefficient = 3.0,
                    validityPeriod = FiniteDateRange(today.plusDays(10), today.plusDays(20)),
                ),
            )
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(child.id, updated.childId)
        assertEquals(BigDecimal("3.0000"), updated.coefficient)
        assertEquals(
            FiniteDateRange(today.plusDays(10), today.plusDays(20)),
            updated.validityPeriod,
        )
        assertEquals(
            listOf(BigDecimal("1.0000"), BigDecimal("3.0000"), BigDecimal("1.0000")),
            readVoucherValueDecisionAssistanceCoefficients(),
        )

        deleteAssistanceNeedVoucherCoefficient(created.id)
        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(listOf(BigDecimal("1.0000")), readVoucherValueDecisionAssistanceCoefficients())
    }

    @Test
    fun `coefficient supports four decimal places`() {
        val created =
            createAssistanceNeedVoucherCoefficient(
                AssistanceNeedVoucherCoefficientRequest(
                    coefficient = 1.5234,
                    validityPeriod = FiniteDateRange(today, today.plusDays(30)),
                )
            )
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(BigDecimal("1.5234"), created.coefficient)
        assertEquals(listOf(BigDecimal("1.5234")), readVoucherValueDecisionAssistanceCoefficients())

        val updated =
            updateAssistanceNeedVoucherCoefficient(
                created.id,
                AssistanceNeedVoucherCoefficientRequest(
                    coefficient = 2.7891,
                    validityPeriod = FiniteDateRange(today, today.plusDays(30)),
                ),
            )
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(BigDecimal("2.7891"), updated.coefficient)
        assertEquals(listOf(BigDecimal("2.7891")), readVoucherValueDecisionAssistanceCoefficients())
    }

    @Test
    fun `coefficient with four decimal places can be retrieved`() {
        val created =
            createAssistanceNeedVoucherCoefficient(
                AssistanceNeedVoucherCoefficientRequest(
                    coefficient = 1.2345,
                    validityPeriod = FiniteDateRange(today, today.plusDays(30)),
                )
            )

        val coefficients =
            assistanceNeedVoucherCoefficientController.getAssistanceNeedVoucherCoefficients(
                dbInstance(),
                employee.user,
                clock,
                child.id,
            )

        val coefficient = coefficients.find { it.voucherCoefficient.id == created.id }
        assertEquals(BigDecimal("1.2345"), coefficient?.voucherCoefficient?.coefficient)
    }

    private fun createAssistanceNeedVoucherCoefficient(
        body: AssistanceNeedVoucherCoefficientRequest
    ): AssistanceNeedVoucherCoefficient {
        return assistanceNeedVoucherCoefficientController.createAssistanceNeedVoucherCoefficient(
            dbInstance(),
            employee.user,
            clock,
            child.id,
            body,
        )
    }

    private fun updateAssistanceNeedVoucherCoefficient(
        id: AssistanceNeedVoucherCoefficientId,
        body: AssistanceNeedVoucherCoefficientRequest,
    ): AssistanceNeedVoucherCoefficient {
        return assistanceNeedVoucherCoefficientController.updateAssistanceNeedVoucherCoefficient(
            dbInstance(),
            employee.user,
            clock,
            id,
            body,
        )
    }

    private fun deleteAssistanceNeedVoucherCoefficient(id: AssistanceNeedVoucherCoefficientId) {
        assistanceNeedVoucherCoefficientController.deleteAssistanceNeedVoucherCoefficient(
            dbInstance(),
            employee.user,
            clock,
            id,
        )
    }

    private fun readVoucherValueDecisionAssistanceCoefficients(): List<BigDecimal> {
        return db.read { tx ->
            tx.createQuery {
                    sql(
                        "SELECT assistance_need_coefficient FROM voucher_value_decision ORDER BY valid_from"
                    )
                }
                .toList<BigDecimal>()
        }
    }
}
