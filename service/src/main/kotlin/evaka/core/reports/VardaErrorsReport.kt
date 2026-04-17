// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class VardaErrorReport(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/varda-child-errors")
    fun getVardaChildErrorsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<VardaChildErrorReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_VARDA_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getVardaChildErrors()
                }
            }
            .also { Audit.VardaReportRead.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/employee/reports/varda-unit-errors")
    fun getVardaUnitErrorsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<VardaUnitErrorReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_VARDA_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getVardaUnitErrors()
                }
            }
            .also { Audit.VardaUnitReportRead.log(meta = mapOf("count" to it.size)) }
    }
}

private fun Database.Read.getVardaChildErrors(): List<VardaChildErrorReportRow> =
    createQuery {
            sql(
                """
SELECT
    child_id,
    errored_at,
    errored_since,
    error
FROM varda_state
WHERE errored_at IS NOT NULL
ORDER BY errored_at DESC
    """
            )
        }
        .toList<VardaChildErrorReportRow>()

data class VardaChildErrorReportRow(
    val childId: ChildId,
    val erroredAt: HelsinkiDateTime,
    val erroredSince: HelsinkiDateTime,
    val error: String,
)

private fun Database.Read.getVardaUnitErrors(): List<VardaUnitErrorReportRow> =
    createQuery {
            sql(
                """
                SELECT
                    vu.evaka_daycare_id AS unit_id,
                    u.name AS unit_name,
                    vu.errored_since,
                    vu.errored_at,
                    vu.error
                FROM varda_unit vu
                JOIN daycare u ON u.id = vu.evaka_daycare_id
                WHERE vu.errored_at IS NOT NULL
                """
            )
        }
        .toList<VardaUnitErrorReportRow>()

data class VardaUnitErrorReportRow(
    val unitId: DaycareId,
    val unitName: String,
    val erroredSince: HelsinkiDateTime,
    val erroredAt: HelsinkiDateTime,
    val error: String,
)
