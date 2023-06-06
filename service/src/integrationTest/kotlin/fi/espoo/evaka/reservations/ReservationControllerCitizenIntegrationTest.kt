// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDaycareContractDays10
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testRoundTheClockDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ReservationControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var reservationControllerCitizen: ReservationControllerCitizen
    @Autowired private lateinit var dailyServiceTimesController: DailyServiceTimesController

    // Monday
    private val mockToday: LocalDate = LocalDate.of(2021, 11, 1)

    private val sunday = LocalDate.of(2021, 11, 14)
    private val monday = sunday.plusDays(1)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val thursday = monday.plusDays(3)

    private val startTime = LocalTime.of(9, 0)
    private val endTime = LocalTime.of(17, 0)

    private val testStaffId = EmployeeId(UUID.randomUUID())

    @BeforeEach
    fun before() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = monday,
                endDate = tuesday
            )
            it.insertTestPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = monday,
                    endDate = tuesday
                )
                .let { placementId ->
                    it.insertTestServiceNeed(
                        confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        placementId = placementId,
                        period = FiniteDateRange(monday, tuesday),
                        optionId = snDaycareContractDays10.id,
                        shiftCare = false
                    )
                }
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_2.id)

            it.insertTestEmployee(DevEmployee(testStaffId))
        }
    }

    @Test
    fun `get reservations returns correct children every day in range`() {
        db.transaction { tx ->
            tx.insertTestHoliday(wednesday)

            // Reservation not required (PRESCHOOL)
            tx.insertTestPlacement(
                childId = testChild_3.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL,
                startDate = monday,
                endDate = wednesday
            )
            tx.insertGuardian(guardianId = testAdult_1.id, childId = testChild_3.id)
        }

        val res = getReservations(FiniteDateRange(sunday, thursday))

        assertEquals(
            ReservationsResponse(
                reservableRange =
                    FiniteDateRange(
                        LocalDate.of(2021, 11, 8), // Next week's monday
                        LocalDate.of(2022, 8, 31),
                    ),
                includesWeekends = false,
                children =
                    // Sorted by date of birth, oldest child first
                    listOf(
                        ReservationChild(
                            id = testChild_2.id,
                            firstName = testChild_2.firstName,
                            lastName = testChild_2.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_1.id,
                            firstName = testChild_1.firstName,
                            lastName = testChild_1.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_3.id,
                            firstName = testChild_3.firstName,
                            lastName = testChild_3.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.PRESCHOOL,
                        ),
                    ),
                days =
                    listOf(
                        // sunday is not included because it's not a weekday

                        ReservationResponseDay(
                            date = monday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            unitOperationTime = testDaycare.operationTimes[0]
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            contractDays = true,
                                            unitOperationTime = testDaycare.operationTimes[0]
                                        ),
                                        dayChild(
                                            testChild_3.id,
                                            requiresReservation = false,
                                            unitOperationTime = testDaycare.operationTimes[0]
                                        )
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = tuesday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            unitOperationTime = testDaycare.operationTimes[1]
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            contractDays = true,
                                            unitOperationTime = testDaycare.operationTimes[1]
                                        ),
                                        dayChild(
                                            testChild_3.id,
                                            requiresReservation = false,
                                            unitOperationTime = testDaycare.operationTimes[1]
                                        )
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = wednesday,
                            holiday = true,
                            children = emptyList() // Holiday, no children eligible for daycare
                        ),
                        ReservationResponseDay(
                            date = thursday,
                            holiday = false,
                            children = emptyList()
                        )
                    )
            ),
            res
        )
    }

    @Test
    fun `get reservations returns correct children every day in range with a child in shift care`() {
        val roundTheClockDaycare =
            testRoundTheClockDaycare.copy(
                id = DaycareId(UUID.randomUUID()),
                enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
            )

        db.transaction { tx ->
            tx.insertTestHoliday(tuesday)

            tx.insertTestDaycare(roundTheClockDaycare)
            tx.insertTestPlacement(
                childId = testChild_3.id,
                unitId = roundTheClockDaycare.id,
                type = PlacementType.DAYCARE,
                startDate = sunday,
                endDate = tuesday
            )
            tx.insertGuardian(guardianId = testAdult_1.id, childId = testChild_3.id)
        }

        val res = getReservations(FiniteDateRange(sunday, wednesday))

        assertEquals(
            ReservationsResponse(
                reservableRange =
                    FiniteDateRange(
                        LocalDate.of(2021, 11, 8), // Next week's monday
                        LocalDate.of(2022, 8, 31),
                    ),
                includesWeekends = true,
                children =
                    // Sorted by date of birth, oldest child first
                    listOf(
                        ReservationChild(
                            id = testChild_2.id,
                            firstName = testChild_2.firstName,
                            lastName = testChild_2.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_1.id,
                            firstName = testChild_1.firstName,
                            lastName = testChild_1.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_3.id,
                            firstName = testChild_3.firstName,
                            lastName = testChild_3.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                    ),
                days =
                    listOf(
                        ReservationResponseDay(
                            date = sunday,
                            holiday = false,
                            children =
                                listOf(
                                    // sunday, only shift care is eligible
                                    dayChild(
                                        testChild_3.id,
                                        shiftCare = true,
                                        unitOperationTime = roundTheClockDaycare.operationTimes[6]
                                    ),
                                )
                        ),
                        ReservationResponseDay(
                            date = monday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            unitOperationTime = testDaycare.operationTimes[0]
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            contractDays = true,
                                            unitOperationTime = testDaycare.operationTimes[0]
                                        ),
                                        dayChild(
                                            testChild_3.id,
                                            shiftCare = true,
                                            unitOperationTime =
                                                roundTheClockDaycare.operationTimes[0]
                                        ),
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = tuesday,
                            holiday = true,
                            children =
                                listOf(
                                    // holiday, only shift care is eligible
                                    dayChild(
                                        testChild_3.id,
                                        shiftCare = true,
                                        unitOperationTime = roundTheClockDaycare.operationTimes[1]
                                    ),
                                )
                        ),
                        ReservationResponseDay(
                            date = wednesday,
                            holiday = false,
                            children = emptyList()
                        )
                    )
            ),
            res
        )
    }

    @Test
    fun `irregular daily service time absences are non-editable`() {
        db.transaction { tx ->
            tx.insertDaycareAclRow(testDaycare.id, testDecisionMaker_1.id, UserRole.UNIT_SUPERVISOR)
        }

        dailyServiceTimesController.postDailyServiceTimes(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf()),
            MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0))),
            testChild_1.id,
            DailyServiceTimesValue.IrregularTimes(
                validityPeriod = DateRange(mockToday.plusDays(1), null),
                monday = null,
                tuesday = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                wednesday = null,
                thursday = null,
                friday = null,
                saturday = null,
                sunday = null,
            )
        )

        val res = getReservations(FiniteDateRange(monday, tuesday))

        assertEquals(
            ReservationsResponse(
                reservableRange =
                    FiniteDateRange(
                        LocalDate.of(2021, 11, 8), // Next week's monday
                        LocalDate.of(2022, 8, 31),
                    ),
                includesWeekends = false,
                children =
                    // Sorted by date of birth, oldest child first
                    listOf(
                        ReservationChild(
                            id = testChild_2.id,
                            firstName = testChild_2.firstName,
                            lastName = testChild_2.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_1.id,
                            firstName = testChild_1.firstName,
                            lastName = testChild_1.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                        ),
                    ),
                days =
                    listOf(
                        ReservationResponseDay(
                            date = monday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            absence =
                                                AbsenceInfo(
                                                    AbsenceType.OTHER_ABSENCE,
                                                    editable = false
                                                ),
                                            unitOperationTime = testDaycare.operationTimes[0]
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            contractDays = true,
                                            unitOperationTime = testDaycare.operationTimes[0]
                                        ),
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = tuesday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            unitOperationTime = testDaycare.operationTimes[1]
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            contractDays = true,
                                            unitOperationTime = testDaycare.operationTimes[0]
                                        ),
                                    )
                                    .sortedBy { it.childId }
                        ),
                    )
            ),
            res
        )
    }

    @Test
    fun `adding reservations works in a basic case`() {
        postReservations(
            listOf(testChild_1.id, testChild_2.id).flatMap { child ->
                listOf(
                    DailyReservationRequest.Reservations(
                        child,
                        monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        child,
                        tuesday,
                        TimeRange(startTime, endTime),
                    )
                )
            }
        )

        val res = getReservations(FiniteDateRange(monday, wednesday))

        assertEquals(
            listOf(
                ReservationChild(
                    id = testChild_2.id,
                    firstName = testChild_2.firstName,
                    lastName = testChild_2.lastName,
                    preferredName = "",
                    duplicateOf = null,
                    imageId = null,
                    upcomingPlacementType = PlacementType.DAYCARE,
                ),
                ReservationChild(
                    id = testChild_1.id,
                    firstName = testChild_1.firstName,
                    lastName = testChild_1.lastName,
                    preferredName = "",
                    duplicateOf = null,
                    imageId = null,
                    upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                )
            ),
            res.children
        )

        assertEquals(3, res.days.size)

        res.days[0].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            unitOperationTime = testDaycare.operationTimes[0]
                        ),
                        dayChild(
                            testChild_2.id,
                            contractDays = true,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            unitOperationTime = testDaycare.operationTimes[0]
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[1].let { day ->
            assertEquals(tuesday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            unitOperationTime = testDaycare.operationTimes[1]
                        ),
                        dayChild(
                            testChild_2.id,
                            contractDays = true,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            unitOperationTime = testDaycare.operationTimes[1]
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[2].let { day ->
            assertEquals(wednesday, day.date)
            assertEquals(emptyList(), day.children)
        }
    }

    @Test
    fun `adding a reservation and an absence works`() {
        postReservations(
            listOf(
                DailyReservationRequest.Reservations(
                    testChild_1.id,
                    monday,
                    TimeRange(startTime, endTime),
                ),
                DailyReservationRequest.Absent(
                    testChild_1.id,
                    tuesday,
                )
            )
        )

        val res = getReservations(FiniteDateRange(monday, wednesday))

        assertEquals(3, res.days.size)

        res.days[0].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            unitOperationTime = testDaycare.operationTimes[0]
                        ),
                        dayChild(
                            testChild_2.id,
                            contractDays = true,
                            unitOperationTime = testDaycare.operationTimes[0]
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[1].let { day ->
            assertEquals(tuesday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            unitOperationTime = testDaycare.operationTimes[1]
                        ),
                        dayChild(
                            testChild_2.id,
                            contractDays = true,
                            unitOperationTime = testDaycare.operationTimes[1]
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[2].let { day ->
            assertEquals(wednesday, day.date)
            assertEquals(emptyList(), day.children)
        }
    }

    @Test
    fun `adding reservations fails if past citizen reservation threshold setting`() {
        val request =
            listOf(testChild_1.id, testChild_2.id).flatMap { child ->
                listOf(
                    DailyReservationRequest.Reservations(
                        child,
                        mockToday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        child,
                        mockToday.plusDays(1),
                        TimeRange(startTime, endTime),
                    )
                )
            }

        assertThrows<BadRequest> { postReservations(request) }
    }

    @Test
    fun `adding absences works`() {
        val request =
            AbsenceRequest(
                childIds = setOf(testChild_1.id, testChild_2.id),
                dateRange = FiniteDateRange(monday, wednesday),
                absenceType = AbsenceType.OTHER_ABSENCE
            )
        postAbsences(request)

        val res = getReservations(FiniteDateRange(monday, wednesday))
        assertEquals(3, res.days.size)

        res.days[0].let { day ->
            assertEquals(LocalDate.of(2021, 11, 15), day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            unitOperationTime = testDaycare.operationTimes[0]
                        ),
                        dayChild(
                            testChild_2.id,
                            contractDays = true,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            unitOperationTime = testDaycare.operationTimes[0]
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[1].let { day ->
            assertEquals(LocalDate.of(2021, 11, 16), day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            unitOperationTime = testDaycare.operationTimes[1]
                        ),
                        dayChild(
                            testChild_2.id,
                            contractDays = true,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            unitOperationTime = testDaycare.operationTimes[1]
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[2].let { day ->
            assertEquals(LocalDate.of(2021, 11, 17), day.date)
            assertEquals(emptyList(), day.children)
        }

        // PRESCHOOL_DAYCARE generates two absences per day (nonbillable and billable)
        assertAbsenceCounts(
            testChild_1.id,
            listOf(LocalDate.of(2021, 11, 15) to 2, LocalDate.of(2021, 11, 16) to 2)
        )

        // DAYCARE generates one absence per day
        assertAbsenceCounts(
            testChild_2.id,
            listOf(LocalDate.of(2021, 11, 15) to 1, LocalDate.of(2021, 11, 16) to 1)
        )
    }

    @Test
    fun `citizen cannot override an absence that was added by staff`() {
        db.transaction { tx ->
            tx.insertTestAbsence(
                childId = testChild_2.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = EvakaUserId(testStaffId.raw)
            )
        }

        val request =
            AbsenceRequest(
                childIds = setOf(testChild_2.id),
                dateRange = FiniteDateRange(monday, tuesday),
                absenceType = AbsenceType.OTHER_ABSENCE
            )
        postAbsences(request)

        val res = getReservations(FiniteDateRange(monday, tuesday))
        assertEquals(2, res.days.size)

        res.days[0].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                        dayChild(testChild_1.id, unitOperationTime = testDaycare.operationTimes[0]),
                        dayChild(
                            testChild_2.id,
                            contractDays = true,
                            absence =
                                AbsenceInfo(type = AbsenceType.PLANNED_ABSENCE, editable = false),
                            unitOperationTime = testDaycare.operationTimes[0]
                        ),
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[1].let { day ->
            assertEquals(tuesday, day.date)
            assertEquals(
                listOf(
                        dayChild(testChild_1.id, unitOperationTime = testDaycare.operationTimes[1]),
                        dayChild(
                            testChild_2.id,
                            contractDays = true,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            unitOperationTime = testDaycare.operationTimes[1]
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        assertAbsenceCounts(testChild_2.id, listOf(monday to 1, tuesday to 1))
    }

    private fun dayChild(
        childId: ChildId,
        unitOperationTime: TimeRange?,
        requiresReservation: Boolean = true,
        shiftCare: Boolean = false,
        contractDays: Boolean = false,
        absence: AbsenceInfo? = null,
        reservations: List<Reservation> = emptyList(),
        attendances: List<OpenTimeRange> = emptyList(),
    ) =
        ReservationResponseDayChild(
            childId,
            requiresReservation,
            shiftCare,
            contractDays,
            absence,
            reservations,
            attendances,
            unitOperationTime
        )

    private fun postReservations(request: List<DailyReservationRequest>) {
        reservationControllerCitizen.postReservations(
            dbInstance(),
            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(0, 0))),
            request
        )
    }

    private fun postAbsences(request: AbsenceRequest) {
        reservationControllerCitizen.postAbsences(
            dbInstance(),
            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0))),
            request,
        )
    }

    private fun getReservations(range: FiniteDateRange): ReservationsResponse {
        return reservationControllerCitizen.getReservations(
            dbInstance(),
            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0))),
            range.start,
            range.end,
        )
    }

    private fun assertAbsenceCounts(childId: ChildId, counts: List<Pair<LocalDate, Int>>) {
        data class QueryResult(val date: LocalDate, val count: Int)

        val expected = counts.map { QueryResult(it.first, it.second) }
        val actual =
            db.read {
                it.createQuery(
                        """
                SELECT date, COUNT(category) as count
                FROM absence WHERE
                child_id = :childId
                GROUP BY date
                ORDER BY date
            """
                    )
                    .bind("childId", childId)
                    .mapTo<QueryResult>()
                    .list()
            }

        assertEquals(expected, actual)
    }
}
