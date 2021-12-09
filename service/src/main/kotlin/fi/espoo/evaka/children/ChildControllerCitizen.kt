// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class ChildrenResponse(val children: List<Child>)

@RestController
class ChildControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping("/citizen/children")
    fun getChildren(db: Database.Connection, user: AuthenticatedUser.Citizen): ChildrenResponse {
        Audit.CitizenChildrenRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_OWN_CHILDREN)
        return ChildrenResponse(db.read { it.getChildrenByGuardian(PersonId(user.id)) })
    }
}
