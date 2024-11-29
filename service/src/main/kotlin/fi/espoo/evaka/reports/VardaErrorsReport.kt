// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
    NULL AS service_need_id,
    NULL AS service_need_validity,
    NULL AS service_need_option_name,
    child_id,
    errored_at AS updated,
    coalesce(last_success_at, created_at) AS created,
    ARRAY[error] AS errors,
    NULL AS reset_timestamp
FROM varda_state
WHERE errored_at IS NOT NULL
ORDER BY updated DESC
    """
            )
        }
        .toList<VardaChildErrorReportRow>()

data class VardaChildErrorReportRow(
    val serviceNeedId: ServiceNeedId?,
    val serviceNeedValidity: FiniteDateRange?,
    val serviceNeedOptionName: String?,
    val childId: ChildId,
    val updated: HelsinkiDateTime,
    val created: HelsinkiDateTime,
    val errors: List<String>,
    val resetTimeStamp: HelsinkiDateTime?,
)

private fun Database.Read.getVardaUnitErrors(): List<VardaUnitErrorReportRow> =
    createQuery {
            sql(
                """
                SELECT
                    vu.evaka_daycare_id AS unit_id,
                    u.name AS unit_name,
                    coalesce(vu.last_success_at, vu.created_at) AS created_at,
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
    val createdAt: HelsinkiDateTime,
    val erroredAt: HelsinkiDateTime,
    val error: String,
)
