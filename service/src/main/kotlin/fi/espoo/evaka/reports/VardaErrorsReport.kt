// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class VardaErrorReport(private val accessControl: AccessControl) {
    @GetMapping("/reports/varda-errors")
    fun getVardaErrors(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<VardaErrorReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_VARDA_REPORT
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getVardaErrors()
                }
            }
            .also { Audit.VardaReportRead.log(meta = mapOf("count" to it.size)) }
    }
}

private fun Database.Read.getVardaErrors(): List<VardaErrorReportRow> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT
    vsn.evaka_service_need_id AS service_need_id,
    sn.start_date as service_need_start_date,
    sn.end_date as service_need_end_date,
    sno.name_fi as service_need_option_name,
    vsn.evaka_child_id AS child_id,
    vsn.updated,
    CASE WHEN vrc.reset_timestamp IS NULL THEN vrc.updated ELSE vsn.created END AS created,
    vsn.errors,
    vrc.reset_timestamp
FROM varda_service_need vsn
JOIN service_need sn on vsn.evaka_service_need_id = sn.id
JOIN service_need_option sno ON sn.option_id = sno.id
LEFT JOIN varda_reset_child vrc ON vrc.evaka_child_id = vsn.evaka_child_id
WHERE vsn.update_failed = true AND vrc.reset_timestamp IS NOT NULL
ORDER BY vsn.updated DESC
    """
                .trimIndent()
        )
        .toList<VardaErrorReportRow>()

data class VardaErrorReportRow(
    val serviceNeedId: ServiceNeedId,
    val serviceNeedStartDate: String,
    val serviceNeedEndDate: String,
    val serviceNeedOptionName: String,
    val childId: ChildId,
    val updated: HelsinkiDateTime,
    val created: HelsinkiDateTime,
    val errors: List<String>,
    val resetTimeStamp: HelsinkiDateTime?
)
