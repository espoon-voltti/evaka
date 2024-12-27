package fi.espoo.evaka.outofoffice

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
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

    @PostMapping("/employee/out-of-office")
    fun upsertOutOfOfficePeriod(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        body: OutOfOfficePeriodUpsert,
    ) {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Employee.UPDATE_OUT_OF_OFFICE,
                        user.id,
                    )
                    if (body.period.start.isBefore(clock.today())) {
                        throw BadRequest("Cannot create out-of-office period in the past")
                    }
                    if (body.period.start.isAfter(body.period.end)) {
                        throw BadRequest("Start date must be before end date")
                    }
                    it.upsertOutOfOfficePeriod(employeeId = user.id, period = body)
                }
            }
            .also { Audit.OutOfOfficeUpdate.log(targetId = fi.espoo.evaka.AuditId(user.id)) }
    }
}
