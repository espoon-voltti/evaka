// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.sensitive

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.ChildId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildInfoController(private val ac: AccessControl) {

    @GetMapping("/employee-mobile/children/{childId}/basic-info")
    fun getBasicInfo(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): ChildBasicInformation {
        return db.connect { dbc ->
                dbc.read {
                    ac.requirePermissionFor(it, user, clock, Action.Child.READ_BASIC_INFO, childId)

                    it.getChildBasicInfo(clock, childId) ?: throw NotFound("Child not found")
                }
            }
            .also { Audit.ChildBasicInfoRead.log(targetId = AuditId(childId)) }
    }

    @GetMapping("/employee-mobile/children/{childId}/sensitive-info")
    fun getSensitiveInfo(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): ChildSensitiveInformation {
        return db.connect { dbc ->
                dbc.read {
                    ac.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.READ_SENSITIVE_INFO,
                        childId,
                    )

                    it.getChildSensitiveInfo(childId) ?: throw NotFound("Child not found")
                }
            }
            .also { Audit.ChildSensitiveInfoRead.log(targetId = AuditId(childId)) }
    }
}
