// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class VardaErrorReport(private val acl: AccessControlList) {
    @GetMapping("/reports/varda-errors")
    fun getVardaErrors(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam("errorsSince") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) errorsSince: LocalDate
    ): List<VardaErrorReportRow> {
        Audit.VardaReportRead.log()
        user.requireOneOfRoles(UserRole.ADMIN)
        return db.read { it.getVardaErrorsSince(errorsSince) }
    }
}

private fun Database.Read.getVardaErrorsSince(errorsSince: LocalDate): List<VardaErrorReportRow> = createQuery(
    """
SELECT
    vsn.evaka_service_need_id AS service_need_id,
    sn.start_date as service_need_start_date,
    sn.end_date as service_need_end_date,
    sno.name_fi as service_need_option_name,
    vsn.evaka_child_id AS child_id,
    vsn.updated,
    vsn.errors
FROM varda_service_need vsn
JOIN service_need sn on vsn.evaka_service_need_id = sn.id
JOIN service_need_option sno ON sn.option_id = sno.id
WHERE vsn.updated > :errorsSince AND vsn.update_failed = true
    """.trimIndent()
).bind("errorsSince", errorsSince)
    .mapTo<VardaErrorReportRow>()
    .toList()

data class VardaErrorReportRow(
    val serviceNeedId: UUID,
    val serviceNeedStartDate: String,
    val serviceNeedEndDate: String,
    val serviceNeedOptionName: String,
    val childId: UUID,
    val updated: HelsinkiDateTime,
    val errors: List<String>
)
