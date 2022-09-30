// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionDistinctiveParams
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionSortParam
import fi.espoo.evaka.invoicing.createVoucherValueDecisionFixture
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testAdult_7
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.toValueDecisionServiceNeed
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

internal class VoucherValueDecisionQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val testPeriod = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `search with max fee accepted`() {
        val decisions = db.transaction { tx ->
            val baseDecision = { child: DevPerson ->
                createVoucherValueDecisionFixture(
                    status = VoucherValueDecisionStatus.DRAFT,
                    validFrom = testPeriod.start,
                    validTo = testPeriod.end,
                    headOfFamilyId = PersonId(UUID.randomUUID()),
                    childId = child.id,
                    dateOfBirth = child.dateOfBirth,
                    unitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1).copy(
                        headOfFamilyId = testAdult_1.id,
                        headOfFamilyIncome = null,
                    ),
                    baseDecision(testChild_2).copy(
                        headOfFamilyId = testAdult_2.id,
                        headOfFamilyIncome = DecisionIncome(
                            effect = IncomeEffect.MAX_FEE_ACCEPTED,
                            emptyMap(),
                            totalIncome = 0,
                            totalExpenses = 0,
                            total = 0,
                            worksAtECHA = false
                        ),
                    ),
                    baseDecision(testChild_3).copy(
                        headOfFamilyId = testAdult_3.id,
                        headOfFamilyIncome = null,
                        partnerId = testAdult_4.id,
                        partnerIncome = DecisionIncome(
                            effect = IncomeEffect.MAX_FEE_ACCEPTED,
                            emptyMap(),
                            totalIncome = 0,
                            totalExpenses = 0,
                            total = 0,
                            worksAtECHA = false
                        ),
                    ),
                    baseDecision(testChild_4).copy(
                        headOfFamilyId = testAdult_5.id,
                        headOfFamilyIncome = DecisionIncome(
                            effect = IncomeEffect.INCOME,
                            emptyMap(),
                            totalIncome = 200000,
                            totalExpenses = 0,
                            total = 200000,
                            worksAtECHA = false
                        )
                    ),
                    baseDecision(testChild_5).copy(
                        headOfFamilyId = testAdult_6.id,
                        headOfFamilyIncome = DecisionIncome(
                            effect = IncomeEffect.INCOME,
                            emptyMap(),
                            totalIncome = 200000,
                            totalExpenses = 0,
                            total = 200000,
                            worksAtECHA = false
                        ),
                        partnerId = testAdult_7.id,
                        partnerIncome = DecisionIncome(
                            effect = IncomeEffect.MAX_FEE_ACCEPTED,
                            emptyMap(),
                            totalIncome = 0,
                            totalExpenses = 0,
                            total = 0,
                            worksAtECHA = false
                        ),
                    ),
                )
            )
            val ids = tx.createQuery("SELECT id FROM voucher_value_decision").mapTo<VoucherValueDecisionId>()
            ids.map { id -> tx.getVoucherValueDecision(id)!! }
        }
        assertThat(decisions)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName }, { it.incomeEffect })
            .containsExactlyInAnyOrder(
                Tuple(testAdult_1.lastName, testAdult_1.firstName, IncomeEffect.NOT_AVAILABLE),
                Tuple(testAdult_2.lastName, testAdult_2.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(testAdult_3.lastName, testAdult_3.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(testAdult_5.lastName, testAdult_5.firstName, IncomeEffect.INCOME),
                Tuple(testAdult_6.lastName, testAdult_6.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
            )

        val result = db.read { tx ->
            tx.searchValueDecisions(
                evakaClock = MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(13, 37))),
                page = 0,
                pageSize = 100,
                sortBy = VoucherValueDecisionSortParam.HEAD_OF_FAMILY,
                sortDirection = SortDirection.ASC,
                status = VoucherValueDecisionStatus.DRAFT,
                areas = emptyList(),
                unit = null,
                startDate = null,
                endDate = null,
                financeDecisionHandlerId = null,
                distinctiveParams = listOf(VoucherValueDecisionDistinctiveParams.MAX_FEE_ACCEPTED),
            )
        }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactly(
                Tuple(testAdult_2.lastName, testAdult_2.firstName),
                Tuple(testAdult_3.lastName, testAdult_3.firstName),
                Tuple(testAdult_6.lastName, testAdult_6.firstName),
            )
    }
}
