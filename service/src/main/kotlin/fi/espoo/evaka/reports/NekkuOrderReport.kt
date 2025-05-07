import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.nekku.NekkuOrdersReport
import fi.espoo.evaka.nekku.getDaycareGroupIds
import fi.espoo.evaka.nekku.getNekkuOrderReport
import fi.espoo.evaka.reports.REPORT_STATEMENT_TIMEOUT
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class NekkuOrderReportController(private val accessControl: AccessControl) {

    @GetMapping("/employee/reports/nekkuorders/{unitId}")
    fun getNekkuOrderReportByUnit(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @RequestParam groupIds: List<GroupId>?,
    ): List<NekkuOrdersReport> {
        if (start.isAfter(end)) throw BadRequest("Inverted time range")
        if (end.isAfter(start.plusWeeks(2))) throw BadRequest("Too long time range")

        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_NEKKU_ORDER_REPORT,
                        unitId,
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    getNekkuOrderReportRows(tx, start, end, unitId, groupIds?.ifEmpty { null })
                }
            }
            .also {
                Audit.NekkuOrdersReportRead.log(
                    targetId = AuditId(unitId),
                    meta =
                        mapOf(
                            "groupIds" to groupIds,
                            "startDate" to start,
                            "endDate" to end,
                            "count" to it.size,
                        ),
                )
            }
    }

    fun getNekkuOrderReportRows(
        tx: Database.Read,
        start: LocalDate,
        end: LocalDate,
        unitId: DaycareId,
        groupIds: List<GroupId>?,
    ): List<NekkuOrdersReport> {

        val reports = mutableListOf<NekkuOrdersReport>()
        var currentDate = start
        var mutableGroupIds = groupIds?.toMutableList()

        if (mutableGroupIds == null) {
            mutableGroupIds = tx.getDaycareGroupIds(unitId) as MutableList<GroupId>?
        }

        while (!currentDate.isAfter(end)) {
            mutableGroupIds?.forEach { groupId ->
                val report = tx.getNekkuOrderReport(unitId, groupId, currentDate)
                reports.addAll(report)
            }
            currentDate = currentDate.plusDays(1)
        }

        return reports
    }
}
