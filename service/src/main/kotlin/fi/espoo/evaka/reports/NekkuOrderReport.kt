import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.nekku.NekkuOrdersReport
import fi.espoo.evaka.nekku.getNekkuOrderReport
import fi.espoo.evaka.reports.REPORT_STATEMENT_TIMEOUT
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
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
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam groupIds: List<GroupId>?,
    ): List<NekkuOrdersReport> {

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
                    getNekkuOrderReportRows(tx, date, unitId, groupIds?.ifEmpty { null })
                }
            }
            .also {
                Audit.NekkuOrdersReportRead.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("groupIds" to groupIds, "date" to date, "count" to it.size),
                )
            }
    }

    fun getNekkuOrderReportRows(
        tx: Database.Read,
        date: LocalDate,
        unitId: DaycareId,
        groupIds: List<GroupId>?,
    ): List<NekkuOrdersReport> {

        var nekkuOrdersReport: List<NekkuOrdersReport> = emptyList()

        //TODO: at least one group should be added
        if (groupIds != null) {
            var orderRows = tx.getNekkuOrderReport(daycareId = unitId, groupIds.first(), date)
            nekkuOrdersReport = orderRows
        }

        return nekkuOrdersReport
    }
}
