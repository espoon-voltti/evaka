// SPDX-FileCopyrightText: 2017-2026 City of Espoo
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
class KoskiErrorReport(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/koski-errors")
    fun getKoskiErrorsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<KoskiErrorReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_KOSKI_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getKoskiErrors()
                }
            }
            .also { Audit.KoskiReportRead.log(meta = mapOf("count" to it.size)) }
    }
}

private fun Database.Read.getKoskiErrors(): List<KoskiErrorReportRow> = createQuery {
    sql(
        """
SELECT e.child_id, e.unit_id, d.name AS unit_name, e.type, e.error, e.errored_at, e.errored_since
FROM koski_upload_error e
JOIN daycare d ON d.id = e.unit_id
ORDER BY e.errored_at DESC
    """
    )
}
    .toList<KoskiErrorReportRow>()

// evaka.core.koski.OpiskeluoikeudenTyyppiKoodi can't be exposed in the API, because it serializes
// to the Koski wire format codes that the generated frontend types don't know about
enum class KoskiStudyRightType {
    PRESCHOOL,
    PREPARATORY,
}

data class KoskiErrorReportRow(
    val childId: ChildId,
    val unitId: DaycareId,
    val unitName: String,
    val type: KoskiStudyRightType,
    val error: String,
    val erroredAt: HelsinkiDateTime,
    val erroredSince: HelsinkiDateTime,
)
