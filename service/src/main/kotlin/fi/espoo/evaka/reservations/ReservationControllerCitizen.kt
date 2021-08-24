package fi.espoo.evaka.reservations

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.isWeekend
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.json.Json
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
        Audit.AttendanceReservationCitizenRead.log(targetId = user.id)
        user.requireOneOfRoles(UserRole.CITIZEN_WEAK, UserRole.END_USER)

        return db.read { it.getReservations(user.id, FiniteDateRange(from, to)) }
    }

    @PostMapping("/citizen/reservations")
    fun postReservations(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: ReservationRequest
    ) {
        Audit.AttendanceReservationCitizenCreate.log(targetId = body.children.joinToString())
        user.requireOneOfRoles(UserRole.CITIZEN_WEAK, UserRole.END_USER)

        val reservations = body.children.flatMap { childId ->
            body.reservations.map { res ->
                Reservation(
                    childId = childId,
                    startTime = HelsinkiDateTime.of(
                        date = res.date,
                        time = res.startTime
                    ),
                    endTime = HelsinkiDateTime.of(
                        date = if (res.endTime.isAfter(res.startTime)) res.date else res.date.plusDays(1),
                        time = res.endTime
                    )
                )
            }
        }

        db.transaction { createReservations(it, user.id, reservations) }
    }
}

fun createReservations(tx: Database.Transaction, userId: UUID, reservations: List<Reservation>) {
    // tx.clearOldCitizenAbsences(reservations) todo: add created_by_citizen column
    tx.clearOldCitizenReservations(reservations)
    tx.insertValidReservations(userId, reservations)
}

fun Database.Transaction.clearOldCitizenReservations(reservations: List<Reservation>) {
    val batch = prepareBatch(
        """
        DELETE FROM attendance_reservation 
        WHERE child_id = :childId AND start_date = :date AND created_by_employee_id IS NULL
        """.trimIndent()
    )

    reservations.forEach { res ->
        batch
            .bind("childId", res.childId)
            .bind("date", res.startTime.toLocalDate())
    }

    batch.execute()
}

fun Database.Transaction.insertValidReservations(userId: UUID, reservations: List<Reservation>) {
    val batch = prepareBatch(
        """
        INSERT INTO attendance_reservation (child_id, start_time, end_time, created_by_guardian_id, created_by_employee_id)
        SELECT :childId, :start, :end, :userId, NULL
        FROM placement pl 
        JOIN daycare d ON d.id = pl.unit_id AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
        JOIN guardian g ON g.child_id = pl.child_id AND g.guardian_id = :userId
        WHERE 
            pl.child_id = :childId AND 
            daterange(pl.start_date, pl.end_date, '[]') @> :date AND 
            extract(DOW FROM :date) = ANY(d.operation_days) AND 
            NOT EXISTS(SELECT 1 FROM holiday h WHERE h.date = :date) AND
            NOT EXISTS(SELECT 1 FROM absence ab WHERE ab.child_id = :childId AND ab.date = :date)
        ON CONFLICT DO NOTHING;
        """.trimIndent()
    )

    reservations.forEach { res ->
        batch
            .bind("userId", userId)
            .bind("childId", res.childId)
            .bind("start", res.startTime)
            .bind("end", res.endTime)
            .bind("date", res.startTime.toLocalDate())
            .add()
    }

    batch.execute()
}

data class ReservationRequest(
    val children: Set<UUID>,
    val reservations: List<DailyReservationRequest>
)

data class DailyReservationRequest(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
)

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
    val childId: UUID
)

fun Database.Read.getReservations(guardianId: UUID, range: FiniteDateRange): List<DailyReservationData> {
    if (range.durationInDays() > 366) throw BadRequest("Range too long")

    return createQuery(
        """
SELECT
    t::date AS date,
    EXISTS(SELECT 1 FROM holiday h WHERE h.date = t::date) AS is_holiday,
    coalesce(
        jsonb_agg(
            jsonb_build_object('startTime', ar.start_time, 'endTime', ar.end_time, 'childId', ar.child_id)
        ) FILTER (WHERE ar.id IS NOT NULL), 
        '[]'
    ) AS reservations
FROM generate_series(:start, :end, '1 day') t
JOIN guardian g ON g.guardian_id = :guardianId
LEFT JOIN attendance_reservation ar ON ar.child_id = g.child_id AND ar.start_date = t::date
GROUP BY date, is_holiday
        """.trimIndent()
    )
        .bind("guardianId", guardianId)
        .bind("start", range.start)
        .bind("end", range.end)
        .mapTo<DailyReservationData>()
        .list()
        .filter { !it.date.isWeekend() }
}
