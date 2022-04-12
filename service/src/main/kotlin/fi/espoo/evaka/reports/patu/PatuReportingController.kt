package fi.espoo.evaka.reports.patu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/patu-reporting")
class PatuReportingController(private val asyncJobRunner: AsyncJobRunner<AsyncJob>, private val accessControl: AccessControl) {

    @PostMapping
    fun sendPatuReport(db: Database, user: AuthenticatedUser) {
        Audit.PatuReportSend.log()
        accessControl.requirePermissionFor(user, Action.Global.SEND_PATU_REPORT)
    }
}
