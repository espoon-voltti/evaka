// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FuturePreschoolersReport(private val accessControl: AccessControl) {
    @GetMapping(
        "/reports/future-preschoolers", // deprecated
        "/employee/reports/future-preschoolers"
    )
    fun getFuturePreschoolersReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<FuturePreschoolersReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FUTURE_PRESCHOOLERS
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
                        Action.Global.READ_FUTURE_PRESCHOOLERS
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
        clock: EvakaClock
    ): List<SourceUnitsReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FUTURE_PRESCHOOLERS
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.geSourceUnitsRows(clock.today())
                }
            }
            .also { Audit.FuturePreschoolers.log(meta = mapOf("count" to it.size)) }
    }
}

const val preschoolSelectionAge = 5

fun Database.Read.getFuturePreschoolerRows(today: LocalDate): List<FuturePreschoolersReportRow> =
    createQuery {
            sql(
                """
SELECT p.id, 
    p.last_name AS child_last_name,
    p.first_name AS child_first_name,
    p.street_address AS child_address,
    p.postal_code AS child_postal_code,
    upper(p.post_office) AS child_post_office,
    d.id AS unit_id,
    d.name AS unit_name,
    array_remove(ARRAY[
        CASE WHEN sn.shift_care = 'FULL'::shift_care_type OR sn.shift_care = 'INTERMITTENT'::shift_care_type THEN 'SHIFT_CARE' END,
        NULL, -- language emphasis cannot be obtained from current evaka dataset
        CASE WHEN sno.name_fi like 'Kaksivuotinen%' THEN 'TWO_YEAR_PRESCHOOL' END
    ], NULL) AS options
FROM person p
JOIN placement pl ON pl.child_id = p.id AND pl.start_date < :today AND pl.end_date >= :today
JOIN daycare d ON d.id = pl.unit_id
LEFT JOIN service_need sn ON sn.placement_id = p.id and sn.start_date < :today AND sn.end_date >= :today
LEFT JOIN service_need_option sno on sn.option_id = sno.id
WHERE CASE WHEN sno.name_fi like 'Kaksivuotinen%' THEN
    extract(year from :today) -  extract(year from p.date_of_birth) = $preschoolSelectionAge - 1
ELSE
    extract(year from :today) -  extract(year from p.date_of_birth) = $preschoolSelectionAge
END
            """
                    .trimIndent()
            )
        }
        .bind("today", today)
        .toList<FuturePreschoolersReportRow>()

fun Database.Read.getPreschoolUnitsRows(
    today: LocalDate,
): List<PreschoolUnitsReportRow> =
    createQuery {
            sql(
                """
SELECT d.id, 
    d.name AS unit_name,
    d.street_address AS address,
    d.postal_code AS postal_code,
    d.post_office as post_office,
    (SELECT count(id) FROM daycare_caretaker WHERE group_id = dg.id AND start_date < :today AND end_date >= :today) * 7 AS group_size,
    array_remove(ARRAY[
        CASE WHEN d.provider_type != 'MUNICIPAL' THEN 'PRIVATE' END,
        CASE WHEN d.with_school THEN 'WITH_SCHOOL' END,
        CASE WHEN d.provides_shift_care THEN 'SHIFT_CARE' END,
        CASE WHEN d.language_emphasis_id IS NOT NULL THEN 'LANGUAGE_EMPHASIS' END
    ], NULL) AS options
FROM daycare d
JOIN daycare_group dg on d.id = dg.daycare_id
WHERE d.type && '{PRESCHOOL}'::care_types[] AND
dg.start_date <= :today AND (dg.end_date IS NULL OR dg.end_date >= :today)
            """
                    .trimIndent()
            )
        }
        .bind("today", today)
        .toList<PreschoolUnitsReportRow>()

fun Database.Read.geSourceUnitsRows(today: LocalDate): List<SourceUnitsReportRow> =
    createQuery {
            sql(
                """
SELECT d.id,
    d.name AS unit_name,
    d.street_address AS address,
    d.postal_code AS postal_code,
    d.post_office as post_office
FROM daycare d
WHERE d.closing_date IS NULL OR d.closing_date >= :today
            """
                    .trimIndent()
            )
        }
        .bind("today", today)
        .toList<SourceUnitsReportRow>()

data class FuturePreschoolersReportRow(
    val id: ChildId,
    val childLastName: String,
    val childFirstName: String,
    val childAddress: String,
    val childPostalCode: String,
    val childPostOffice: String,
    val unitId: DaycareId,
    val unitName: String,
    val options: List<String>
)

data class PreschoolUnitsReportRow(
    val id: DaycareId,
    val unitName: String,
    val address: String,
    val postalCode: String,
    val postOffice: String,
    val groupSize: Int,
    val options: List<String>
)

data class SourceUnitsReportRow(
    val id: DaycareId,
    val unitName: String,
    val address: String,
    val postalCode: String,
    val postOffice: String
)
