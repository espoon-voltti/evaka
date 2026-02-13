// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.deleteFosterParentRelationship
import fi.espoo.evaka.pis.deleteParentship
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevFridgeChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

class MissingHeadOfFamilyReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var missingHeadOfFamilyReportController: MissingHeadOfFamilyReportController

    private val startDate: LocalDate = LocalDate.now()
    private val clock = MockEvakaClock(2024, 1, 3, 3, 7)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id, name = "Test Daycare 2")
    private val employee = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adult = DevPerson()
    private val child1 = DevPerson(firstName = "Ricky", lastName = "Doe")
    private val child2 = DevPerson(firstName = "Micky", lastName = "Doe")

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(employee)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
        }
    }

    @Test
    fun `children without placement are not listed`() {
        assertEquals(listOf(), getReport(startDate, startDate.plusDays(7)))
    }

    @Test
    fun `has a head of family for the whole duration of placements`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = startDate.plusDays(3),
                )
            )
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate.plusDays(6),
                    endDate = startDate.plusDays(9),
                )
            )
        }

        //  pppp  pppp
        //  hhhhhhhhhh
        test(headPeriods = listOf(0 to 9), expected = listOf())

        //  pppp  pppp
        // hhhhhhhhhhh
        test(headPeriods = listOf(-1 to 9), expected = listOf())

        //  pppp  pppp
        //  hhhhhhhhhhh
        test(headPeriods = listOf(0 to 10), expected = listOf())

        //  pppp  pppp
        // hhhhhhhhhhhh
        test(headPeriods = listOf(-1 to 10), expected = listOf())

        //  pppp  pppp
        //  hhhh  hhhh
        test(headPeriods = listOf(0 to 3, 6 to 9), expected = listOf())
    }

    @Test
    fun `has a head of family for a part of placements`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = startDate.plusDays(3),
                )
            )
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate.plusDays(6),
                    endDate = startDate.plusDays(9),
                )
            )
        }

        //  pppp  pppp
        //  h
        test(headPeriods = listOf(0 to 0), expected = listOf(1 to 3, 6 to 9))

        //  pppp  pppp
        //           h
        test(headPeriods = listOf(9 to 9), expected = listOf(0 to 3, 6 to 8))

        //  pppp  pppp
        //  hhhhhhhh
        test(headPeriods = listOf(0 to 7), expected = listOf(8 to 9))

        //  pppp  pppp
        //    hhhhhhhh
        test(headPeriods = listOf(3 to 9), expected = listOf(0 to 2))

        //  pppp  pppp
        //     hhh
        test(headPeriods = listOf(3 to 5), expected = listOf(0 to 2, 6 to 9))

        //  pppp  pppp
        //      hhh
        test(headPeriods = listOf(4 to 6), expected = listOf(0 to 3, 7 to 9))
    }

    @Test
    fun `has a foster parent for the whole duration of placements`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = startDate.plusDays(3),
                )
            )
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate.plusDays(6),
                    endDate = startDate.plusDays(9),
                )
            )
        }

        //  pppp  pppp
        //  ffffffffff
        test(fosterPeriods = listOf(0 to 9), expected = listOf())

        //  pppp  pppp
        // fffffffffff
        test(fosterPeriods = listOf(-1 to 9), expected = listOf())

        //  pppp  pppp
        //  fffffffffff
        test(fosterPeriods = listOf(0 to 10), expected = listOf())

        //  pppp  pppp
        // ffffffffffff
        test(fosterPeriods = listOf(-1 to 10), expected = listOf())

        //  pppp  pppp
        //  ffff  ffff
        test(fosterPeriods = listOf(0 to 3, 6 to 9), expected = listOf())
    }

    @Test
    fun `has a foster parent for a part of placements`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = startDate.plusDays(3),
                )
            )
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate.plusDays(6),
                    endDate = startDate.plusDays(9),
                )
            )
        }

        //  pppp  pppp
        //  f
        test(fosterPeriods = listOf(0 to 0), expected = listOf(1 to 3, 6 to 9))

        //  pppp  pppp
        //           f
        test(fosterPeriods = listOf(9 to 9), expected = listOf(0 to 3, 6 to 8))

        //  pppp  pppp
        //  ffffffff
        test(fosterPeriods = listOf(0 to 7), expected = listOf(8 to 9))

        //  pppp  pppp
        //    ffffffff
        test(fosterPeriods = listOf(3 to 9), expected = listOf(0 to 2))

        //  pppp  pppp
        //     fff
        test(fosterPeriods = listOf(3 to 5), expected = listOf(0 to 2, 6 to 9))

        //  pppp  pppp
        //      fff
        test(fosterPeriods = listOf(4 to 6), expected = listOf(0 to 3, 7 to 9))
    }

    @Test
    fun `mixed head of family and foster parent`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = startDate.plusDays(3),
                )
            )
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate.plusDays(6),
                    endDate = startDate.plusDays(9),
                )
            )
        }

        //  pppp  pppp
        //  h
        //           f
        test(
            headPeriods = listOf(0 to 0),
            fosterPeriods = listOf(9 to 9),
            expected = listOf(1 to 3, 6 to 8),
        )

        //  pppp  pppp
        //           f
        //  h
        test(
            headPeriods = listOf(9 to 9),
            fosterPeriods = listOf(0 to 0),
            expected = listOf(1 to 3, 6 to 8),
        )

        //  pppp  pppp
        //  hhhhh
        //       fff
        test(
            headPeriods = listOf(0 to 4),
            fosterPeriods = listOf(5 to 7),
            expected = listOf(8 to 9),
        )

        //  pppp  pppp
        //         hhh
        //    fffff
        test(
            headPeriods = listOf(7 to 9),
            fosterPeriods = listOf(2 to 6),
            expected = listOf(0 to 1),
        )

        //  pppp  pppp
        //      h
        //     fff
        test(
            headPeriods = listOf(4 to 4),
            fosterPeriods = listOf(3 to 5),
            expected = listOf(0 to 2, 6 to 9),
        )

        //  pppp  pppp
        //  h h h h h
        //   f f f f f
        test(
            headPeriods = listOf(0 to 0, 2 to 2, 4 to 4, 6 to 6, 8 to 8),
            fosterPeriods = listOf(1 to 1, 3 to 3, 5 to 5, 7 to 7, 9 to 9),
            expected = listOf(),
        )
    }

    private fun test(
        headPeriods: List<Pair<Int, Int>> = listOf(),
        fosterPeriods: List<Pair<Int, Int>> = listOf(),
        expected: List<Pair<Int, Int>>,
    ) {
        val headIds =
            db.transaction { tx ->
                headPeriods.map { (start, end) ->
                    tx.insert(
                        DevFridgeChild(
                            childId = child1.id,
                            headOfChild = adult.id,
                            startDate = startDate.plusDays(start.toLong()),
                            endDate = startDate.plusDays(end.toLong()),
                        )
                    )
                }
            }
        val fosterIds =
            db.transaction { tx ->
                fosterPeriods.map { (start, end) ->
                    tx.insert(
                        DevFosterParent(
                            childId = child1.id,
                            parentId = adult.id,
                            validDuring =
                                DateRange(
                                    startDate.plusDays(start.toLong()),
                                    startDate.plusDays(end.toLong()),
                                ),
                            modifiedAt = clock.now(),
                            modifiedBy = employee.evakaUserId,
                        )
                    )
                }
            }
        assertEquals(
            if (expected.isEmpty()) listOf()
            else
                listOf(
                    toReportRow(
                        child1,
                        expected.map { (start, end) ->
                            FiniteDateRange(
                                startDate.plusDays(start.toLong()),
                                startDate.plusDays(end.toLong()),
                            )
                        },
                    )
                ),
            getReport(startDate.minusDays(10), startDate.plusDays(10)),
        )
        db.transaction { tx ->
            headIds.forEach { tx.deleteParentship(it) }
            fosterIds.forEach { tx.deleteFosterParentRelationship(it) }
        }
    }

    @Test
    fun `child with placement without head of family is shown`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = startDate,
                )
            )
        }

        assertEquals(
            listOf(toReportRow(child1, listOf(FiniteDateRange(startDate, startDate)))),
            getReport(startDate, startDate),
        )
    }

    @Test
    fun `child with placement who is deceased is not shown`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = startDate,
                )
            )
            it.execute { sql("UPDATE person set date_of_death = ${bind(startDate.minusDays(1))}") }
        }

        assertEquals(listOf(), getReport(startDate, startDate))
    }

    @Test
    fun `child with head of family for each day is not shown`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = startDate,
                )
            )
            it.insert(
                DevFridgeChild(
                    childId = child1.id,
                    startDate = startDate,
                    endDate = startDate,
                    headOfChild = adult.id,
                )
            )
        }

        assertEquals(listOf(), getReport(startDate, startDate))
    }

    @Test
    fun `works with multiple children`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = startDate,
                )
            )
            it.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare2.id,
                    startDate = startDate,
                    endDate = startDate,
                )
            )
        }

        assertEquals(
            listOf(
                    toReportRow(child1, listOf(FiniteDateRange(startDate, startDate))),
                    toReportRow(child2, listOf(FiniteDateRange(startDate, startDate))),
                )
                .sortedWith(compareBy({ it.lastName }, { it.firstName })),
            getReport(startDate, startDate),
        )
    }

    private fun getReport(
        from: LocalDate,
        to: LocalDate? = null,
    ): List<MissingHeadOfFamilyReportRow> =
        missingHeadOfFamilyReportController.getMissingHeadOfFamilyReport(
            dbInstance(),
            employee.user,
            RealEvakaClock(),
            from,
            to,
        )

    private fun toReportRow(child: DevPerson, rangesWithoutHead: List<FiniteDateRange>) =
        MissingHeadOfFamilyReportRow(
            childId = child.id,
            firstName = child.firstName,
            lastName = child.lastName,
            rangesWithoutHead = rangesWithoutHead,
        )
}
