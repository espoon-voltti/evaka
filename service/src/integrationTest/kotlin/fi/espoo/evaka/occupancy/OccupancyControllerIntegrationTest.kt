// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.Address
import fi.espoo.evaka.application.ApplicationForm
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.ChildDetails
import fi.espoo.evaka.application.Guardian
import fi.espoo.evaka.application.PersonBasics
import fi.espoo.evaka.application.Preferences
import fi.espoo.evaka.application.PreferredUnit
import fi.espoo.evaka.application.ServiceNeed
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
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
import fi.espoo.evaka.shared.dev.DevApplicationWithForm
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChildAttendance
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevPlacementDraft
import fi.espoo.evaka.shared.dev.DevPlacementPlan
import fi.espoo.evaka.shared.dev.DevStaffAttendance
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertApplication
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OccupancyControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var occupancyController: OccupancyController

    private val today = LocalDate.of(2024, 8, 21)
    private val mockClock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(16, 0)))
    private val startDate = LocalDate.of(2019, 1, 1)
    private val endDate = LocalDate.of(2021, 12, 31)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val employee = DevEmployee()
    private val adult = DevPerson()
    // Children with specific dateOfBirth values that determine under-3/over-3 capacity factors
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))
    private val child3 = DevPerson(dateOfBirth = LocalDate.of(2018, 9, 1))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2, child3).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insert(preschoolTerm2020)
            tx.insertServiceNeedOptions()
        }
    }

    @Test
    fun `No caretakers, cannot compute`() {
        val applicationId = createApplication(child1.id)

        val occupancies =
            getSpeculatedOccupancies(
                daycare.id,
                applicationId,
                period = FiniteDateRange(startDate, endDate),
                preschoolDaycarePeriod = null,
            )

        assertEquals(OccupancyResponseSpeculated(null, null, null, null), occupancies)
    }

    @Test
    fun `No previous placements`() {
        db.transaction { tx ->
            val groupId = tx.insert(DevDaycareGroup(daycareId = daycare.id))
            tx.insert(
                DevDaycareCaretaker(
                    groupId = groupId,
                    amount = 1.0.toBigDecimal(),
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val applicationId = createApplication(child1.id)

        val occupancies =
            getSpeculatedOccupancies(
                daycare.id,
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
            val groupId = tx.insert(DevDaycareGroup(daycareId = daycare.id))
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
                    childId = child3.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = startDate.plusMonths(4),
                    endDate = endDate,
                )
            )
        }

        // Under 3 years (coefficient 1.75)
        val applicationId = createApplication(child1.id)

        val occupancies =
            getSpeculatedOccupancies(
                daycare.id,
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
            val groupId = tx.insert(DevDaycareGroup(daycareId = daycare.id))
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
                    childId = child3.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = LocalDate.of(2021, 7, 1),
                    endDate = endDate,
                )
            )
        }

        // Over 3 years (coefficient 1)
        val applicationId =
            createApplication(child2.id, type = ApplicationType.PRESCHOOL, connectedDaycare = false)

        val occupancies =
            getSpeculatedOccupancies(
                daycare.id,
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
            val groupId = tx.insert(DevDaycareGroup(daycareId = daycare.id))
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
                    childId = child3.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = LocalDate.of(2021, 7, 1),
                    endDate = endDate,
                )
            )
        }

        // Over 3 years (coefficient 1)
        val applicationId =
            createApplication(child2.id, type = ApplicationType.PRESCHOOL, connectedDaycare = true)

        val occupancies =
            getSpeculatedOccupancies(
                daycare.id,
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
            val groupId = tx.insert(DevDaycareGroup(daycareId = daycare.id))
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
                    childId = child3.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = LocalDate.of(2021, 7, 1),
                    endDate = endDate,
                )
            )

            // Over 3 years (coefficient 1), will be speculated for preschool
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }

        val applicationId =
            createApplication(child2.id, type = ApplicationType.PRESCHOOL, connectedDaycare = true)

        val occupancies =
            getSpeculatedOccupancies(
                daycare.id,
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
        val group1 = DevDaycareGroup(daycareId = daycare.id, name = "g1")
        val group2 = DevDaycareGroup(daycareId = daycare.id, name = "g2")
        val caretakers1 =
            DevDaycareCaretaker(groupId = group1.id, amount = BigDecimal("2.0"), startDate = start)
        val caretakers2 =
            DevDaycareCaretaker(groupId = group2.id, amount = BigDecimal("1.0"), startDate = start)

        val placement1 =
            DevPlacement(childId = child1.id, unitId = daycare.id, startDate = start, endDate = end)
        val groupPlacement1a =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = group1.id,
                startDate = start,
                endDate = start.plusDays(2),
            )
        val groupPlacement1b =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = group2.id,
                startDate = start.plusDays(3),
                endDate = end,
            )
        val placement2 =
            DevPlacement(
                childId = child2.id,
                type = PlacementType.PRESCHOOL,
                unitId = daycare.id,
                startDate = start.plusDays(1),
                endDate = end,
            )
        val groupPlacement2 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement2.id,
                daycareGroupId = group1.id,
                startDate = start.plusDays(1),
                endDate = end,
            )
        val placement3 =
            DevPlacement(
                childId = child3.id,
                unitId = daycare.id,
                startDate = start,
                endDate = end.minusDays(1),
            )
        val groupPlacement3 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement3.id,
                daycareGroupId = group2.id,
                startDate = start,
                endDate = end.minusDays(1),
            )

        val plannedChild = DevPerson()
        val application1 =
            createTestApplication(
                child = plannedChild,
                guardian = adult,
                preferredUnits = listOf(daycare),
                status = ApplicationStatus.WAITING_DECISION,
                preferredStart = start,
            )
        val placementPlan =
            DevPlacementPlan(
                applicationId = application1.id,
                unitId = daycare.id,
                startDate = start,
                endDate = end,
            )

        val draftPlacedChild = DevPerson()
        val application2 =
            createTestApplication(
                child = draftPlacedChild,
                guardian = adult,
                preferredUnits = listOf(daycare),
                preferredStart = start,
            )
        val placementDraft =
            DevPlacementDraft(
                applicationId = application2.id,
                unitId = daycare.id,
                startDate = application2.form.preferences.preferredStartDate!!,
                createdAt = mockClock.now(),
                createdBy = employee.evakaUserId,
                modifiedAt = mockClock.now(),
                modifiedBy = employee.evakaUserId,
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
            tx.insert(plannedChild, DevPersonType.CHILD)
            tx.insertApplication(application1)
            tx.insert(placementPlan)
            tx.insert(draftPlacedChild, DevPersonType.CHILD)
            tx.insertApplication(application2)
            tx.insert(placementDraft)
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
                                    percentage = 9.5,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                    sum = 2.5,
                                    headcount = 3,
                                    caretakers = 3.0,
                                    percentage = 11.9,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = 3.0,
                                    percentage = 7.1,
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                sum = 2.5,
                                headcount = 3,
                                caretakers = 3.0,
                                percentage = 11.9,
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                sum = 1.5,
                                headcount = 2,
                                caretakers = 3.0,
                                percentage = 7.1,
                            ),
                    ),
                planned =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period =
                                        FiniteDateRange(today.minusDays(2), today.minusDays(2)),
                                    sum = 3.0,
                                    headcount = 3,
                                    caretakers = 3.0,
                                    percentage = 14.3,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                    sum = 3.5,
                                    headcount = 4,
                                    caretakers = 3.0,
                                    percentage = 16.7,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                    sum = 2.5,
                                    headcount = 3,
                                    caretakers = 3.0,
                                    percentage = 11.9,
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                sum = 3.5,
                                headcount = 4,
                                caretakers = 3.0,
                                percentage = 16.7,
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                sum = 2.5,
                                headcount = 3,
                                caretakers = 3.0,
                                percentage = 11.9,
                            ),
                    ),
                draft =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period =
                                        FiniteDateRange(today.minusDays(2), today.minusDays(2)),
                                    sum = 4.0,
                                    headcount = 4,
                                    caretakers = 3.0,
                                    percentage = 19.0,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                    sum = 4.5,
                                    headcount = 5,
                                    caretakers = 3.0,
                                    percentage = 21.4,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                    sum = 3.5,
                                    headcount = 4,
                                    caretakers = 3.0,
                                    percentage = 16.7,
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today.plusDays(1)),
                                sum = 4.5,
                                headcount = 5,
                                caretakers = 3.0,
                                percentage = 21.4,
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(2), today.plusDays(2)),
                                sum = 3.5,
                                headcount = 4,
                                caretakers = 3.0,
                                percentage = 16.7,
                            ),
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
                                    percentage = null,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today),
                                    sum = 2.5,
                                    headcount = 3,
                                    caretakers = null,
                                    percentage = null,
                                ),
                            ),
                        max = null,
                        min = null,
                    ),
            ),
            occupanciesForUnit,
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
                                    percentage = 7.1,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = 2.0,
                                    percentage = 10.7,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                    sum = 0.5,
                                    headcount = 1,
                                    caretakers = 2.0,
                                    percentage = 3.6,
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today),
                                sum = 1.5,
                                headcount = 2,
                                caretakers = 2.0,
                                percentage = 10.7,
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                sum = 0.5,
                                headcount = 1,
                                caretakers = 2.0,
                                percentage = 3.6,
                            ),
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
                                    percentage = 7.1,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = 2.0,
                                    percentage = 10.7,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                    sum = 0.5,
                                    headcount = 1,
                                    caretakers = 2.0,
                                    percentage = 3.6,
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today),
                                sum = 1.5,
                                headcount = 2,
                                caretakers = 2.0,
                                percentage = 10.7,
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                sum = 0.5,
                                headcount = 1,
                                caretakers = 2.0,
                                percentage = 3.6,
                            ),
                    ),
                draft =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period =
                                        FiniteDateRange(today.minusDays(2), today.minusDays(2)),
                                    sum = 1.0,
                                    headcount = 1,
                                    caretakers = 2.0,
                                    percentage = 7.1,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = 2.0,
                                    percentage = 10.7,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                    sum = 0.5,
                                    headcount = 1,
                                    caretakers = 2.0,
                                    percentage = 3.6,
                                ),
                            ),
                        max =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.minusDays(1), today),
                                sum = 1.5,
                                headcount = 2,
                                caretakers = 2.0,
                                percentage = 10.7,
                            ),
                        min =
                            OccupancyPeriod(
                                period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                                sum = 0.5,
                                headcount = 1,
                                caretakers = 2.0,
                                percentage = 3.6,
                            ),
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
                                    percentage = null,
                                ),
                                OccupancyPeriod(
                                    period = FiniteDateRange(today.minusDays(1), today),
                                    sum = 1.5,
                                    headcount = 2,
                                    caretakers = null,
                                    percentage = null,
                                ),
                            ),
                        max = null,
                        min = null,
                    ),
            ),
            occupanciesForGroup1,
        )
    }

    @Test
    fun `getUnitOccupancies does not show draft occupancies to unit supervisor`() {
        val start = today.minusDays(2)
        val end = today.plusDays(2)
        val group1 = DevDaycareGroup(daycareId = daycare.id, name = "g1")
        val caretakers1 =
            DevDaycareCaretaker(groupId = group1.id, amount = BigDecimal("2.0"), startDate = start)
        val unitSupervisor = DevEmployee()

        val placement1 =
            DevPlacement(childId = child1.id, unitId = daycare.id, startDate = start, endDate = end)
        val draftPlacedChild = DevPerson()
        val application =
            createTestApplication(
                child = draftPlacedChild,
                guardian = adult,
                preferredUnits = listOf(daycare),
                preferredStart = start,
            )
        val placementDraft =
            DevPlacementDraft(
                applicationId = application.id,
                unitId = daycare.id,
                startDate = application.form.preferences.preferredStartDate!!,
                createdAt = mockClock.now(),
                createdBy = employee.evakaUserId,
                modifiedAt = mockClock.now(),
                modifiedBy = employee.evakaUserId,
            )

        db.transaction { tx ->
            tx.insert(group1)
            tx.insert(caretakers1)
            tx.insert(unitSupervisor, mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(placement1)
            tx.insert(draftPlacedChild, DevPersonType.CHILD)
            tx.insertApplication(application)
            tx.insert(placementDraft)
        }

        val occupanciesForUnit =
            getUnitOccupancies(start, end, groupId = null, user = unitSupervisor.user)
        val occupancyPeriod =
            OccupancyPeriod(
                period = FiniteDateRange(start, end),
                sum = 1.0,
                headcount = 1,
                caretakers = 2.0,
                percentage = 7.1,
            )
        assertEquals(
            UnitOccupancies(
                caretakers = Caretakers(minimum = 2.0, maximum = 2.0),
                confirmed =
                    OccupancyResponse(
                        occupancies = listOf(occupancyPeriod),
                        max = occupancyPeriod,
                        min = occupancyPeriod,
                    ),
                planned =
                    OccupancyResponse(
                        occupancies = listOf(occupancyPeriod),
                        max = occupancyPeriod,
                        min = occupancyPeriod,
                    ),
                draft = null,
                realized =
                    OccupancyResponse(
                        occupancies =
                            listOf(
                                OccupancyPeriod(
                                    period = FiniteDateRange(start, mockClock.today()),
                                    sum = 1.0,
                                    headcount = 1,
                                    caretakers = null,
                                    percentage = null,
                                )
                            ),
                        max = null,
                        min = null,
                    ),
            ),
            occupanciesForUnit,
        )
    }

    @Test
    fun `getUnitRealizedOccupanciesForDay returns correct data for current day`() {
        // children 1 and 2 are in group 1, child 3 in group 2
        // staff 1 and 2 are in group 1, staff 3 in group 2
        val group1 = DevDaycareGroup(daycareId = daycare.id, name = "g1")
        val group2 = DevDaycareGroup(daycareId = daycare.id, name = "g2")

        val placementRange = FiniteDateRange(today.minusMonths(3), today.plusMonths(3))
        val placement1 =
            DevPlacement(
                childId = child1.id,
                unitId = daycare.id,
                startDate = placementRange.start,
                endDate = placementRange.end,
            )
        val groupPlacement1 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = group1.id,
                startDate = placementRange.start,
                endDate = placementRange.end,
            )
        val placement2 =
            DevPlacement(
                childId = child2.id,
                type = PlacementType.PRESCHOOL,
                unitId = daycare.id,
                startDate = placementRange.start,
                endDate = placementRange.end,
            )
        val groupPlacement2 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement2.id,
                daycareGroupId = group1.id,
                startDate = placementRange.start,
                endDate = placementRange.end,
            )
        val placement3 =
            DevPlacement(
                childId = child3.id,
                unitId = daycare.id,
                startDate = placementRange.start,
                endDate = placementRange.end,
            )
        val groupPlacement3 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement3.id,
                daycareGroupId = group2.id,
                startDate = placementRange.start,
                endDate = placementRange.end,
            )
        val childAttendance1 =
            DevChildAttendance(
                childId = child1.id,
                unitId = daycare.id,
                date = today,
                arrived = LocalTime.of(9, 15),
                departed = LocalTime.of(14, 55),
            )
        val childAttendance2 =
            DevChildAttendance(
                childId = child2.id,
                unitId = daycare.id,
                date = today,
                arrived = LocalTime.of(10, 0),
                departed = null,
            )
        val childAttendance3 =
            DevChildAttendance(
                childId = child3.id,
                unitId = daycare.id,
                date = today,
                arrived = LocalTime.of(9, 0),
                departed = LocalTime.of(14, 0),
            )

        val staff1 = DevEmployee()
        val staff2 = DevEmployee()
        val staff3 = DevEmployee()
        val staffAttendance1 =
            DevStaffAttendance(
                employeeId = staff1.id,
                groupId = group1.id,
                arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 0)),
                departed = HelsinkiDateTime.of(today, LocalTime.of(14, 0)),
                modifiedAt = mockClock.now(),
                modifiedBy = staff1.evakaUserId,
            )
        val staffAttendance2 =
            DevStaffAttendance(
                employeeId = staff2.id,
                groupId = group1.id,
                arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                departed = null,
                occupancyCoefficient = BigDecimal.ZERO,
                modifiedAt = mockClock.now(),
                modifiedBy = staff2.evakaUserId,
            )
        val staffAttendance3 =
            DevStaffAttendance(
                employeeId = staff3.id,
                groupId = group2.id,
                arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 30)),
                departed = HelsinkiDateTime.of(today, LocalTime.of(15, 0)),
                modifiedAt = mockClock.now(),
                modifiedBy = staff3.evakaUserId,
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

        val occupanciesForUnit = getUnitRealizedOccupanciesForDay(today, groupIds = null)
        assertEquals(
            RealtimeOccupancy(
                childAttendances =
                    listOf(
                        ChildOccupancyAttendance(
                            childId = child3.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(9, 0)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 0)),
                            capacity = 1.0,
                        ),
                        ChildOccupancyAttendance(
                            childId = child1.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(9, 15)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 55)),
                            capacity = 1.0,
                        ),
                        ChildOccupancyAttendance(
                            childId = child2.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                            departed = null,
                            capacity = 0.5,
                        ),
                    ),
                staffAttendances =
                    listOf(
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 0)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 0)),
                            capacity = 7.0,
                        ),
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 30)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(15, 0)),
                            capacity = 7.0,
                        ),
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                            departed = null,
                            capacity = 0.0,
                        ),
                    ),
            ),
            occupanciesForUnit,
        )

        val occupanciesForGroup1 = getUnitRealizedOccupanciesForDay(today, listOf(group1.id))
        assertEquals(
            RealtimeOccupancy(
                childAttendances =
                    listOf(
                        ChildOccupancyAttendance(
                            childId = child1.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(9, 15)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 55)),
                            capacity = 1.0,
                        ),
                        ChildOccupancyAttendance(
                            childId = child2.id,
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                            departed = null,
                            capacity = 0.5,
                        ),
                    ),
                staffAttendances =
                    listOf(
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 0)),
                            departed = HelsinkiDateTime.of(today, LocalTime.of(14, 0)),
                            capacity = 7.0,
                        ),
                        StaffOccupancyAttendance(
                            arrived = HelsinkiDateTime.of(today, LocalTime.of(10, 0)),
                            departed = null,
                            capacity = 0.0,
                        ),
                    ),
            ),
            occupanciesForGroup1,
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
                headOfChildId = adult.id,
                startDate = startDate,
                endDate = endDate,
                creator = Creator.DVV,
            )
            tx.insertTestApplication(
                status = ApplicationStatus.WAITING_PLACEMENT,
                childId = childId,
                guardianId = adult.id,
                type = type,
                document =
                    DaycareFormV0(
                        type = type,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                        connectedDaycare =
                            if (type == ApplicationType.PRESCHOOL) connectedDaycare else null,
                    ),
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
            AuthenticatedUser.Employee(employee.id, setOf(UserRole.SERVICE_WORKER)),
            mockClock,
            unitId,
            applicationId,
            from = period.start,
            to = period.end,
            preschoolDaycareFrom = preschoolDaycarePeriod?.start,
            preschoolDaycareTo = preschoolDaycarePeriod?.end,
        )
    }

    private fun getUnitOccupancies(
        startDate: LocalDate,
        endDate: LocalDate,
        groupId: GroupId?,
        user: AuthenticatedUser.Employee =
            AuthenticatedUser.Employee(employee.id, setOf(UserRole.SERVICE_WORKER)),
    ) =
        occupancyController.getUnitOccupancies(
            dbInstance(),
            user,
            mockClock,
            daycare.id,
            startDate,
            endDate,
            groupId,
        )

    private fun getUnitRealizedOccupanciesForDay(date: LocalDate, groupIds: List<GroupId>?) =
        occupancyController
            .getUnitRealizedOccupanciesForDay(
                dbInstance(),
                AuthenticatedUser.Employee(employee.id, setOf(UserRole.SERVICE_WORKER)),
                mockClock,
                daycare.id,
                OccupancyController.GetUnitOccupanciesForDayBody(date, groupIds),
            )
            .let { occupancies ->
                occupancies.copy(
                    childAttendances =
                        occupancies.childAttendances.sortedWith(
                            compareBy({ it.arrived }, { it.departed })
                        ),
                    staffAttendances =
                        occupancies.staffAttendances.sortedWith(
                            compareBy({ it.arrived }, { it.departed })
                        ),
                )
            }

    private fun createTestApplication(
        child: DevPerson,
        guardian: DevPerson,
        preferredUnits: List<DevDaycare>,
        status: ApplicationStatus = ApplicationStatus.WAITING_PLACEMENT,
        transferApplication: Boolean = false,
        preferredStart: LocalDate,
        partTime: Boolean = false,
    ) =
        DevApplicationWithForm(
            id = ApplicationId(UUID.randomUUID()),
            type = ApplicationType.DAYCARE,
            createdAt = mockClock.now().minusDays(55),
            createdBy = guardian.evakaUserId(),
            modifiedAt = mockClock.now().minusDays(55),
            modifiedBy = guardian.evakaUserId(),
            sentDate = mockClock.today().minusDays(55),
            sentTime = mockClock.now().toLocalTime(),
            dueDate = mockClock.today().plusDays(18),
            status = status,
            guardianId = guardian.id,
            childId = child.id,
            origin = ApplicationOrigin.ELECTRONIC,
            checkedByAdmin = true,
            confidential = true,
            hideFromGuardian = false,
            transferApplication = transferApplication,
            otherGuardians = emptyList(),
            form =
                ApplicationForm(
                    child =
                        ChildDetails(
                            person =
                                PersonBasics(
                                    child.firstName,
                                    child.lastName,
                                    socialSecurityNumber = null,
                                ),
                            dateOfBirth = child.dateOfBirth,
                            address = Address("Testikatu 1", "00200", "Espoo"),
                            futureAddress = null,
                            nationality = "fi",
                            language = "fi",
                            allergies = "",
                            diet = "",
                            assistanceNeeded = false,
                            assistanceDescription = "",
                        ),
                    guardian =
                        Guardian(
                            person =
                                PersonBasics(
                                    guardian.firstName,
                                    guardian.lastName,
                                    socialSecurityNumber = null,
                                ),
                            address = Address("Testikatu 1", "00200", "Espoo"),
                            futureAddress = null,
                            phoneNumber = "+358 50 1234567",
                            email = "testitesti@gmail.com",
                        ),
                    secondGuardian = null,
                    otherPartner = null,
                    otherChildren = emptyList(),
                    preferences =
                        Preferences(
                            preferredUnits = preferredUnits.map { PreferredUnit(it.id, it.name) },
                            preferredStartDate = preferredStart,
                            connectedDaycarePreferredStartDate = null,
                            serviceNeed =
                                ServiceNeed(
                                    startTime = "08:00",
                                    endTime = "16:00",
                                    shiftCare = false,
                                    partTime = partTime,
                                    serviceNeedOption = null,
                                ),
                            siblingBasis = null,
                            preparatory = false,
                            urgent = false,
                        ),
                    maxFeeAccepted = true,
                    otherInfo = "",
                    clubDetails = null,
                ),
        )
}
