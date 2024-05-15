// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OccupancyControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var occupancyController: OccupancyController

    private val startDate = LocalDate.of(2019, 1, 1)
    private val endDate = LocalDate.of(2021, 12, 31)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `No caretakers, cannot compute`() {
        val applicationId = createApplication(testChild_1.id)

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(startDate, endDate),
                preschoolDaycarePeriod = null
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
                    endDate = endDate
                )
            )
        }
        val applicationId = createApplication(testChild_1.id)

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(startDate, endDate),
                preschoolDaycarePeriod = null
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 0.0,
                        sumOver3y = 0.0,
                        headcount = 0,
                        caretakers = 1.0,
                        percentage = 0.0
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 0.0,
                        sumOver3y = 0.0,
                        headcount = 0,
                        caretakers = 1.0,
                        percentage = 0.0
                    ),
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0
                    ),
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0
                    )
            ),
            occupancies
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
                    endDate = endDate
                )
            )

            // Under 3 years (coefficient 1.75)
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = startDate.plusMonths(4),
                    endDate = endDate
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
                preschoolDaycarePeriod = null
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3
                    ),
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 3.5,
                        sumOver3y = 0.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 50.0
                    ),
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 3.5,
                        sumOver3y = 1.0,
                        headcount = 3,
                        caretakers = 1.0,
                        percentage = 64.3
                    )
            ),
            occupancies
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
                    endDate = endDate
                )
            )

            // Under 3 years (coefficient 1.75)
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2021, 7, 1),
                    endDate = endDate
                )
            )
        }

        // Over 3 years (coefficient 1)
        val applicationId =
            createApplication(
                testChild_2.id,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = false
            )

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 6, 4)),
                preschoolDaycarePeriod = null
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3
                    ),
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.5,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 32.1
                    ),
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3
                    )
            ),
            occupancies
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
                    endDate = endDate
                )
            )

            // Under 3 years (coefficient 1.75)
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2021, 7, 1),
                    endDate = endDate
                )
            )
        }

        // Over 3 years (coefficient 1)
        val applicationId =
            createApplication(
                testChild_2.id,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true
            )

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 6, 4)),
                preschoolDaycarePeriod =
                    FiniteDateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 7, 31))
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.0,
                        headcount = 1,
                        caretakers = 1.0,
                        percentage = 25.0
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3
                    ),
                // preschool only
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 0.5,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 32.1
                    ),
                // preschool+daycare
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 2.0,
                        headcount = 3,
                        caretakers = 1.0,
                        percentage = 53.6
                    )
            ),
            occupancies
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
                    endDate = endDate
                )
            )

            // Under 3 years (coefficient 1.75)
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate
                )
            )

            // Over 3 years (coefficient 1), starts after 3 months of the speculated placement
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2021, 7, 1),
                    endDate = endDate
                )
            )

            // Over 3 years (coefficient 1), will be speculated for preschool
            tx.insert(
                DevPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate
                )
            )
        }

        val applicationId =
            createApplication(
                testChild_2.id,
                type = ApplicationType.PRESCHOOL,
                connectedDaycare = true
            )

        val occupancies =
            getSpeculatedOccupancies(
                testDaycare.id,
                applicationId,
                period = FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 6, 4)),
                preschoolDaycarePeriod =
                    FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 7, 31))
            )

        assertEquals(
            OccupancyResponseSpeculated(
                max3Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3
                    ),
                max6Months =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 2.0,
                        headcount = 3,
                        caretakers = 1.0,
                        percentage = 53.6
                    ),
                // Same occupancy values as above, because the child already had a placement in this
                // unit
                max3MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 1.0,
                        headcount = 2,
                        caretakers = 1.0,
                        percentage = 39.3
                    ),
                max6MonthsSpeculated =
                    OccupancyValues(
                        sumUnder3y = 1.75,
                        sumOver3y = 2.0,
                        headcount = 3,
                        caretakers = 1.0,
                        percentage = 53.6
                    )
            ),
            occupancies
        )
    }

    private fun createApplication(
        childId: PersonId,
        type: ApplicationType = ApplicationType.DAYCARE,
        connectedDaycare: Boolean = false
    ): ApplicationId =
        db.transaction { tx ->
            tx.createParentship(
                childId = childId,
                headOfChildId = testAdult_1.id,
                startDate = startDate,
                endDate = endDate,
                creator = Creator.DVV
            )
            val applicationId =
                tx.insertTestApplication(
                    status = ApplicationStatus.WAITING_PLACEMENT,
                    childId = childId,
                    guardianId = testAdult_1.id,
                    type = type
                )
            val form =
                DaycareFormV0.fromApplication2(validDaycareApplication)
                    .copy(type = type, connectedDaycare = connectedDaycare)
            tx.insertTestApplicationForm(applicationId, form)

            applicationId
        }

    private fun getSpeculatedOccupancies(
        unitId: DaycareId,
        applicationId: ApplicationId,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange?
    ): OccupancyResponseSpeculated {
        val user =
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
        return occupancyController.getOccupancyPeriodsSpeculated(
            dbInstance(),
            user,
            RealEvakaClock(),
            unitId,
            applicationId,
            from = period.start,
            to = period.end,
            preschoolDaycareFrom = preschoolDaycarePeriod?.start,
            preschoolDaycareTo = preschoolDaycarePeriod?.end,
        )
    }
}
