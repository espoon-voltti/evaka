// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.mobile

import fi.espoo.evaka.Audit
import fi.espoo.evaka.attendance.ChildSensitiveInformation
import fi.espoo.evaka.attendance.getChildSensitiveInfo
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildSensitiveInfoController(
    private val ac: AccessControl
) {

    @GetMapping("/children/{childId}/sensitive-info")
    fun getSensitiveInfo(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
    ): ChildSensitiveInformation {
        Audit.ChildSensitiveInfoRead.log(targetId = childId)

        ac.requirePermissionFor(user, Action.Child.READ_SENSITIVE_INFO, childId.raw)

        return db.read { it.getChildSensitiveInfo(childId.raw) ?: throw NotFound("Child not found") }
    }
}
