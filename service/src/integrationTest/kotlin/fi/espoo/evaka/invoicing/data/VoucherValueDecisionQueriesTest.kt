// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.incomestatement.IncomeStatementBody
import fi.espoo.evaka.incomestatement.IncomeStatementStatus
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
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDefaultDaycare
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

    private val testPeriod = FiniteDateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)

    private val adult1 = DevPerson(firstName = "John", lastName = "Doe")
    private val adult2 = DevPerson(firstName = "Joan", lastName = "Doe")
    private val adult3 = DevPerson(firstName = "Mark", lastName = "Foo")
    private val adult4 = DevPerson(firstName = "Dork", lastName = "Aman")
    private val adult5 = DevPerson(firstName = "Johannes", lastName = "Karhula")
    private val adult6 = DevPerson(firstName = "Ville", lastName = "Vilkas")
    private val adult7 = DevPerson(firstName = "Tepi", lastName = "Turvakiellollinen")

    private val child1 =
        DevPerson(firstName = "Ricky", lastName = "Doe", dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 =
        DevPerson(firstName = "Micky", lastName = "Doe", dateOfBirth = LocalDate.of(2016, 3, 1))
    private val child3 =
        DevPerson(firstName = "Hillary", lastName = "Foo", dateOfBirth = LocalDate.of(2018, 9, 1))
    // postOffice = "Helsinki" makes this child "external" for EXTERNAL_CHILD filter
    private val child4 =
        DevPerson(
            firstName = "Maisa",
            lastName = "Farang",
            dateOfBirth = LocalDate.of(2019, 3, 2),
            postOffice = "Helsinki",
        )
    private val child5 =
        DevPerson(firstName = "Visa", lastName = "Virén", dateOfBirth = LocalDate.of(2018, 11, 13))

    private val decisionMaker = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            listOf(adult1, adult2, adult3, adult4, adult5, adult6, adult7).forEach {
                tx.insert(it, DevPersonType.ADULT)
            }
            listOf(child1, child2, child3, child4, child5).forEach {
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
                        unitId = daycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                    )
                }
                tx.upsertValueDecisions(
                    listOf(
                        baseDecision(child1)
                            .copy(headOfFamilyId = adult1.id, headOfFamilyIncome = null),
                        baseDecision(child2)
                            .copy(
                                headOfFamilyId = adult2.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        data = emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false,
                                    ),
                            ),
                        baseDecision(child3)
                            .copy(
                                headOfFamilyId = adult3.id,
                                headOfFamilyIncome = null,
                                partnerId = adult4.id,
                                partnerIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        data = emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false,
                                    ),
                            ),
                        baseDecision(child4)
                            .copy(
                                headOfFamilyId = adult5.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.INCOME,
                                        data = emptyMap(),
                                        totalIncome = 200000,
                                        totalExpenses = 0,
                                        total = 200000,
                                        worksAtECHA = false,
                                    ),
                            ),
                        baseDecision(child5)
                            .copy(
                                headOfFamilyId = adult6.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.INCOME,
                                        data = emptyMap(),
                                        totalIncome = 200000,
                                        totalExpenses = 0,
                                        total = 200000,
                                        worksAtECHA = false,
                                    ),
                                partnerId = adult7.id,
                                partnerIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        data = emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false,
                                    ),
                            ),
                    )
                )
                val ids =
                    tx.createQuery { sql("SELECT id FROM voucher_value_decision") }
                        .toList<VoucherValueDecisionId>()
                ids.map { id -> tx.getVoucherValueDecision(id)!! }
            }
        assertThat(decisions)
            .extracting(
                { it.headOfFamily.lastName },
                { it.headOfFamily.firstName },
                { it.incomeEffect },
            )
            .containsExactlyInAnyOrder(
                Tuple(adult1.lastName, adult1.firstName, IncomeEffect.NOT_AVAILABLE),
                Tuple(adult2.lastName, adult2.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(adult3.lastName, adult3.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(adult5.lastName, adult5.firstName, IncomeEffect.INCOME),
                Tuple(adult6.lastName, adult6.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
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
                        listOf(VoucherValueDecisionDistinctiveParams.MAX_FEE_ACCEPTED),
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactly(
                Tuple(adult2.lastName, adult2.firstName),
                Tuple(adult3.lastName, adult3.firstName),
                Tuple(adult6.lastName, adult6.firstName),
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1).copy(headOfFamilyId = adult1.id, difference = emptySet()),
                    baseDecision(child2)
                        .copy(
                            headOfFamilyId = adult2.id,
                            difference = setOf(VoucherValueDecisionDifference.INCOME),
                        ),
                    baseDecision(child3)
                        .copy(
                            headOfFamilyId = adult3.id,
                            difference =
                                setOf(
                                    VoucherValueDecisionDifference.INCOME,
                                    VoucherValueDecisionDifference.FAMILY_SIZE,
                                ),
                        ),
                    baseDecision(child4)
                        .copy(
                            headOfFamilyId = adult4.id,
                            difference = setOf(VoucherValueDecisionDifference.PLACEMENT),
                        ),
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
                    distinctiveParams = emptyList(),
                )
            }

        assertThat(result.data)
            .extracting(
                { it.headOfFamily.lastName },
                { it.headOfFamily.firstName },
                { it.difference },
            )
            .containsExactly(
                Tuple(
                    adult2.lastName,
                    adult2.firstName,
                    setOf(VoucherValueDecisionDifference.INCOME),
                ),
                Tuple(
                    adult3.lastName,
                    adult3.firstName,
                    setOf(
                        VoucherValueDecisionDifference.INCOME,
                        VoucherValueDecisionDifference.FAMILY_SIZE,
                    ),
                ),
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1)
                        .copy(
                            headOfFamilyId = adult1.id,
                            serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                        ),
                    baseDecision(child2)
                        .copy(
                            headOfFamilyId = adult2.id,
                            serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
                        ),
                    baseDecision(child3)
                        .copy(
                            headOfFamilyId = adult3.id,
                            serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
                        ),
                    baseDecision(child4)
                        .copy(
                            headOfFamilyId = adult4.id,
                            serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                        ),
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
                        listOf(VoucherValueDecisionDistinctiveParams.UNCONFIRMED_HOURS),
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactlyInAnyOrder(
                Tuple(adult2.lastName, adult2.firstName),
                Tuple(adult3.lastName, adult3.firstName),
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1).copy(headOfFamilyId = adult1.id),
                    baseDecision(child2).copy(headOfFamilyId = adult2.id),
                    baseDecision(child3).copy(headOfFamilyId = adult3.id),
                    baseDecision(child4).copy(headOfFamilyId = adult4.id),
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
                    distinctiveParams = listOf(VoucherValueDecisionDistinctiveParams.EXTERNAL_CHILD),
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactly(Tuple(adult4.lastName, adult4.firstName))
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1)
                        .copy(
                            headOfFamilyId = adult1.id,
                            validFrom = testPeriod1.start,
                            validTo = testPeriod1.end,
                        ),
                    baseDecision(child2)
                        .copy(
                            headOfFamilyId = adult2.id,
                            validFrom = testPeriod1.start,
                            validTo = testPeriod1.end,
                        ),
                    baseDecision(child3)
                        .copy(
                            headOfFamilyId = adult3.id,
                            validFrom = testPeriod2.start,
                            validTo = testPeriod2.end,
                        ),
                    baseDecision(child4)
                        .copy(
                            headOfFamilyId = adult4.id,
                            validFrom = testPeriod2.start,
                            validTo = testPeriod2.end,
                        ),
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
                    distinctiveParams = listOf(VoucherValueDecisionDistinctiveParams.RETROACTIVE),
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactlyInAnyOrder(
                Tuple(adult1.lastName, adult1.firstName),
                Tuple(adult2.lastName, adult2.firstName),
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("0bcbeb16-57b2-49ff-b1f3-e234770d693c")
                                ),
                            headOfFamilyId = adult1.id,
                        ),
                    baseDecision(child2)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("cbe1e6a2-47a2-4291-8881-6991714e266f")
                                ),
                            headOfFamilyId = adult2.id,
                        ),
                    baseDecision(child3).copy(headOfFamilyId = adult3.id),
                    baseDecision(child4).copy(headOfFamilyId = adult4.id),
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
                    distinctiveParams = emptyList(),
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(child1.lastName, child1.firstName),
                Tuple(child2.lastName, child2.firstName),
                Tuple(child4.lastName, child4.firstName),
                Tuple(child3.lastName, child3.firstName),
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1)
                        .copy(
                            headOfFamilyId = adult1.id,
                            validFrom = LocalDate.of(2022, 10, 22),
                            validTo = LocalDate.of(2022, 10, 22),
                        ),
                    baseDecision(child2)
                        .copy(
                            headOfFamilyId = adult2.id,
                            validFrom = LocalDate.of(2022, 9, 22),
                            validTo = LocalDate.of(2022, 10, 22),
                        ),
                    baseDecision(child3)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("0bcbeb16-57b2-49ff-b1f3-e234770d693c")
                                ),
                            headOfFamilyId = adult3.id,
                            validFrom = LocalDate.of(2022, 8, 22),
                            validTo = LocalDate.of(2022, 9, 22),
                        ),
                    baseDecision(child4)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("cbe1e6a2-47a2-4291-8881-6991714e266f")
                                ),
                            headOfFamilyId = adult4.id,
                            validFrom = LocalDate.of(2022, 8, 22),
                            validTo = LocalDate.of(2022, 12, 31),
                        ),
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
                    distinctiveParams = emptyList(),
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(child3.lastName, child3.firstName, LocalDate.of(2022, 8, 22)),
                Tuple(child4.lastName, child4.firstName, LocalDate.of(2022, 8, 22)),
                Tuple(child2.lastName, child2.firstName, LocalDate.of(2022, 9, 22)),
                Tuple(child1.lastName, child1.firstName, LocalDate.of(2022, 10, 22)),
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1).copy(headOfFamilyId = adult1.id, voucherValue = 29100),
                    baseDecision(child2)
                        .copy(
                            headOfFamilyId = adult2.id,
                            baseCoPayment = 0,
                            coPayment = 0,
                            finalCoPayment = 0,
                            voucherValue = 0,
                        ),
                    baseDecision(child3)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("0bcbeb16-57b2-49ff-b1f3-e234770d693c")
                                ),
                            headOfFamilyId = adult3.id,
                            voucherValue = 64400,
                        ),
                    baseDecision(child4)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("cbe1e6a2-47a2-4291-8881-6991714e266f")
                                ),
                            headOfFamilyId = adult4.id,
                            voucherValue = 64400,
                        ),
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
                    distinctiveParams = emptyList(),
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(child2.lastName, child2.firstName, 0),
                Tuple(child1.lastName, child1.firstName, 29100),
                Tuple(child3.lastName, child3.firstName, 64400),
                Tuple(child4.lastName, child4.firstName, 64400),
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1).copy(headOfFamilyId = adult1.id, finalCoPayment = 21300),
                    baseDecision(child2).copy(headOfFamilyId = adult2.id, finalCoPayment = 12300),
                    baseDecision(child3)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("0bcbeb16-57b2-49ff-b1f3-e234770d693c")
                                ),
                            headOfFamilyId = adult3.id,
                            finalCoPayment = 55500,
                        ),
                    baseDecision(child4)
                        .copy(
                            id =
                                VoucherValueDecisionId(
                                    UUID.fromString("cbe1e6a2-47a2-4291-8881-6991714e266f")
                                ),
                            headOfFamilyId = adult4.id,
                            finalCoPayment = 55500,
                        ),
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
                    distinctiveParams = emptyList(),
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(child2.lastName, child2.firstName, 12300),
                Tuple(child1.lastName, child1.firstName, 21300),
                Tuple(child3.lastName, child3.firstName, 55500),
                Tuple(child4.lastName, child4.firstName, 55500),
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1).copy(headOfFamilyId = adult1.id, decisionNumber = 202203),
                    baseDecision(child2).copy(headOfFamilyId = adult2.id, decisionNumber = 202201),
                    baseDecision(child3).copy(headOfFamilyId = adult3.id, decisionNumber = 202312),
                    baseDecision(child4).copy(headOfFamilyId = adult4.id, decisionNumber = 202204),
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
                    distinctiveParams = emptyList(),
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(child2.lastName, child2.firstName, 202201L),
                Tuple(child1.lastName, child1.firstName, 202203L),
                Tuple(child4.lastName, child4.firstName, 202204L),
                Tuple(child3.lastName, child3.firstName, 202312L),
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
                unitId = daycare.id,
                placementType = PlacementType.DAYCARE,
                serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
            )
        }
        listOf(
                baseDecision(child1).copy(headOfFamilyId = adult1.id),
                baseDecision(child2).copy(headOfFamilyId = adult2.id),
                baseDecision(child3).copy(headOfFamilyId = adult3.id),
                baseDecision(child4).copy(headOfFamilyId = adult4.id),
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
                    distinctiveParams = emptyList(),
                )
            }
        }
        val expectedAsc =
            listOf(
                Tuple(child1.lastName, child1.firstName),
                Tuple(child2.lastName, child2.firstName),
                Tuple(child3.lastName, child3.firstName),
                Tuple(child4.lastName, child4.firstName),
            )
        assertThat(sortByCreated(SortDirection.ASC).data)
            .extracting({ it.child.lastName }, { it.child.firstName })
            .containsExactlyElementsOf(expectedAsc)
        assertThat(sortByCreated(SortDirection.DESC).data)
            .extracting({ it.child.lastName }, { it.child.firstName })
            .containsExactlyElementsOf(expectedAsc.reversed())
    }

    @Test
    fun getHeadOfFamilyVoucherValueDecisions() {
        db.transaction { tx ->
            val baseDecision = { child: DevPerson ->
                createVoucherValueDecisionFixture(
                    status = VoucherValueDecisionStatus.DRAFT,
                    validFrom = testPeriod.start,
                    validTo = testPeriod.end,
                    headOfFamilyId = PersonId(UUID.randomUUID()),
                    childId = child.id,
                    dateOfBirth = child.dateOfBirth,
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1).copy(headOfFamilyId = adult1.id),
                    baseDecision(child2).copy(headOfFamilyId = adult1.id),
                    baseDecision(child3).copy(headOfFamilyId = adult2.id),
                )
            )
        }

        val decisions = db.read { tx -> tx.getHeadOfFamilyVoucherValueDecisions(adult1.id) }

        assertThat(decisions)
            .extracting(
                { it.headOfFamily.lastName },
                { it.headOfFamily.firstName },
                { it.child.dateOfBirth },
            )
            .containsExactlyInAnyOrder(
                Tuple(adult1.lastName, adult1.firstName, child1.dateOfBirth),
                Tuple(adult1.lastName, adult1.firstName, child2.dateOfBirth),
            )
    }

    @Test
    fun `search with NO_STARTING_PLACEMENTS`() {
        val now = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

        db.transaction { tx ->
            tx.upsertValueDecisions(
                listOf(
                    createVoucherValueDecisionFixture(
                        status = VoucherValueDecisionStatus.DRAFT,
                        validFrom = now.today(),
                        validTo = now.today(),
                        headOfFamilyId = adult1.id,
                        childId = child1.id,
                        dateOfBirth = child1.dateOfBirth,
                        unitId = daycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
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
                        areas = listOf(area.shortName),
                        unit = null,
                        startDate = null,
                        endDate = null,
                        financeDecisionHandlerId = null,
                        difference = emptySet(),
                        distinctiveParams =
                            listOf(VoucherValueDecisionDistinctiveParams.NO_STARTING_PLACEMENTS),
                    )
                    .data
                    .size,
            )

            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = now.today(),
                    endDate = now.today(),
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
                            listOf(VoucherValueDecisionDistinctiveParams.NO_STARTING_PLACEMENTS),
                    )
                    .data
                    .size,
            )
        }
    }

    @Test
    fun `search with NO_OPEN_INCOME_STATEMENTS`() {
        val clock = RealEvakaClock()

        fun createTestDecision(headOfFamilyId: PersonId, childId: ChildId, partnerId: PersonId?) =
            createVoucherValueDecisionFixture(
                status = VoucherValueDecisionStatus.DRAFT,
                validFrom = clock.now().toLocalDate(),
                validTo = clock.now().toLocalDate(),
                headOfFamilyId = headOfFamilyId,
                partnerId = partnerId,
                childId = childId,
                dateOfBirth = child1.dateOfBirth,
                unitId = daycare.id,
                placementType = PlacementType.DAYCARE,
                serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
            )

        val decisionWithHandledStatement = createTestDecision(adult1.id, child1.id, adult2.id)
        val decisionWithOpenAdultStatement = createTestDecision(adult3.id, child2.id, adult4.id)
        val decisionWithFarAwayAndFutureOpenStatements =
            createTestDecision(adult5.id, child3.id, adult6.id)
        val decisionWithOpenChildStatement = createTestDecision(adult7.id, child4.id, null)

        db.transaction { tx ->
            tx.upsertValueDecisions(
                listOf(
                    decisionWithHandledStatement,
                    decisionWithOpenAdultStatement,
                    decisionWithFarAwayAndFutureOpenStatements,
                    decisionWithOpenChildStatement,
                )
            )
            tx.insert(decisionMaker)
            tx.insert(
                DevIncomeStatement(
                    personId = adult1.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.HANDLED,
                    handledAt = clock.now(),
                    handlerId = decisionMaker.id,
                )
            )

            // adult2 statement not submitted
            tx.insert(
                DevIncomeStatement(
                    personId = adult2.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.DRAFT,
                    sentAt = null,
                )
            )

            tx.insert(
                DevIncomeStatement(
                    personId = adult3.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.HANDLED,
                    handledAt = clock.now(),
                    handlerId = decisionMaker.id,
                )
            )

            // adult4 statement not handled
            tx.insert(
                DevIncomeStatement(
                    personId = adult4.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerId = null,
                )
            )

            // adult5 statement not handled
            tx.insert(
                DevIncomeStatement(
                    personId = adult5.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(20),
                            clock.today().minusMonths(14).minusDays(1),
                        ),
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerId = null,
                )
            )

            // adult6 statement not handled
            tx.insert(
                DevIncomeStatement(
                    personId = adult6.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().plusDays(1),
                            clock.today().plusMonths(12),
                        ),
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerId = null,
                )
            )

            tx.insert(
                DevIncomeStatement(
                    personId = adult7.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.HANDLED,
                    handledAt = clock.now(),
                    handlerId = decisionMaker.id,
                )
            )

            // child4 statement not handled
            tx.insert(
                DevIncomeStatement(
                    personId = child4.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerId = null,
                )
            )

            val result =
                tx.searchValueDecisions(
                    evakaClock = clock,
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
                    difference = emptySet(),
                    financeDecisionHandlerId = null,
                    distinctiveParams =
                        listOf(VoucherValueDecisionDistinctiveParams.NO_OPEN_INCOME_STATEMENTS),
                )

            assertThat(result.data.map { it.child.id })
                .containsExactlyInAnyOrder(child1.id, child3.id)
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
                    unitId = daycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
                )
            }
            tx.upsertValueDecisions(
                listOf(
                    baseDecision(child1)
                        .copy(
                            headOfFamilyId = adult1.id,
                            status = VoucherValueDecisionStatus.DRAFT,
                        ),
                    baseDecision(child2)
                        .copy(headOfFamilyId = adult2.id, status = VoucherValueDecisionStatus.SENT),
                    baseDecision(child3)
                        .copy(
                            headOfFamilyId = adult3.id,
                            status = VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                        ),
                )
            )
        }

        assertEquals(3, searchWithStatuses(emptyList()).size)
        assertEquals(
            2,
            searchWithStatuses(
                    listOf(VoucherValueDecisionStatus.DRAFT, VoucherValueDecisionStatus.SENT)
                )
                .size,
        )
        assertEquals(
            1,
            searchWithStatuses(listOf(VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING)).size,
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
                    distinctiveParams = emptyList(),
                )
                .data
        }
    }
}
