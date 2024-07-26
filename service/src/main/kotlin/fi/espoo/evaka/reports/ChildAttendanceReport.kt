// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeInterval
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildAttendanceReportController(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/child-attendance/{childId}")
    fun getChildAttendanceReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): List<ChildAttendanceReportRow> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val range = FiniteDateRange(from, to)
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_ATTENDANCE_REPORT,
                        childId
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    tx.getChildAttendanceRows(childId, range)
                }
            }
            .also { Audit.ChildAttendanceReportRead.log(targetId = AuditId(childId)) }
    }
}

private fun Database.Read.getChildAttendanceRows(
    childId: ChildId,
    range: FiniteDateRange
): List<ChildAttendanceReportRow> {
    data class SimpleReservation(
        val date: LocalDate,
        val startTime: LocalTime,
        val endTime: LocalTime
    )
    val reservations =
        createQuery {
                sql(
                    """
        SELECT date, start_time, end_time
        FROM attendance_reservation
        WHERE child_id = ${bind(childId)} AND between_start_and_end(${bind(range)}, date) 
            AND start_time IS NOT NULL AND end_time IS NOT NULL
    """
                )
            }
            .toList<SimpleReservation>()
            .groupBy(
                keySelector = { it.date },
                valueTransform = { TimeRange(it.startTime, it.endTime) }
            )

    data class SimpleAttendance(
        val date: LocalDate,
        val startTime: LocalTime,
        val endTime: LocalTime?
    )
    val attendances =
        createQuery {
                sql(
                    """
        SELECT date, start_time, end_time
        FROM child_attendance
        WHERE child_id = ${bind(childId)} AND between_start_and_end(${bind(range)}, date)
    """
                )
            }
            .toList<SimpleAttendance>()
            .groupBy(
                keySelector = { it.date },
                valueTransform = { TimeInterval(it.startTime, it.endTime) }
            )

    data class SimpleAbsence(
        val date: LocalDate,
        val absenceType: AbsenceType,
        val category: AbsenceCategory
    )
    val absences =
        createQuery {
                sql(
                    """
        SELECT date, absence_type, category
        FROM absence
        WHERE child_id = ${bind(childId)} AND between_start_and_end(${bind(range)}, date)
    """
                )
            }
            .toList<SimpleAbsence>()
            .associateBy(
                keySelector = { Pair(it.date, it.category) },
                valueTransform = { it.absenceType }
            )

    return range
        .dates()
        .map { date ->
            ChildAttendanceReportRow(
                date = date,
                reservations = reservations[date]?.sortedBy { it.start } ?: emptyList(),
                attendances = attendances[date]?.sortedBy { it.start } ?: emptyList(),
                billableAbsence = absences[Pair(date, AbsenceCategory.BILLABLE)],
                nonbillableAbsence = absences[Pair(date, AbsenceCategory.NONBILLABLE)]
            )
        }
        .toList()
}

data class ChildAttendanceReportRow(
    val date: LocalDate,
    val reservations: List<TimeRange>,
    val attendances: List<TimeInterval>,
    val billableAbsence: AbsenceType?,
    val nonbillableAbsence: AbsenceType?
)
