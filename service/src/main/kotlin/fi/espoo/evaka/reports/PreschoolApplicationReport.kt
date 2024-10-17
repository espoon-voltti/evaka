// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PreschoolApplicationReport(private val accessControl: AccessControl) {

    @GetMapping("/employee/reports/preschool-application")
    fun getPreschoolApplicationReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<PreschoolApplicationReportRow> {
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                val filter =
                    accessControl.requireAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_PRESCHOOL_APPLICATION_REPORT,
                    )
                tx.getPreschoolApplicationReportRows(clock.today(), filter)
            }
        }
    }
}

private fun Database.Read.getPreschoolApplicationReportRows(
    today: LocalDate,
    filter: AccessControlFilter<DaycareId>,
): List<PreschoolApplicationReportRow> =
    createQuery {
            sql(
                """
SELECT
    application.id AS application_id,
    application_unit.id AS application_unit_id,
    application_unit.name AS application_unit_name,
    person.id AS child_id,
    person.last_name AS child_last_name,
    person.first_name AS child_first_name,
    person.street_address AS child_street_address,
    person.postal_code AS child_postal_code,
    current_unit.id AS current_unit_id,
    current_unit.name AS current_unit_name,
    person.date_of_birth AS child_date_of_birth,
    EXISTS (SELECT FROM daycare_assistance WHERE child_id = person.id AND valid_during @> ${bind(today)}) AS is_daycare_assistance_need
FROM application
JOIN daycare application_unit ON (application.document -> 'apply' -> 'preferredUnits' ->> 0)::uuid = application_unit.id
JOIN person on application.child_id = person.id
LEFT JOIN placement ON person.id = placement.child_id AND ${bind(today)} BETWEEN placement.start_date AND placement.end_date
LEFT JOIN daycare current_unit ON placement.unit_id = current_unit.id
WHERE application.type = 'PRESCHOOL'
  AND application.status = 'WAITING_UNIT_CONFIRMATION'
  AND ${predicate(filter.forTable("application_unit"))}
    """
                    .trimIndent()
            )
        }
        .toList<PreschoolApplicationReportRow>()

data class PreschoolApplicationReportRow(
    val applicationId: ApplicationId,
    val applicationUnitId: DaycareId,
    val applicationUnitName: String,
    val childId: ChildId,
    val childLastName: String,
    val childFirstName: String,
    val childStreetAddress: String,
    val childPostalCode: String,
    val currentUnitId: DaycareId?,
    val currentUnitName: String?,
    val childDateOfBirth: LocalDate,
    val isDaycareAssistanceNeed: Boolean,
)
