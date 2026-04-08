// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FuturePreschoolersReport(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/future-preschoolers")
    fun getFuturePreschoolersReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<FuturePreschoolersReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FUTURE_PRESCHOOLERS,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getFuturePreschoolerRows(clock.today())
                }
            }
            .also { Audit.FuturePreschoolers.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/employee/reports/future-preschoolers/units")
    fun getFuturePreschoolersUnitsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<PreschoolUnitsReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FUTURE_PRESCHOOLERS,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getPreschoolUnitsRows(clock.today())
                }
            }
            .also { Audit.FuturePreschoolers.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/employee/reports/future-preschoolers/source-units")
    fun getFuturePreschoolersSourceUnitsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<SourceUnitsReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FUTURE_PRESCHOOLERS,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getSourceUnitsRows(clock.today())
                }
            }
            .also { Audit.FuturePreschoolers.log(meta = mapOf("count" to it.size)) }
    }
}

fun Database.Read.getFuturePreschoolerRows(today: LocalDate): List<FuturePreschoolersReportRow> {
    // Before June display children starting preschool in the current year,
    // from June onwards display children starting preschool in the next year
    val preschoolStartYear = if (today.monthValue >= 6) today.year + 1 else today.year

    return createQuery {
            sql(
                """
SELECT p.id,
    p.last_name AS child_last_name,
    p.first_name AS child_first_name,
    p.date_of_birth AS child_date_of_birth,
    p.language AS child_language,
    p.street_address AS child_address,
    p.postal_code AS child_postal_code,
    upper(p.post_office) AS child_post_office,
    d.id AS unit_id,
    d.name AS unit_name,
    array_remove(ARRAY[
        CASE WHEN sn.shift_care = 'FULL'::shift_care_type OR sn.shift_care = 'INTERMITTENT'::shift_care_type THEN 'SHIFT_CARE' END,
        CASE WHEN sno.name_fi like 'Kaksivuotinen%' THEN 'TWO_YEAR_PRESCHOOL' END
    ], NULL) AS options
FROM person p
JOIN placement pl ON pl.child_id = p.id AND pl.start_date < ${bind(today)} AND pl.end_date >= ${bind(today)}
JOIN daycare d ON d.id = pl.unit_id
LEFT JOIN service_need sn ON sn.placement_id = p.id and sn.start_date < ${bind(today)} AND sn.end_date >= ${bind(today)}
LEFT JOIN service_need_option sno on sn.option_id = sno.id
WHERE CASE WHEN sno.name_fi like 'Kaksivuotinen%' THEN
    extract(year from p.date_of_birth) = ${bind(preschoolStartYear - 5)}
ELSE
    extract(year from p.date_of_birth) = ${bind(preschoolStartYear - 6)}
END
            """
            )
        }
        .toList<FuturePreschoolersReportRow>()
}

fun Database.Read.getPreschoolUnitsRows(today: LocalDate): List<PreschoolUnitsReportRow> =
    createQuery {
            sql(
                """
SELECT d.id, 
    d.name AS unit_name,
    d.street_address AS address,
    d.postal_code AS postal_code,
    d.post_office as post_office,
    (
        SELECT count(id) 
        FROM daycare_caretaker 
        WHERE group_id IN (
            SELECT id 
            FROM daycare_group 
            WHERE daycare_id = d.id
        ) AND start_date < ${bind(today)} AND end_date >= ${bind(today)}
    ) * 7 AS unit_size,
    array_remove(ARRAY[
        CASE WHEN d.provider_type != 'MUNICIPAL' THEN 'PRIVATE' END,
        CASE WHEN d.with_school THEN 'WITH_SCHOOL' END,
        CASE WHEN d.provides_shift_care THEN 'SHIFT_CARE' END
    ], NULL) AS options
FROM daycare d
WHERE d.type && '{PRESCHOOL}'::care_types[] AND
d.opening_date <= ${bind(today)} AND (d.closing_date IS NULL OR d.closing_date >= ${bind(today)})
            """
            )
        }
        .toList<PreschoolUnitsReportRow>()

fun Database.Read.getSourceUnitsRows(today: LocalDate): List<SourceUnitsReportRow> =
    createQuery {
            sql(
                """
SELECT d.id,
    d.name AS unit_name,
    d.street_address AS address,
    d.postal_code AS postal_code,
    d.post_office as post_office
FROM daycare d
WHERE d.opening_date <= ${bind(today)} AND (d.closing_date IS NULL OR d.closing_date >= ${bind(today)})
            """
            )
        }
        .toList<SourceUnitsReportRow>()

data class FuturePreschoolersReportRow(
    val id: ChildId,
    val childLastName: String,
    val childFirstName: String,
    val childDateOfBirth: LocalDate,
    val childLanguage: String?,
    val childAddress: String,
    val childPostalCode: String,
    val childPostOffice: String,
    val unitId: DaycareId,
    val unitName: String,
    val options: List<String>,
)

data class PreschoolUnitsReportRow(
    val id: DaycareId,
    val unitName: String,
    val address: String,
    val postalCode: String,
    val postOffice: String,
    val unitSize: Int,
    val options: List<String>,
)

data class SourceUnitsReportRow(
    val id: DaycareId,
    val unitName: String,
    val address: String,
    val postalCode: String,
    val postOffice: String,
)
