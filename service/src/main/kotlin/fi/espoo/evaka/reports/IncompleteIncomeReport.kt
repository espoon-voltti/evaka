// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IncompleteIncomeReport(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/incomplete-income")
    fun getIncompleteIncomeReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<IncompleteIncomeDbRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_INCOMPLETE_INCOMES_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getIncompleteReport(clock.today())
                }
            }
            .also { Audit.TitaniaReportRead.log() }
    }
}

fun Database.Read.getIncompleteReport(today: LocalDate): List<IncompleteIncomeDbRow> {
    val dbRows =
        createQuery {
                sql(
                    """
                SELECT DISTINCT pe.id as personId, pe.first_name as firstName, pe.last_name as lastName, ie.valid_from as validFrom, pl.unit_id as daycareId, dg.name as daycareName, ca.id as careareaId, ca.name as careareaName
                FROM income ie
                INNER JOIN guardian gu
                        ON ie.person_id = gu.guardian_id
                INNER JOIN placement pl
                        ON gu.child_id = pl.child_id
                INNER JOIN person pe
                        ON gu.guardian_id = pe.id
                INNER JOIN daycare dg
                           ON pl.unit_id = dg.id
                INNER JOIN care_area ca
                           ON dg.care_area_id = ca.id
                WHERE ie.effect = 'INCOMPLETE'
                AND ie.valid_to is null
                AND ie.updated_by = '00000000-0000-0000-0000-000000000000'
                  AND pl.child_id = gu.child_id
                  AND pl.start_date <= ${bind(today)}
                  AND pl.end_date >= ${bind(today)}
                ORDER BY ie.valid_from;
            """
                        .trimIndent()
                )
            }
            .toList<IncompleteIncomeDbRow>()

    return dbRows
}

data class IncompleteIncomeDbRow(
    val personId: PersonId,
    val firstName: String,
    val lastName: String,
    val validFrom: LocalDate,
    val daycareId: String,
    val daycareName: String,
    val careareaId: String,
    val careareaName: String,
)
