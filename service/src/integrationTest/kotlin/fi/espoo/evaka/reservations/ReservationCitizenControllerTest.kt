// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.snDaycareContractDays10
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.withMockedTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReservationCitizenControllerTest : FullApplicationTest(resetDbBeforeEach = true) {
    // Monday
    private val mockToday: LocalDate = LocalDate.of(2021, 11, 1)

    // Also Monday
    private val testDate = LocalDate.of(2021, 11, 15)
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
                startDate = testDate,
                endDate = testDate.plusDays(1)
            )
            it.insertTestPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = testDate,
                    endDate = testDate.plusDays(1)
                )
                .let { placementId ->
                    it.insertTestServiceNeed(
                        confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        placementId = placementId,
                        period = FiniteDateRange(testDate, testDate.plusDays(1)),
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
    fun `adding reservations works in a basic case`() {
        postReservations(
            listOf(testChild_1.id, testChild_2.id).flatMap { child ->
                listOf(
                    DailyReservationRequest(
                        child,
                        testDate,
                        listOf(Reservation.Times(startTime, endTime)),
                        absent = false
                    ),
                    DailyReservationRequest(
                        child,
                        testDate.plusDays(1),
                        listOf(Reservation.Times(startTime, endTime)),
                        absent = false
                    )
                )
            }
        )

        val res = getReservations(FiniteDateRange(testDate, testDate.plusDays(2)))

        assertEquals(
            listOf(
                // Sorted by date of birth, oldest child first
                ReservationChild(
                    id = testChild_2.id,
                    firstName = testChild_2.firstName,
                    lastName = testChild_2.lastName,
                    preferredName = "",
                    duplicateOf = null,
                    imageId = null,
                    placements = listOf(FiniteDateRange(testDate, testDate.plusDays(1))),
                    serviceNeeds = listOf(FiniteDateRange(testDate, testDate.plusDays(1))),
                    upcomingPlacementType = null,
                    maxOperationalDays = setOf(1, 2, 3, 4, 5),
                    inShiftCareUnit = false,
                    hasContractDays = true
                ),
                ReservationChild(
                    id = testChild_1.id,
                    firstName = testChild_1.firstName,
                    lastName = testChild_1.lastName,
                    preferredName = "",
                    duplicateOf = null,
                    imageId = null,
                    placements = listOf(FiniteDateRange(testDate, testDate.plusDays(1))),
                    serviceNeeds = emptyList(),
                    upcomingPlacementType = null,
                    maxOperationalDays = setOf(1, 2, 3, 4, 5),
                    inShiftCareUnit = false,
                    hasContractDays = false
                )
            ),
            res.children
        )

        val dailyData = res.dailyData
        assertEquals(3, dailyData.size)

        assertEquals(testDate, dailyData[0].date)
        assertEquals(
            setOf(
                ChildDailyData(
                    testChild_1.id,
                    false,
                    null,
                    listOf(Reservation.Times(startTime, endTime)),
                    listOf(),
                ),
                ChildDailyData(
                    testChild_2.id,
                    false,
                    null,
                    listOf(Reservation.Times(startTime, endTime)),
                    listOf(),
                )
            ),
            dailyData[0].children.toSet()
        )

        assertEquals(testDate.plusDays(1), dailyData[1].date)
        assertEquals(
            setOf(
                ChildDailyData(
                    testChild_1.id,
                    false,
                    null,
                    listOf(Reservation.Times(startTime, endTime)),
                    listOf(),
                ),
                ChildDailyData(
                    testChild_2.id,
                    false,
                    null,
                    listOf(Reservation.Times(startTime, endTime)),
                    listOf(),
                )
            ),
            dailyData[1].children.toSet()
        )

        assertEquals(testDate.plusDays(2), dailyData[2].date)
        assertEquals(0, dailyData[2].children.size)
    }

    @Test
    fun `adding a reservation and an absence works`() {
        postReservations(
            listOf(
                DailyReservationRequest(
                    testChild_1.id,
                    testDate,
                    listOf(Reservation.Times(startTime, endTime)),
                    absent = false
                ),
                DailyReservationRequest(
                    testChild_1.id,
                    testDate.plusDays(1),
                    listOf(Reservation.Times(startTime, endTime)),
                    absent = true
                )
            )
        )

        val res = getReservations(FiniteDateRange(testDate, testDate.plusDays(2)))

        val dailyData = res.dailyData
        assertEquals(3, dailyData.size)

        assertEquals(testDate, dailyData[0].date)
        assertEquals(
            setOf(
                ChildDailyData(
                    testChild_1.id,
                    false,
                    null,
                    listOf(Reservation.Times(startTime, endTime)),
                    listOf(),
                )
            ),
            dailyData[0].children.toSet()
        )

        assertEquals(testDate.plusDays(1), dailyData[1].date)
        assertEquals(
            setOf(
                ChildDailyData(
                    testChild_1.id,
                    false,
                    AbsenceType.OTHER_ABSENCE,
                    emptyList(),
                    listOf(),
                )
            ),
            dailyData[1].children.toSet()
        )

        assertEquals(testDate.plusDays(2), dailyData[2].date)
        assertEquals(0, dailyData[2].children.size)
    }

    @Test
    fun `adding reservations fails if past citizen reservation threshold setting`() {
        val request =
            listOf(testChild_1.id, testChild_2.id).flatMap { child ->
                listOf(
                    DailyReservationRequest(
                        child,
                        mockToday,
                        listOf(Reservation.Times(startTime, endTime)),
                        absent = false
                    ),
                    DailyReservationRequest(
                        child,
                        mockToday.plusDays(1),
                        listOf(Reservation.Times(startTime, endTime)),
                        absent = false
                    )
                )
            }
        postReservations(request, 400)
    }

    @Test
    fun `adding absences works`() {
        val request =
            AbsenceRequest(
                childIds = setOf(testChild_1.id, testChild_2.id),
                dateRange = FiniteDateRange(testDate, testDate.plusDays(2)),
                absenceType = AbsenceType.OTHER_ABSENCE
            )
        postAbsences(request)

        val res = getReservations(FiniteDateRange(testDate, testDate.plusDays(2)))
        assertEquals(3, res.dailyData.size)

        assertEquals(LocalDate.of(2021, 11, 15), res.dailyData[0].date)
        assertEquals(
            setOf(
                ChildDailyData(
                    childId = testChild_1.id,
                    markedByEmployee = false,
                    absence = AbsenceType.OTHER_ABSENCE,
                    reservations = listOf(),
                    attendances = listOf(),
                ),
                ChildDailyData(
                    childId = testChild_2.id,
                    markedByEmployee = false,
                    absence = AbsenceType.OTHER_ABSENCE,
                    reservations = listOf(),
                    attendances = listOf(),
                )
            ),
            res.dailyData[0].children.toSet()
        )

        assertEquals(LocalDate.of(2021, 11, 16), res.dailyData[1].date)
        assertEquals(
            setOf(
                ChildDailyData(
                    childId = testChild_1.id,
                    markedByEmployee = false,
                    absence = AbsenceType.OTHER_ABSENCE,
                    reservations = listOf(),
                    attendances = listOf(),
                ),
                ChildDailyData(
                    childId = testChild_2.id,
                    markedByEmployee = false,
                    absence = AbsenceType.OTHER_ABSENCE,
                    reservations = listOf(),
                    attendances = listOf(),
                )
            ),
            res.dailyData[1].children.toSet()
        )

        assertEquals(LocalDate.of(2021, 11, 17), res.dailyData[2].date)
        assertEquals(listOf(), res.dailyData[2].children)

        // PRESCHOOL_DAYCARE generates two absences per day (nonbillable and billable)
        assertAbsenceCounts(
            testChild_1.id,
            listOf(LocalDate.of(2021, 11, 15) to 2, LocalDate.of(2021, 11, 16) to 2)
        )

        // DAYCARE generates one absences per day
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
                date = testDate,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = EvakaUserId(testStaffId.raw)
            )
        }

        val request =
            AbsenceRequest(
                childIds = setOf(testChild_2.id),
                dateRange = FiniteDateRange(testDate, testDate.plusDays(1)),
                absenceType = AbsenceType.OTHER_ABSENCE
            )
        postAbsences(request)

        val res = getReservations(FiniteDateRange(testDate, testDate.plusDays(1)))
        assertEquals(2, res.dailyData.size)

        assertEquals(testDate, res.dailyData[0].date)
        assertEquals(
            setOf(
                ChildDailyData(
                    childId = testChild_2.id,
                    markedByEmployee = true,
                    absence = AbsenceType.PLANNED_ABSENCE,
                    reservations = listOf(),
                    attendances = listOf(),
                )
            ),
            res.dailyData[0].children.toSet()
        )

        assertEquals(testDate.plusDays(1), res.dailyData[1].date)
        assertEquals(
            setOf(
                ChildDailyData(
                    childId = testChild_2.id,
                    markedByEmployee = false,
                    absence = AbsenceType.OTHER_ABSENCE,
                    reservations = listOf(),
                    attendances = listOf(),
                )
            ),
            res.dailyData[1].children.toSet()
        )

        assertAbsenceCounts(testChild_2.id, listOf(testDate to 1, testDate.plusDays(1) to 1))
    }

    private fun postReservations(
        request: List<DailyReservationRequest>,
        expectedStatus: Int? = 200
    ) {
        val (_, res, _) =
            http
                .post("/citizen/reservations")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG))
                .withMockedTime(HelsinkiDateTime.of(mockToday, LocalTime.of(0, 0)))
                .response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun postAbsences(request: AbsenceRequest) {
        val (_, res, _) =
            http
                .post("/citizen/absences")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG))
                .withMockedTime(HelsinkiDateTime.of(mockToday, LocalTime.of(0, 0)))
                .response()

        assertEquals(200, res.statusCode)
    }

    private fun getReservations(range: FiniteDateRange): ReservationsResponse {
        val (_, res, result) =
            http
                .get(
                    "/citizen/reservations?from=${range.start.format(DateTimeFormatter.ISO_DATE)}&to=${
            range.end.format(
                DateTimeFormatter.ISO_DATE
            )
            }"
                )
                .asUser(AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG))
                .responseObject<ReservationsResponse>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
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
