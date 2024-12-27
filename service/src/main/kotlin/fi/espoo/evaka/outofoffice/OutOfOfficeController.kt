package fi.espoo.evaka.outofoffice

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class OutOfOfficeController(private val accessControl: AccessControl) {

    @GetMapping("/employee/out-of-office")
    fun getOutOfOfficePeriods(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<OutOfOfficePeriod> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Employee.READ_OUT_OF_OFFICE,
                        user.id,
                    )
                    it.getOutOfOfficePeriods(employeeId = user.id, today = clock.today())
                }
            }
            .also { Audit.OutOfOfficeRead.log(targetId = fi.espoo.evaka.AuditId(user.id)) }
    }
}
