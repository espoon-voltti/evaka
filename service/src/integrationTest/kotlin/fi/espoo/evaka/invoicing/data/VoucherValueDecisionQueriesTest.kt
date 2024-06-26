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
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDifference
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testAdult_7
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.toValueDecisionServiceNeed
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VoucherValueDecisionQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val testPeriod = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testDaycare)
            listOf(
                    testAdult_1,
                    testAdult_2,
                    testAdult_3,
                    testAdult_4,
                    testAdult_5,
                    testAdult_6,
                    testAdult_7
                )
                .forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2, testChild_3, testChild_4, testChild_5).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
        }
    }

    @Test
    fun `search with max fee accepted`() {
        val decisions =
            db.transaction { tx ->
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
                        serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                    )
                }
                tx.upsertValueDecisions(
                    listOf(
                        baseDecision(testChild_1)
                            .copy(headOfFamilyId = testAdult_1.id, headOfFamilyIncome = null),
                        baseDecision(testChild_2)
                            .copy(
                                headOfFamilyId = testAdult_2.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false
                                    )
                            ),
                        baseDecision(testChild_3)
                            .copy(
                                headOfFamilyId = testAdult_3.id,
                                headOfFamilyIncome = null,
                                partnerId = testAdult_4.id,
                                partnerIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false
                                    )
                            ),
                        baseDecision(testChild_4)
                            .copy(
                                headOfFamilyId = testAdult_5.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.INCOME,
                                        emptyMap(),
                                        totalIncome = 200000,
                                        totalExpenses = 0,
                                        total = 200000,
                                        worksAtECHA = false
                                    )
                            ),
                        baseDecision(testChild_5)
                            .copy(
                                headOfFamilyId = testAdult_6.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.INCOME,
                                        emptyMap(),
                                        totalIncome = 200000,
                                        totalExpenses = 0,
                                        total = 200000,
                                        worksAtECHA = false
                                    ),
                                partnerId = testAdult_7.id,
                                partnerIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false
                                    )
                            )
                    )
                )
                val ids =
                    @Suppress("DEPRECATION")
                    tx.createQuery("SELECT id FROM voucher_value_decision")
                        .toList<VoucherValueDecisionId>()
                ids.map { id -> tx.getVoucherValueDecision(id)!! }
            }
        assertThat(decisions)
            .extracting(
                { it.headOfFamily.lastName },
                { it.headOfFamily.firstName },
                { it.incomeEffect }
            )
            .containsExactlyInAnyOrder(
                Tuple(testAdult_1.lastName, testAdult_1.firstName, IncomeEffect.NOT_AVAILABLE),
                Tuple(testAdult_2.lastName, testAdult_2.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(testAdult_3.lastName, testAdult_3.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(testAdult_5.lastName, testAdult_5.firstName, IncomeEffect.INCOME),
                Tuple(testAdult_6.lastName, testAdult_6.firstName, IncomeEffect.MAX_FEE_ACCEPTED)
            )

        val result =
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(13, 37))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams =
                        listOf(VoucherValueDecisionDistinctiveParams.MAX_FEE_ACCEPTED)
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactly(
                Tuple(testAdult_2.lastName, testAdult_2.firstName),
                Tuple(testAdult_3.lastName, testAdult_3.firstName),
                Tuple(testAdult_6.lastName, testAdult_6.firstName)
            )
    }

    @Test
    fun `search with difference`() {
        db.transaction { tx ->
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
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(headOfFamilyId = testAdult_1.id, difference = emptySet()),
                    baseDecision(testChild_2)
                        .copy(
                            headOfFamilyId = testAdult_2.id,
                            difference = setOf(VoucherValueDecisionDifference.INCOME)
                        ),
                    baseDecision(testChild_3)
                        .copy(
                            headOfFamilyId = testAdult_3.id,
                            difference =
                                setOf(
                                    VoucherValueDecisionDifference.INCOME,
                                    VoucherValueDecisionDifference.FAMILY_SIZE
                                )
                        ),
                    baseDecision(testChild_4)
                        .copy(
                            headOfFamilyId = testAdult_4.id,
                            difference = setOf(VoucherValueDecisionDifference.PLACEMENT)
                        )
                )
            )
        }

        val result =
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = setOf(VoucherValueDecisionDifference.INCOME),
                    distinctiveParams = emptyList()
                )
            }

        assertThat(result.data)
            .extracting(
                { it.headOfFamily.lastName },
                { it.headOfFamily.firstName },
                { it.difference }
            )
            .containsExactly(
                Tuple(
                    testAdult_2.lastName,
                    testAdult_2.firstName,
                    setOf(VoucherValueDecisionDifference.INCOME)
                ),
                Tuple(
                    testAdult_3.lastName,
                    testAdult_3.firstName,
                    setOf(
                        VoucherValueDecisionDifference.INCOME,
                        VoucherValueDecisionDifference.FAMILY_SIZE
                    )
                )
            )
    }

    @Test
    fun `search with UNCONFIRMED_HOURS`() {
        db.transaction { tx ->
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
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(
                            headOfFamilyId = testAdult_1.id,
                            serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                        ),
                    baseDecision(testChild_2)
                        .copy(
                            headOfFamilyId = testAdult_2.id,
                            serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed()
                        ),
                    baseDecision(testChild_3)
                        .copy(
                            headOfFamilyId = testAdult_3.id,
                            serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed()
                        ),
                    baseDecision(testChild_4)
                        .copy(
                            headOfFamilyId = testAdult_4.id,
                            serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                        )
                )
            )
        }

        val result =
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams =
                        listOf(VoucherValueDecisionDistinctiveParams.UNCONFIRMED_HOURS)
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactlyInAnyOrder(
                Tuple(testAdult_2.lastName, testAdult_2.firstName),
                Tuple(testAdult_3.lastName, testAdult_3.firstName)
            )
    }

    @Test
    fun `search with EXTERNAL_CHILD`() {
        db.transaction { tx ->
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
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1).copy(headOfFamilyId = testAdult_1.id),
                    baseDecision(testChild_2).copy(headOfFamilyId = testAdult_2.id),
                    baseDecision(testChild_3).copy(headOfFamilyId = testAdult_3.id),
                    baseDecision(testChild_4).copy(headOfFamilyId = testAdult_4.id)
                )
            )
        }

        val result =
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams = listOf(VoucherValueDecisionDistinctiveParams.EXTERNAL_CHILD)
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactly(Tuple(testAdult_4.lastName, testAdult_4.firstName))
    }

    @Test
    fun `search with RETROACTIVE`() {
        val testPeriod1 = FiniteDateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))
        val testPeriod2 = FiniteDateRange(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 30))
        db.transaction { tx ->
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
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(
                            headOfFamilyId = testAdult_1.id,
                            validFrom = testPeriod1.start,
                            validTo = testPeriod1.end
                        ),
                    baseDecision(testChild_2)
                        .copy(
                            headOfFamilyId = testAdult_2.id,
                            validFrom = testPeriod1.start,
                            validTo = testPeriod1.end
                        ),
                    baseDecision(testChild_3)
                        .copy(
                            headOfFamilyId = testAdult_3.id,
                            validFrom = testPeriod2.start,
                            validTo = testPeriod2.end
                        ),
                    baseDecision(testChild_4)
                        .copy(
                            headOfFamilyId = testAdult_4.id,
                            validFrom = testPeriod2.start,
                            validTo = testPeriod2.end
                        )
                )
            )
        }

        val result =
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod2.start, LocalTime.of(15, 6))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams = listOf(VoucherValueDecisionDistinctiveParams.RETROACTIVE)
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactlyInAnyOrder(
                Tuple(testAdult_1.lastName, testAdult_1.firstName),
                Tuple(testAdult_2.lastName, testAdult_2.firstName)
            )
    }

    @Test
    fun `search - sort by child`() {
        db.transaction { tx ->
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
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("0bcbeb16-57b2-49ff-b1f3-e234770d693c")
                                ),
                            headOfFamilyId = testAdult_1.id
                        ),
                    baseDecision(testChild_2)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("cbe1e6a2-47a2-4291-8881-6991714e266f")
                                ),
                            headOfFamilyId = testAdult_2.id
                        ),
                    baseDecision(testChild_3).copy(headOfFamilyId = testAdult_3.id),
                    baseDecision(testChild_4).copy(headOfFamilyId = testAdult_4.id)
                )
            )
        }

        val sortByChild = { direction: SortDirection ->
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.CHILD,
                    sortDirection = direction,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams = emptyList()
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(testChild_1.lastName, testChild_1.firstName),
                Tuple(testChild_2.lastName, testChild_2.firstName),
                Tuple(testChild_4.lastName, testChild_4.firstName),
                Tuple(testChild_3.lastName, testChild_3.firstName)
            )
        assertThat(sortByChild(SortDirection.ASC).data)
            .extracting({ it.child.lastName }, { it.child.firstName })
            .containsExactlyElementsOf(expectedAsc)
        assertThat(sortByChild(SortDirection.DESC).data)
            .extracting({ it.child.lastName }, { it.child.firstName })
            .containsExactlyElementsOf(expectedAsc.reversed())
    }

    @Test
    fun `search - sort by validity`() {
        db.transaction { tx ->
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
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(
                            headOfFamilyId = testAdult_1.id,
                            validFrom = LocalDate.of(2022, 10, 22),
                            validTo = LocalDate.of(2022, 10, 22)
                        ),
                    baseDecision(testChild_2)
                        .copy(
                            headOfFamilyId = testAdult_2.id,
                            validFrom = LocalDate.of(2022, 9, 22),
                            validTo = LocalDate.of(2022, 10, 22)
                        ),
                    baseDecision(testChild_3)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("0bcbeb16-57b2-49ff-b1f3-e234770d693c")
                                ),
                            headOfFamilyId = testAdult_3.id,
                            validFrom = LocalDate.of(2022, 8, 22),
                            validTo = LocalDate.of(2022, 9, 22)
                        ),
                    baseDecision(testChild_4)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("cbe1e6a2-47a2-4291-8881-6991714e266f")
                                ),
                            headOfFamilyId = testAdult_4.id,
                            validFrom = LocalDate.of(2022, 8, 22),
                            validTo = LocalDate.of(2022, 12, 31)
                        )
                )
            )
        }

        val sortByValidity = { direction: SortDirection ->
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.VALIDITY,
                    sortDirection = direction,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams = emptyList()
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(testChild_3.lastName, testChild_3.firstName, LocalDate.of(2022, 8, 22)),
                Tuple(testChild_4.lastName, testChild_4.firstName, LocalDate.of(2022, 8, 22)),
                Tuple(testChild_2.lastName, testChild_2.firstName, LocalDate.of(2022, 9, 22)),
                Tuple(testChild_1.lastName, testChild_1.firstName, LocalDate.of(2022, 10, 22))
            )
        assertThat(sortByValidity(SortDirection.ASC).data)
            .extracting({ it.child.lastName }, { it.child.firstName }, { it.validFrom })
            .containsExactlyElementsOf(expectedAsc)
        assertThat(sortByValidity(SortDirection.DESC).data)
            .extracting({ it.child.lastName }, { it.child.firstName }, { it.validFrom })
            .containsExactlyElementsOf(expectedAsc.reversed())
    }

    @Test
    fun `search - sort by voucher value`() {
        db.transaction { tx ->
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
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(headOfFamilyId = testAdult_1.id, voucherValue = 29100),
                    baseDecision(testChild_2)
                        .copy(
                            headOfFamilyId = testAdult_2.id,
                            baseCoPayment = 0,
                            coPayment = 0,
                            finalCoPayment = 0,
                            voucherValue = 0
                        ),
                    baseDecision(testChild_3)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("0bcbeb16-57b2-49ff-b1f3-e234770d693c")
                                ),
                            headOfFamilyId = testAdult_3.id,
                            voucherValue = 64400
                        ),
                    baseDecision(testChild_4)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("cbe1e6a2-47a2-4291-8881-6991714e266f")
                                ),
                            headOfFamilyId = testAdult_4.id,
                            voucherValue = 64400
                        )
                )
            )
        }

        val sortByVoucherValue = { direction: SortDirection ->
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.VOUCHER_VALUE,
                    sortDirection = direction,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams = emptyList()
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(testChild_2.lastName, testChild_2.firstName, 0),
                Tuple(testChild_1.lastName, testChild_1.firstName, 29100),
                Tuple(testChild_3.lastName, testChild_3.firstName, 64400),
                Tuple(testChild_4.lastName, testChild_4.firstName, 64400)
            )
        assertThat(sortByVoucherValue(SortDirection.ASC).data)
            .extracting({ it.child.lastName }, { it.child.firstName }, { it.voucherValue })
            .containsExactlyElementsOf(expectedAsc)
        assertThat(sortByVoucherValue(SortDirection.DESC).data)
            .extracting({ it.child.lastName }, { it.child.firstName }, { it.voucherValue })
            .containsExactlyElementsOf(expectedAsc.reversed())
    }

    @Test
    fun `search - sort by final co payment`() {
        db.transaction { tx ->
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
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(headOfFamilyId = testAdult_1.id, finalCoPayment = 21300),
                    baseDecision(testChild_2)
                        .copy(headOfFamilyId = testAdult_2.id, finalCoPayment = 12300),
                    baseDecision(testChild_3)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("0bcbeb16-57b2-49ff-b1f3-e234770d693c")
                                ),
                            headOfFamilyId = testAdult_3.id,
                            finalCoPayment = 55500
                        ),
                    baseDecision(testChild_4)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("cbe1e6a2-47a2-4291-8881-6991714e266f")
                                ),
                            headOfFamilyId = testAdult_4.id,
                            finalCoPayment = 55500
                        )
                )
            )
        }

        val sortByFinalCoPayment = { direction: SortDirection ->
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.FINAL_CO_PAYMENT,
                    sortDirection = direction,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams = emptyList()
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(testChild_2.lastName, testChild_2.firstName, 12300),
                Tuple(testChild_1.lastName, testChild_1.firstName, 21300),
                Tuple(testChild_3.lastName, testChild_3.firstName, 55500),
                Tuple(testChild_4.lastName, testChild_4.firstName, 55500)
            )
        assertThat(sortByFinalCoPayment(SortDirection.ASC).data)
            .extracting({ it.child.lastName }, { it.child.firstName }, { it.finalCoPayment })
            .containsExactlyElementsOf(expectedAsc)
        assertThat(sortByFinalCoPayment(SortDirection.DESC).data)
            .extracting({ it.child.lastName }, { it.child.firstName }, { it.finalCoPayment })
            .containsExactlyElementsOf(expectedAsc.reversed())
    }

    @Test
    fun `search - sort by number`() {
        db.transaction { tx ->
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
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(headOfFamilyId = testAdult_1.id, decisionNumber = 202203),
                    baseDecision(testChild_2)
                        .copy(headOfFamilyId = testAdult_2.id, decisionNumber = 202201),
                    baseDecision(testChild_3)
                        .copy(headOfFamilyId = testAdult_3.id, decisionNumber = 202312),
                    baseDecision(testChild_4)
                        .copy(headOfFamilyId = testAdult_4.id, decisionNumber = 202204)
                )
            )
        }

        val sortByNumber = { direction: SortDirection ->
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.NUMBER,
                    sortDirection = direction,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams = emptyList()
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(testChild_2.lastName, testChild_2.firstName, 202201L),
                Tuple(testChild_1.lastName, testChild_1.firstName, 202203L),
                Tuple(testChild_4.lastName, testChild_4.firstName, 202204L),
                Tuple(testChild_3.lastName, testChild_3.firstName, 202312L)
            )
        assertThat(sortByNumber(SortDirection.ASC).data)
            .extracting({ it.child.lastName }, { it.child.firstName }, { it.decisionNumber })
            .containsExactlyElementsOf(expectedAsc)
        assertThat(sortByNumber(SortDirection.DESC).data)
            .extracting({ it.child.lastName }, { it.child.firstName }, { it.decisionNumber })
            .containsExactlyElementsOf(expectedAsc.reversed())
    }

    @Test
    fun `search - sort by created`() {
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
                serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
            )
        }
        listOf(
                baseDecision(testChild_1).copy(headOfFamilyId = testAdult_1.id),
                baseDecision(testChild_2).copy(headOfFamilyId = testAdult_2.id),
                baseDecision(testChild_3).copy(headOfFamilyId = testAdult_3.id),
                baseDecision(testChild_4).copy(headOfFamilyId = testAdult_4.id)
            )
            .forEach { decision ->
                db.transaction { tx -> tx.upsertValueDecisions(listOf(decision)) }
                Thread.sleep(10)
            }

        val sortByCreated = { direction: SortDirection ->
            db.read { tx ->
                tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.CREATED,
                    sortDirection = direction,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams = emptyList()
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(testChild_1.lastName, testChild_1.firstName),
                Tuple(testChild_2.lastName, testChild_2.firstName),
                Tuple(testChild_3.lastName, testChild_3.firstName),
                Tuple(testChild_4.lastName, testChild_4.firstName)
            )
        assertThat(sortByCreated(SortDirection.ASC).data)
            .extracting({ it.child.lastName }, { it.child.firstName })
            .containsExactlyElementsOf(expectedAsc)
        assertThat(sortByCreated(SortDirection.DESC).data)
            .extracting({ it.child.lastName }, { it.child.firstName })
            .containsExactlyElementsOf(expectedAsc.reversed())
    }

    @Test
    fun `getHeadOfFamilyVoucherValueDecisions`() {
        db.transaction { tx ->
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
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1).copy(headOfFamilyId = testAdult_1.id),
                    baseDecision(testChild_2).copy(headOfFamilyId = testAdult_1.id),
                    baseDecision(testChild_3).copy(headOfFamilyId = testAdult_2.id)
                )
            )
        }

        val decisions = db.read { tx -> tx.getHeadOfFamilyVoucherValueDecisions(testAdult_1.id) }

        assertThat(decisions)
            .extracting(
                { it.headOfFamily.lastName },
                { it.headOfFamily.firstName },
                { it.child.dateOfBirth }
            )
            .containsExactlyInAnyOrder(
                Tuple(testAdult_1.lastName, testAdult_1.firstName, testChild_1.dateOfBirth),
                Tuple(testAdult_1.lastName, testAdult_1.firstName, testChild_2.dateOfBirth)
            )
    }

    @Test
    fun `search with NO_STARTING_CHILDREN`() {
        val now = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

        db.transaction { tx ->
            tx.upsertValueDecisions(
                listOf(
                    createVoucherValueDecisionFixture(
                        status = VoucherValueDecisionStatus.DRAFT,
                        validFrom = now.today(),
                        validTo = now.today(),
                        headOfFamilyId = testAdult_1.id,
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        unitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed()
                    )
                )
            )

            assertEquals(
                1,
                tx.searchValueDecisions(
                        evakaClock =
                            MockEvakaClock(HelsinkiDateTime.of(now.today(), LocalTime.of(15, 6))),
                        postOffice = "ESPOO",
                        page = 0,
                        pageSize = 100,
                        sortBy = VoucherValueDecisionSortParam.HEAD_OF_FAMILY,
                        sortDirection = SortDirection.ASC,
                        statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                        areas = listOf(testArea.shortName),
                        unit = null,
                        startDate = null,
                        endDate = null,
                        financeDecisionHandlerId = null,
                        difference = emptySet(),
                        distinctiveParams =
                            listOf(VoucherValueDecisionDistinctiveParams.NO_STARTING_PLACEMENTS)
                    )
                    .data
                    .size
            )

            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = now.today(),
                    endDate = now.today()
                )
            )

            assertEquals(
                0,
                tx.searchValueDecisions(
                        evakaClock =
                            MockEvakaClock(HelsinkiDateTime.of(now.today(), LocalTime.of(15, 6))),
                        postOffice = "ESPOO",
                        page = 0,
                        pageSize = 100,
                        sortBy = VoucherValueDecisionSortParam.HEAD_OF_FAMILY,
                        sortDirection = SortDirection.ASC,
                        statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                        areas = emptyList(),
                        unit = null,
                        startDate = null,
                        endDate = null,
                        financeDecisionHandlerId = null,
                        difference = emptySet(),
                        distinctiveParams =
                            listOf(VoucherValueDecisionDistinctiveParams.NO_STARTING_PLACEMENTS)
                    )
                    .data
                    .size
            )
        }
    }

    @Test
    fun `search with status`() {
        db.transaction { tx ->
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
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed()
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(
                            headOfFamilyId = testAdult_1.id,
                            status = VoucherValueDecisionStatus.DRAFT
                        ),
                    baseDecision(testChild_2)
                        .copy(
                            headOfFamilyId = testAdult_2.id,
                            status = VoucherValueDecisionStatus.SENT
                        ),
                    baseDecision(testChild_3)
                        .copy(
                            headOfFamilyId = testAdult_3.id,
                            status = VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING
                        )
                )
            )
        }

        assertEquals(3, searchWithStatuses(emptyList()).size)
        assertEquals(
            2,
            searchWithStatuses(
                    listOf(VoucherValueDecisionStatus.DRAFT, VoucherValueDecisionStatus.SENT)
                )
                .size
        )
        assertEquals(
            1,
            searchWithStatuses(listOf(VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING)).size
        )
    }

    private fun searchWithStatuses(
        statuses: List<VoucherValueDecisionStatus>
    ): List<VoucherValueDecisionSummary> {
        return db.read { tx ->
            tx.searchValueDecisions(
                    evakaClock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(12, 11))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = VoucherValueDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                    statuses = statuses,
                    areas = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet(),
                    distinctiveParams = emptyList()
                )
                .data
        }
    }
}
