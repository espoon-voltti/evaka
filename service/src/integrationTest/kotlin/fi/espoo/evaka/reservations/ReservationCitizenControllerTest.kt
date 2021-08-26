package fi.espoo.evaka.reservations

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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

    val monday = LocalDate.of(2021, 8, 23)
    val startTime = LocalTime.of(9, 0)
    val endTime = LocalTime.of(17, 0)

    @BeforeEach
    fun before() {
        db.transaction {
            it.resetDatabase()
            it.insertGeneralTestFixtures()
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertTestPlacement(childId = testChild_2.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_2.id)
        }
    }

    @Test
    fun `adding reservations works in a basic case`() {
        postReservatios(
            ReservationRequest(
                children = setOf(testChild_1.id, testChild_2.id),
                reservations = listOf(
                    DailyReservationRequest(monday, startTime, endTime),
                    DailyReservationRequest(monday.plusDays(1), startTime, endTime)
                )
            )
        )

        val res = getReservations(FiniteDateRange(monday, monday.plusDays(2)))

        assertEquals(
            listOf(
                ReservationChild(testChild_1.id, testChild_1.firstName, ""),
                ReservationChild(testChild_2.id, testChild_2.firstName, "")
            ).sortedBy { it.firstName },
            res.children
        )

        val dailyData = res.dailyData
        assertEquals(3, dailyData.size)

        assertEquals(monday, dailyData[0].date)
        assertEquals(
            setOf(
                Reservation(HelsinkiDateTime.of(monday, startTime), HelsinkiDateTime.of(monday, endTime), testChild_1.id),
                Reservation(HelsinkiDateTime.of(monday, startTime), HelsinkiDateTime.of(monday, endTime), testChild_2.id),
            ),
            dailyData[0].reservations.toSet()
        )

        assertEquals(monday.plusDays(1), dailyData[1].date)
        assertEquals(
            setOf(
                Reservation(HelsinkiDateTime.of(monday.plusDays(1), startTime), HelsinkiDateTime.of(monday.plusDays(1), endTime), testChild_1.id),
                Reservation(HelsinkiDateTime.of(monday.plusDays(1), startTime), HelsinkiDateTime.of(monday.plusDays(1), endTime), testChild_2.id),
            ),
            dailyData[1].reservations.toSet()
        )

        assertEquals(monday.plusDays(2), dailyData[2].date)
        assertEquals(0, dailyData[2].reservations.size)
    }

    private fun postReservatios(request: ReservationRequest) {
        val (_, res, _) = http.post("/citizen/reservations")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(AuthenticatedUser.Citizen(testAdult_1.id))
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun getReservations(range: FiniteDateRange): ReservationsResponse {
        val (_, res, result) = http.get("/citizen/reservations?from=${range.start.format(DateTimeFormatter.ISO_DATE)}&to=${range.end.format(DateTimeFormatter.ISO_DATE)}")
            .asUser(AuthenticatedUser.Citizen(testAdult_1.id))
            .responseObject<ReservationsResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }
}
