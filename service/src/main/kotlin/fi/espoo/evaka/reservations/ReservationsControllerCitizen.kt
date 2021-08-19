import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@RestController
@RequestMapping("/citizen")
class ReservationsControllerCitizen {
    @GetMapping("/reservations")
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
    val reservations: List<Reservation>
)

data class Reservation(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val childId: ChildId
)

fun Database.Read.getReservations(guardianId: UUID, range: FiniteDateRange): List<DailyReservationData> {
    if (range.durationInDays() > 366) throw BadRequest("Range too long")

    data class QueryResult(
        val date: LocalDate,
        val isHoliday: Boolean,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val childId: ChildId
    )

    data class DailyGroup(
        val date: LocalDate,
        val isHoliday: Boolean
    )

    return createQuery(
        """
        SELECT 
            t::date AS date,
            EXISTS(SELECT 1 FROM holiday h WHERE h.date = t::date) AS is_holiday,
            ar.start_time,
            ar.end_time,
            ar.child_id
        FROM generate_series(:start, :end, '1 day') t
        JOIN guardian g ON g.guardian_id = :guardianId
        LEFT JOIN attendance_reservation ar ON ar.child_id = g.child_id AND ar.start_time::date = t::date
        """.trimIndent()
    )
        .bind("guardianId", guardianId)
        .bind("start", range.start)
        .bind("end", range.start)
        .mapTo<QueryResult>()
        .list()
        .groupBy(
            keySelector = { DailyGroup(it.date, it.isHoliday) },
            valueTransform = { Reservation(it.startTime, it.endTime, it.childId) }
        )
        .map {
            DailyReservationData(
                date = it.key.date,
                isHoliday = it.key.isHoliday,
                reservations = it.value
            )
        }
}
