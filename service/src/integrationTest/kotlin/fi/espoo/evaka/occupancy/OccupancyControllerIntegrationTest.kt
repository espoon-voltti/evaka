// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.service.Caretakers
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.preschoolTerm2020
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevChildAttendance
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevStaffAttendance
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OccupancyControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var occupancyController: OccupancyController

    private val today = LocalDate.of(2024, 8, 21)
    private val mockClock =
        MockEvakaClock(HelsinkiDateTime.Companion.of(today, LocalTime.of(16, 0)))
    private val startDate = LocalDate.of(2019, 1, 1)
    private val endDate = LocalDate.of(2021, 12, 31)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            listOf(testChild_1, testChild_2, testChild_3).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insert(preschoolTerm2020)
            tx.insertServiceNeedOptions()
        }
    }

    @Test
    fun `No caretakers, cannot compute`() {
        val applicationId = createApplication(testChild_1.id)

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(startDate, endDate),
                preschoolDaycarePeriod = null,
            )

        assertEquals(OccupancyResponseSpeculated(null, null, null, null), occupancies)
    }

    @Test
    fun `No previous placements`() {
        db.transaction { tx ->
            val groupId = tx.insert(DevDaycareGroup(daycareId = testDaycare.id))
            tx.insert(
                DevDaycareCaretaker(
                    groupId = groupId,
                    amount = 1.0.toBigDecimal(),
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val applicationId = createApplication(testChild_1.id)

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(startDate, endDate),
                preschoolDaycarePeriod = null,
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 0.0,
                        sumOver3y = 0.0,
                        headcount = 0,
                        caretakers = 1.0,
                        percentage = 0.0,
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 0.0,
                        sumOver3y = 0.0,
                        headcount = 0,
                        caretakers = 1.0,
                        percentage = 0.0,
                    ),
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0,
                    ),
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0,
                    ),
            ),
            occupancies,
        )
    }

    @Test
    fun `Has previous placements`() {
        db.transaction { tx ->
            val groupId = tx.insert(DevDaycareGroup(daycareId = testDaycare.id))
            tx.insert(
                DevDaycareCaretaker(
                    groupId = groupId,
                    amount = 1.0.toBigDecimal(),
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Under 3 years (coefficient 1.75)
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = startDate.plusMonths(4),
                    endDate = endDate,
                )
            )
        }

        // Under 3 years (coefficient 1.75)
        val applicationId = createApplication(testChild_1.id)

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(startDate, endDate),
                preschoolDaycarePeriod = null,
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0,
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3,
                    ),
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 3.5,
                        sumOver3y = 0.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 50.0,
                    ),
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 3.5,
                        sumOver3y = 1.0,
                        headcount = 3,
                        caretakers = 1.0,
                        percentage = 64.3,
                    ),
            ),
            occupancies,
        )
    }

    @Test
    fun `Preschool speculation`() {
        db.transaction { tx ->
            val groupId = tx.insert(DevDaycareGroup(daycareId = testDaycare.id))
            tx.insert(
                DevDaycareCaretaker(
                    groupId = groupId,
                    amount = 1.0.toBigDecimal(),
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Under 3 years (coefficient 1.75)
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2021, 7, 1),
                    endDate = endDate,
                )
            )
        }

        // Over 3 years (coefficient 1)
        val applicationId =
            createApplication(
                testChild_2.id,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = false,
            )

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 6, 4)),
                preschoolDaycarePeriod = null,
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0,
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3,
                    ),
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.5,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 32.1,
                    ),
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3,
                    ),
            ),
            occupancies,
        )
    }

    @Test
    fun `Preschool and connected daycare speculation`() {
        db.transaction { tx ->
            val groupId = tx.insert(DevDaycareGroup(daycareId = testDaycare.id))
            tx.insert(
                DevDaycareCaretaker(
                    groupId = groupId,
                    amount = 1.0.toBigDecimal(),
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Under 3 years (coefficient 1.75)
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2021, 7, 1),
                    endDate = endDate,
                )
            )
        }

        // Over 3 years (coefficient 1)
        val applicationId =
            createApplication(
                testChild_2.id,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
            )

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 6, 4)),
                preschoolDaycarePeriod =
                    FiniteDateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 7, 31)),
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0,
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3,
                    ),
                // preschool only
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.5,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 32.1,
                    ),
                // preschool+daycare
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 2.0,
                        headcount = 3,
                        caretakers = 1.0,
                        percentage = 53.6,
                    ),
            ),
            occupancies,
        )
    }

    @Test
    fun `Preschool and connected daycare speculation, child already has a placement`() {
        db.transaction { tx ->
            val groupId = tx.insert(DevDaycareGroup(daycareId = testDaycare.id))
            tx.insert(
                DevDaycareCaretaker(
                    groupId = groupId,
                    amount = 1.0.toBigDecimal(),
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Under 3 years (coefficient 1.75)
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2021, 7, 1),
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), will be speculated for preschool
            tx.insert(
                DevPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }

        val applicationId =
            createApplication(
                testChild_2.id,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true,
            )

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 6, 4)),
                preschoolDaycarePeriod =
                    FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 7, 31)),
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3,
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 2.0,
                        headcount = 3,
                        caretakers = 1.0,
                        percentage = 53.6,
                    ),
                // Same occupancy values as above, because the child already had a placement in this
                // unit
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3,
                    ),
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 2.0,
                        headcount = 3,
                        caretakers = 1.0,
                        percentage = 53.6,
                    ),
            ),
            occupancies,
        )
    }

    @Test
    fun `getUnitOccupancies returns correct data`() {
        val start = today.minusDays(2)
        val end = today.plusDays(2)
        val group1 = DevDaycareGroup(daycareId = testDaycare.id, name = "g1")
        val group2 = DevDaycareGroup(daycareId = testDaycare.id, name = "g2")
        val caretakers1 =
            DevDaycareCaretaker(groupId = group1.id, amount = BigDecimal("2.0"), startDate = start)
        val caretakers2 =
            DevDaycareCaretaker(groupId = group2.id, amount = BigDecimal("1.0"), startDate = start)

        val placement1 =
            DevPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = start,
                endDate = end
            )
        val groupPlacement1a =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = group1.id,
                startDate = start,
                endDate = start.plusDays(2)
            )
        val groupPlacement1b =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = group2.id,
                startDate = start.plusDays(3),
                endDate = end
            )
        val placement2 =
            DevPlacement(
                childId = testChild_2.id,
                type = PlacementType.PRESCHOOL,
                unitId = testDaycare.id,
                startDate = start.plusDays(1),
                endDate = end
            )
        val groupPlacement2 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement2.id,
                daycareGroupId = group1.id,
                startDate = start.plusDays(1),
                endDate = end
            )
        val placement3 =
            DevPlacement(
                childId = testChild_3.id,
                unitId = testDaycare.id,
                startDate = start,
                endDate = end.minusDays(1)
            )
        val groupPlacement3 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement3.id,
                daycareGroupId = group2.id,
                startDate = start,
                endDate = end.minusDays(1)
            )

        db.transaction { tx ->
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(caretakers1)
            tx.insert(caretakers2)
            tx.insert(placement1)
            tx.insert(groupPlacement1a)
            tx.insert(groupPlacement1b)
            tx.insert(placement2)
            tx.insert(groupPlacement2)
            tx.insert(placement3)
            tx.insert(groupPlacement3)
        }

        val occupanciesForUnit = getUnitOccupancies(start, end, groupId = null)
        assertEquals(
            UnitOccupancies(
                caretakers = Caretakers(minimum = 3.0, maximum = 3.0),
                confirmed =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period =
                                        FiniteDateRange(today.minusDays(2), today.minusDays(2)),
                                    sum = 2.0,
                                    headcount = 2,
                                    caretakers = 3.0,
                                    percentage = 9.5
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                    sum = 2.5,
                                    headcount = 3,
                                    caretakers = 3.0,
                                    percentage = 11.9
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = 3.0,
                                    percentage = 7.1
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                sum = 2.5,
                                headcount = 3,
                                caretakers = 3.0,
                                percentage = 11.9
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                sum = 1.5,
                                headcount = 2,
                                caretakers = 3.0,
                                percentage = 7.1
                            )
                    ),
                planned =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period =
                                        FiniteDateRange(today.minusDays(2), today.minusDays(2)),
                                    sum = 2.0,
                                    headcount = 2,
                                    caretakers = 3.0,
                                    percentage = 9.5
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                    sum = 2.5,
                                    headcount = 3,
                                    caretakers = 3.0,
                                    percentage = 11.9
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = 3.0,
                                    percentage = 7.1
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                sum = 2.5,
                                headcount = 3,
                                caretakers = 3.0,
                                percentage = 11.9
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                sum = 1.5,
                                headcount = 2,
                                caretakers = 3.0,
                                percentage = 7.1
                            )
                    ),
                realized =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period =
                                        FiniteDateRange(today.minusDays(2), today.minusDays(2)),
                                    sum = 2.0,
                                    headcount = 2,
                                    // note: realized caretakers are read from staff_attendance or
                                    // staff_attendance_realtime tables which have no data here
                                    caretakers = null,
                                    percentage = null
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today),
                                    sum = 2.5,
                                    headcount = 3,
                                    caretakers = null,
                                    percentage = null
                                )
                            ),
                        max = null,
                        min = null
                    ),
            ),
            occupanciesForUnit
        )

        val occupanciesForGroup1 = getUnitOccupancies(start, end, groupId = group1.id)
        assertEquals(
            UnitOccupancies(
                caretakers = Caretakers(minimum = 3.0, maximum = 3.0),
                confirmed =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period =
                                        FiniteDateRange(today.minusDays(2), today.minusDays(2)),
                                    sum = 1.0,
                                    headcount = 1,
                                    caretakers = 2.0,
                                    percentage = 7.1
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = 2.0,
                                    percentage = 10.7
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                    sum = 0.5,
                                    headcount = 1,
                                    caretakers = 2.0,
                                    percentage = 3.6
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today),
                                sum = 1.5,
                                headcount = 2,
                                caretakers = 2.0,
                                percentage = 10.7
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                sum = 0.5,
                                headcount = 1,
                                caretakers = 2.0,
                                percentage = 3.6
                            )
                    ),
                planned =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period =
                                        FiniteDateRange(today.minusDays(2), today.minusDays(2)),
                                    sum = 1.0,
                                    headcount = 1,
                                    caretakers = 2.0,
                                    percentage = 7.1
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = 2.0,
                                    percentage = 10.7
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                    sum = 0.5,
                                    headcount = 1,
                                    caretakers = 2.0,
                                    percentage = 3.6
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today),
                                sum = 1.5,
                                headcount = 2,
                                caretakers = 2.0,
                                percentage = 10.7
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                sum = 0.5,
                                headcount = 1,
                                caretakers = 2.0,
                                percentage = 3.6
                            )
                    ),
                realized =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period =
                                        FiniteDateRange(today.minusDays(2), today.minusDays(2)),
                                    sum = 1.0,
                                    headcount = 1,
                                    caretakers = null,
                                    percentage = null
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = null,
                                    percentage = null
                                ),
                            ),
                        max = null,
                        min = null
                    ),
            ),
            occupanciesForGroup1
        )
    }

    @Test
    fun `getUnitRealizedOccupanciesForDay returns correct data for current day`() {
        // children 1 and 2 are in group 1, child 3 in group 2
        // staff 1 and 2 are in group 1, staff 3 in group 2
        val group1 = DevDaycareGroup(daycareId = testDaycare.id, name = "g1")
        val group2 = DevDaycareGroup(daycareId = testDaycare.id, name = "g2")

        val placementRange = FiniteDateRange(today.minusMonths(3), today.plusMonths(3))
        val placement1 =
            DevPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementRange.start,
                endDate = placementRange.end
            )
        val groupPlacement1 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = group1.id,
                startDate = placementRange.start,
                endDate = placementRange.end
            )
        val placement2 =
            DevPlacement(
                childId = testChild_2.id,
                type = PlacementType.PRESCHOOL,
                unitId = testDaycare.id,
                startDate = placementRange.start,
                endDate = placementRange.end
            )
        val groupPlacement2 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement2.id,
                daycareGroupId = group1.id,
                startDate = placementRange.start,
                endDate = placementRange.end
            )
        val placement3 =
            DevPlacement(
                childId = testChild_3.id,
                unitId = testDaycare.id,
                startDate = placementRange.start,
                endDate = placementRange.end
            )
        val groupPlacement3 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement3.id,
                daycareGroupId = group2.id,
                startDate = placementRange.start,
                endDate = placementRange.end
            )
        val childAttendance1 =
            DevChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                date = today,
                arrived = LocalTime.of(9, 15),
                departed = LocalTime.of(14, 55)
            )
        val childAttendance2 =
            DevChildAttendance(
                childId = testChild_2.id,
                unitId = testDaycare.id,
                date = today,
                arrived = LocalTime.of(10, 0),
                departed = null
            )
        val childAttendance3 =
            DevChildAttendance(
                childId = testChild_3.id,
                unitId = testDaycare.id,
                date = today,
                arrived = LocalTime.of(9, 0),
                departed = LocalTime.of(14, 0)
            )

        val staff1 = DevEmployee()
        val staff2 = DevEmployee()
        val staff3 = DevEmployee()
        val staffAttendance1 =
            DevStaffAttendance(
                employeeId = staff1.id,
                groupId = group1.id,
                arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 0)),
                departed = HelsinkiDateTime.of(today, LocalTime.of(14, 0))
            )
        val staffAttendance2 =
            DevStaffAttendance(
                employeeId = staff2.id,
                groupId = group1.id,
                arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                departed = null,
                occupancyCoefficient = BigDecimal.ZERO
            )
        val staffAttendance3 =
            DevStaffAttendance(
                employeeId = staff3.id,
                groupId = group2.id,
                arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 30)),
                departed = HelsinkiDateTime.of(today, LocalTime.of(15, 0))
            )

        db.transaction { tx ->
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(placement1)
            tx.insert(groupPlacement1)
            tx.insert(placement2)
            tx.insert(groupPlacement2)
            tx.insert(placement3)
            tx.insert(groupPlacement3)
            tx.insert(childAttendance1)
            tx.insert(childAttendance2)
            tx.insert(childAttendance3)
            tx.insert(staff1)
            tx.insert(staff2)
            tx.insert(staff3)
            tx.insert(staffAttendance1)
            tx.insert(staffAttendance2)
            tx.insert(staffAttendance3)
        }

        val occupanciesForUnit = getUnitRealizedOccupanciesForDay(today, groupId = null)
        assertEquals(
            RealtimeOccupancy(
                childAttendances =
                    listOf(
                        ChildOccupancyAttendance(
                            childId = testChild_1.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(9, 15)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 55)),
                            capacity = 1.0
                        ),
                        ChildOccupancyAttendance(
                            childId = testChild_2.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                            departed = null,
                            capacity = 0.5
                        ),
                        ChildOccupancyAttendance(
                            childId = testChild_3.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(9, 0)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 0)),
                            capacity = 1.0
                        )
                    ),
                staffAttendances =
                    listOf(
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 0)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 0)),
                            capacity = 7.0
                        ),
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                            departed = null,
                            capacity = 0.0
                        ),
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 30)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(15, 0)),
                            capacity = 7.0
                        ),
                    )
            ),
            occupanciesForUnit
        )

        val occupanciesForGroup1 = getUnitRealizedOccupanciesForDay(today, group1.id)
        assertEquals(
            RealtimeOccupancy(
                childAttendances =
                    listOf(
                        ChildOccupancyAttendance(
                            childId = testChild_1.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(9, 15)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 55)),
                            capacity = 1.0
                        ),
                        ChildOccupancyAttendance(
                            childId = testChild_2.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                            departed = null,
                            capacity = 0.5
                        )
                    ),
                staffAttendances =
                    listOf(
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 0)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 0)),
                            capacity = 7.0
                        ),
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                            departed = null,
                            capacity = 0.0
                        ),
                    )
            ),
            occupanciesForGroup1
        )
    }

    private fun createApplication(
        childId: PersonId,
        type: ApplicationType = ApplicationType.DAYCARE,
        connectedDaycare: Boolean = false,
    ): ApplicationId =
        db.transaction { tx ->
            tx.createParentship(
                childId = childId,
                headOfChildId = testAdult_1.id,
                startDate = startDate,
                endDate = endDate,
                creator = Creator.DVV,
            )
            tx.insertTestApplication(
                status = ApplicationStatus.WAITING_PLACEMENT,
                childId = childId,
                guardianId = testAdult_1.id,
                type = type,
                document =
                    DaycareFormV0.fromApplication2(validDaycareApplication)
                        .copy(type = type, connectedDaycare = connectedDaycare),
            )
        }

    private fun getSpeculatedOccupancies(
        unitId: DaycareId,
        applicationId: ApplicationId,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange?,
    ): OccupancyResponseSpeculated {
        return occupancyController.getOccupancyPeriodsSpeculated(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER)),
            mockClock,
            unitId,
            applicationId,
            from = period.start,
            to = period.end,
            preschoolDaycareFrom = preschoolDaycarePeriod?.start,
            preschoolDaycareTo = preschoolDaycarePeriod?.end,
        )
    }

    private fun getUnitOccupancies(startDate: LocalDate, endDate: LocalDate, groupId: GroupId?) =
        occupancyController.getUnitOccupancies(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER)),
            mockClock,
            testDaycare.id,
            startDate,
            endDate,
            groupId
        )

    private fun getUnitRealizedOccupanciesForDay(date: LocalDate, groupId: GroupId?) =
        occupancyController.getUnitRealizedOccupanciesForDay(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER)),
            mockClock,
            testDaycare.id,
            date,
            groupId
        )
}
