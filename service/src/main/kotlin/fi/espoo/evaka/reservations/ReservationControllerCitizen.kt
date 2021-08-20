package fi.espoo.evaka.reservations

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.json.Json
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@RestController
class ReservationControllerCitizen {

    @GetMapping("/citizen/reservations")
    fun getReservations(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<DailyReservationData> {
        // TODO: Audit
        user.requireOneOfRoles(UserRole.CITIZEN_WEAK, UserRole.END_USER)

        return db.read { it.getReservations(user.id, FiniteDateRange(from, to)) }
    }
}

data class DailyReservationData(
    val date: LocalDate,
    val isHoliday: Boolean,
    @Json
    val reservations: List<Reservation>
)

@Json
data class Reservation(
    val startTime: HelsinkiDateTime,
    val endTime: HelsinkiDateTime,
    val childId: ChildId
)

fun Database.Read.getReservations(guardianId: UUID, range: FiniteDateRange): List<DailyReservationData> {
    if (range.durationInDays() > 366) throw BadRequest("Range too long")

    return createQuery(
        """
SELECT
    date,
    is_holiday,
    coalesce(jsonb_agg(reservation) FILTER ( WHERE reservation IS NOT NULL ), '[]') AS reservations
FROM (
    SELECT
        t::date AS date,
        EXISTS(SELECT 1 FROM holiday h WHERE h.date = t::date) AS is_holiday,
        CASE WHEN ar.id IS NOT NULL THEN json_build_object('startTime', ar.start_time, 'endTime', ar.end_time, 'childId', ar.child_id) END AS reservation
    FROM generate_series(:start, :end, '1 day') t
    JOIN guardian g ON g.guardian_id = :guardianId
    LEFT JOIN attendance_reservation ar ON ar.child_id = g.child_id AND ar.start_date = t::date
) AS rows
GROUP BY date, is_holiday
        """.trimIndent()
    )
        .bind("guardianId", guardianId)
        .bind("start", range.start)
        .bind("end", range.end)
        .mapTo<DailyReservationData>()
        .list()
}
