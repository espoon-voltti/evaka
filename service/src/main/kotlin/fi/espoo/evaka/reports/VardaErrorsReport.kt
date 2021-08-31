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
        db: Database,
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
    evaka_service_need_id AS service_need_id,
    evaka_child_id AS child_id,
    updated,
    errors
FROM varda_service_need
WHERE updated > :errorsSince AND update_failed = true
    """.trimIndent()
).bind("errorsSince", errorsSince)
    .mapTo<VardaErrorReportRow>()
    .toList()

data class VardaErrorReportRow(
    val serviceNeedId: UUID,
    val childId: UUID,
    val updated: HelsinkiDateTime,
    val errors: String
)
