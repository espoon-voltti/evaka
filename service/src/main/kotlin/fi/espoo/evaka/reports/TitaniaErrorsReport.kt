// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TitaniaErrorReport(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/titania-errors")
    fun getTitaniaErrorsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<TitaniaErrorReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_TITANIA_ERRORS,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getTitaniaErrors()
                }
            }
            .also { Audit.TitaniaReportRead.log() }
    }
}

fun Database.Read.getTitaniaErrors(): List<TitaniaErrorReportRow> {
    val dbRows =
        createQuery {
                sql(
                    """
            SELECT
                te.request_time,
                emp.first_name,
                emp.last_name,
		emp.employee_number,
                te.shift_date,
                te.shift_begins,
                te.shift_ends,
                te.overlapping_shift_begins,
                te.overlapping_shift_ends,
                COALESCE(emp_units.unit_names, 'Ei luvituksia') as unit_names
            FROM titania_errors te
            JOIN employee emp ON te.employee_id = emp.id
            LEFT JOIN (
                SELECT
                    employee_id,
                    STRING_AGG(dc.name, ', ') as unit_names
                    FROM daycare_acl acl
                    JOIN daycare dc ON dc.id = acl.daycare_id
                    WHERE acl.employee_id IN (
                        SELECT employee_id
                        FROM titania_errors
                    )
                    GROUP BY employee_id
            ) emp_units on emp_units.employee_id = te.employee_id
            ORDER BY request_time, unit_names, last_name, first_name, shift_date;
            """
                        .trimIndent()
                )
            }
            .toList<TitaniaDbRow>()

    return dbRows
        .groupBy { it.requestTime }
        .map { requestEntry ->
            TitaniaErrorReportRow(
                requestEntry.key,
                requestEntry.value
                    .groupBy { it.unitNames }
                    .map { unitEntry ->
                        TitaniaErrorUnit(
                            unitEntry.key,
                            unitEntry.value
                                .groupBy {
                                    if (it.employeeNumber != "") it.employeeNumber
                                    else it.lastName + it.firstName
                                }
                                .map { employeeEntry ->
                                    TitaniaErrorEmployee(
                                        employeeEntry.value[0].lastName +
                                            ' ' +
                                            employeeEntry.value[0].firstName,
                                        employeeEntry.value[0].employeeNumber ?: "",
                                        employeeEntry.value.map { shiftEntry ->
                                            TitaniaErrorConflict(
                                                shiftEntry.shiftDate,
                                                shiftEntry.shiftBegins,
                                                shiftEntry.shiftEnds,
                                                shiftEntry.overlappingShiftBegins,
                                                shiftEntry.overlappingShiftEnds,
                                            )
                                        },
                                    )
                                },
                        )
                    },
            )
        }
}

data class TitaniaDbRow(
    val requestTime: HelsinkiDateTime,
    val firstName: String,
    val lastName: String,
    val employeeNumber: String?,
    val shiftDate: LocalDate,
    val shiftBegins: LocalTime,
    val shiftEnds: LocalTime,
    val overlappingShiftBegins: LocalTime,
    val overlappingShiftEnds: LocalTime,
    val unitNames: String,
)

data class TitaniaErrorConflict(
    val shiftDate: LocalDate,
    val shiftBegins: LocalTime,
    val shiftEnds: LocalTime,
    val overlappingShiftBegins: LocalTime,
    val overlappingShiftEnds: LocalTime,
)

data class TitaniaErrorEmployee(
    val employeeName: String,
    val employeeNumber: String,
    val conflictingShifts: List<TitaniaErrorConflict>,
)

data class TitaniaErrorUnit(val unitName: String, val employees: List<TitaniaErrorEmployee>)

data class TitaniaErrorReportRow(
    val requestTime: HelsinkiDateTime,
    val units: List<TitaniaErrorUnit>,
)
