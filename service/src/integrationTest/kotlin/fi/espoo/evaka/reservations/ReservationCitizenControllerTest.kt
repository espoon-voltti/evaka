// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

class ReservationCitizenControllerTest : FullApplicationTest() {

    val testDate = LocalDate.now().let { it.minusDays(it.dayOfWeek.value - 1L) }.plusWeeks(3)
    val startTime = LocalTime.of(9, 0)
    val endTime = LocalTime.of(17, 0)

    @BeforeEach
    fun before() {
        db.transaction {
            it.resetDatabase()
            it.insertGeneralTestFixtures()
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = testDate, endDate = testDate.plusDays(1))
            it.insertTestPlacement(childId = testChild_2.id, unitId = testDaycare.id, startDate = testDate, endDate = testDate.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_2.id)
        }
    }

    @Test
    fun `adding reservations works in a basic case`() {
        postReservations(
            listOf(testChild_1.id, testChild_2.id).flatMap { child ->
                listOf(
                    DailyReservationRequest(child, testDate, listOf(TimeRange(startTime, endTime))),
                    DailyReservationRequest(child, testDate.plusDays(1), listOf(TimeRange(startTime, endTime)))
                )
            },
        )

        val res = getReservations(FiniteDateRange(testDate, testDate.plusDays(2)))

        assertEquals(
            listOf(
                ReservationChild(testChild_1.id, testChild_1.firstName, "", testDate, testDate.plusDays(1), setOf(1, 2, 3, 4, 5), false),
                ReservationChild(testChild_2.id, testChild_2.firstName, "", testDate, testDate.plusDays(1), setOf(1, 2, 3, 4, 5), false)
            ).sortedBy { it.firstName },
            res.children
        )

        val dailyData = res.dailyData
        assertEquals(3, dailyData.size)

        assertEquals(testDate, dailyData[0].date)
        assertEquals(
            setOf(
                ChildDailyData(testChild_1.id, null, listOf(Reservation(startTime.toString(), endTime.toString()))),
                ChildDailyData(testChild_2.id, null, listOf(Reservation(startTime.toString(), endTime.toString())))
            ),
            dailyData[0].children.toSet()
        )

        assertEquals(testDate.plusDays(1), dailyData[1].date)
        assertEquals(
            setOf(
                ChildDailyData(testChild_1.id, null, listOf(Reservation(startTime.toString(), endTime.toString()))),
                ChildDailyData(testChild_2.id, null, listOf(Reservation(startTime.toString(), endTime.toString()))),
            ),
            dailyData[1].children.toSet()
        )

        assertEquals(testDate.plusDays(2), dailyData[2].date)
        assertEquals(0, dailyData[2].children.size)
    }

    @Test
    fun `adding reservations fails if past citizen reservation threshold setting`() {
        val request = listOf(testChild_1.id, testChild_2.id).flatMap { child ->
            listOf(
                DailyReservationRequest(child, LocalDate.now(), listOf(TimeRange(startTime, endTime))),
                DailyReservationRequest(child, LocalDate.now().plusDays(1), listOf(TimeRange(startTime, endTime)))
            )
        }
        postReservations(request, 400)
    }

    private fun postReservations(request: List<DailyReservationRequest>, expectedStatus: Int? = 200) {
        val (_, res, _) = http.post("/citizen/reservations")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(AuthenticatedUser.Citizen(testAdult_1.id.raw))
            .response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun getReservations(range: FiniteDateRange): ReservationsResponse {
        val (_, res, result) = http.get("/citizen/reservations?from=${range.start.format(DateTimeFormatter.ISO_DATE)}&to=${range.end.format(DateTimeFormatter.ISO_DATE)}")
            .asUser(AuthenticatedUser.Citizen(testAdult_1.id.raw))
            .responseObject<ReservationsResponse>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }
}
