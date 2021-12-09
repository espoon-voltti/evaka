// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class ChildrenResponse(val children: List<Child>)

@RestController
@RequestMapping("/citizen/children")
class ChildControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping
    fun getChildren(db: Database.Connection, user: AuthenticatedUser.Citizen): ChildrenResponse {
        Audit.CitizenChildrenRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_OWN_CHILDREN)
        return ChildrenResponse(db.read { it.getChildrenByGuardian(PersonId(user.id)) })
    }
    @GetMapping("/{id}")
    fun getChild(db: Database.Connection, user: AuthenticatedUser.Citizen, @PathVariable id: UUID): Child {
        Audit.CitizenChildRead.log(id)
        accessControl.requirePermissionFor(user, Action.Child.READ, id)
        return db.read { it.getChild(PersonId(id)) } ?: throw NotFound()
    }
}
