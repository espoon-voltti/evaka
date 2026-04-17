// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.outofoffice

import evaka.core.Audit
import evaka.core.shared.OutOfOfficeId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
            .also { Audit.OutOfOfficeRead.log(targetId = evaka.core.AuditId(user.id)) }
    }

    @PostMapping("/employee/out-of-office")
    fun upsertOutOfOfficePeriod(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: OutOfOfficePeriodUpsert,
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
                    if (body.period.end.isBefore(clock.today())) {
                        throw BadRequest("Cannot create out-of-office period in the past")
                    }
                    if (body.period.start.isAfter(body.period.end)) {
                        throw BadRequest("Start date must be before end date")
                    }
                    it.upsertOutOfOfficePeriod(employeeId = user.id, period = body)
                }
            }
            .also { Audit.OutOfOfficeUpdate.log(targetId = evaka.core.AuditId(user.id)) }
    }

    @DeleteMapping("/employee/out-of-office/{id}")
    fun deleteOutOfOfficePeriod(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: OutOfOfficeId,
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
                    it.deleteOutOfOfficePeriod(id = id)
                }
            }
            .also { Audit.OutOfOfficeDelete.log(targetId = evaka.core.AuditId(user.id)) }
    }
}
