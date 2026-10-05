// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.DaycareId
import evaka.core.shared.TitaniaErrorId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.shared.security.actionrule.AccessControlFilter
import evaka.core.shared.security.actionrule.forTable
import evaka.core.titania.TitaniaErrorType
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_TITANIA_ERRORS,
                        )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getTitaniaErrors(filter)
                }
            }
            .also { Audit.TitaniaReportRead.log() }
    }

    @DeleteMapping("/employee/reports/titania-errors/{errorId}")
    fun clearTitaniaErrors(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable errorId: TitaniaErrorId,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.TitaniaError.DELETE,
                        errorId,
                    )
                    tx.deleteTitaniaError(errorId)
                }
            }
            .also { Audit.TitaniaReportDelete.log(targetId = AuditId(errorId)) }
    }
}

fun Database.Read.getTitaniaErrors(
    filter: AccessControlFilter<DaycareId>
): List<TitaniaErrorReportRow> {
    val dbRows = createQuery {
        sql(
            """
            WITH daycare_acls AS (
                SELECT DISTINCT te.employee_id, d.name as unit_name
                FROM titania_errors te
                LEFT JOIN daycare_acl da ON te.employee_id = da.employee_id
                LEFT JOIN daycare d ON da.daycare_id = d.id
                WHERE ${predicate(filter.forTable("d"))}
            )
            SELECT
                te.request_time,
                emp.first_name,
                emp.last_name,
                emp.employee_number,
                te.id,
                te.error_type,
                te.shift_date,
                te.shift_begins,
                te.shift_ends,
                te.overlapping_shift_begins,
                te.overlapping_shift_ends,
                COALESCE(emp_units.unit_names, 'Ei luvituksia') as unit_names
            FROM titania_errors te
            JOIN employee emp ON te.employee_id = emp.id
            JOIN (
                SELECT
                    employee_id,
                    STRING_AGG(unit_name, ', ' ORDER BY unit_name) as unit_names
                    FROM daycare_acls
                    GROUP BY employee_id
            ) emp_units ON te.employee_id = emp_units.employee_id
            ORDER BY request_time, unit_names, last_name, first_name, shift_date, shift_begins;
            """
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
                                    if (it.employeeNumber != null) it.employeeNumber
                                    else it.lastName + it.firstName
                                }
                                .map { employeeEntry ->
                                    TitaniaErrorEmployee(
                                        employeeEntry.value[0].lastName +
                                            ' ' +
                                            employeeEntry.value[0].firstName,
                                        employeeEntry.value[0].employeeNumber ?: "",
                                        employeeEntry.value.map { shiftEntry ->
                                            TitaniaShiftError(
                                                shiftEntry.id,
                                                shiftEntry.errorType,
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

fun Database.Transaction.deleteTitaniaError(id: TitaniaErrorId) {
    createUpdate {
        sql(
            """
                DELETE FROM titania_errors 
                WHERE id = ${bind(id)}
            """
        )
    }
        .execute()
}

data class TitaniaDbRow(
    val requestTime: HelsinkiDateTime,
    val firstName: String,
    val lastName: String,
    val employeeNumber: String?,
    val id: TitaniaErrorId,
    val errorType: TitaniaErrorType,
    val shiftDate: LocalDate,
    val shiftBegins: LocalTime,
    val shiftEnds: LocalTime,
    val overlappingShiftBegins: LocalTime?,
    val overlappingShiftEnds: LocalTime?,
    val unitNames: String,
)

data class TitaniaShiftError(
    val id: TitaniaErrorId,
    val errorType: TitaniaErrorType,
    val shiftDate: LocalDate,
    val shiftBegins: LocalTime,
    val shiftEnds: LocalTime,
    val overlappingShiftBegins: LocalTime?,
    val overlappingShiftEnds: LocalTime?,
)

data class TitaniaErrorEmployee(
    val employeeName: String,
    val employeeNumber: String,
    val shiftErrors: List<TitaniaShiftError>,
)

data class TitaniaErrorUnit(val unitName: String, val employees: List<TitaniaErrorEmployee>)

data class TitaniaErrorReportRow(
    val requestTime: HelsinkiDateTime,
    val units: List<TitaniaErrorUnit>,
)
