// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.absence

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.ChildId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class AbsenceControllerEmployeeMobile(private val accessControl: AccessControl) {
    @GetMapping("/employee-mobile/absences/by-child/{childId}/future")
    fun futureAbsencesOfChild(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): List<Absence> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.READ_FUTURE_ABSENCES,
                        childId,
                    )
                    getFutureAbsencesOfChild(it, clock, childId)
                }
            }
            .also { Audit.AbsenceRead.log(targetId = AuditId(childId)) }
    }
}
