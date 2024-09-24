// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class NonSsnChildrenReportController(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/non-ssn-children")
    fun getNonSsnChildrenReportRows(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<NonSsnChildrenReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_NON_SSN_CHILDREN_REPORT,
                    )

                    it.getNonSsnChildren(clock.today())
                }
            }
            .also { Audit.NonSsnChildrenReport.log(meta = mapOf("count" to it.size)) }
    }
}

data class NonSsnChildrenReportRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val ophPersonOid: String?,
    val lastSentToVarda: HelsinkiDateTime?,
)

private fun Database.Read.getNonSsnChildren(
    examinationDate: LocalDate
): List<NonSsnChildrenReportRow> =
    createQuery {
            sql(
                """
SELECT
    p.first_name,
    p.last_name,
    p.id AS child_id,
    p.date_of_birth,
    p.oph_person_oid,
    v.last_success_at AS last_sent_to_varda
FROM placement pl
JOIN person p ON p.id = pl.child_id
LEFT JOIN varda_state v ON v.child_id = pl.child_id
WHERE
    p.social_security_number IS NULL AND
    pl.end_date >= ${bind(examinationDate)}
"""
            )
        }
        .toList<NonSsnChildrenReportRow>()
