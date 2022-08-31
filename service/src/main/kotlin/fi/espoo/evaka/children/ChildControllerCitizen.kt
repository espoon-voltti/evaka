// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/children")
class ChildControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping
    fun getChildren(db: Database, user: AuthenticatedUser.Citizen, clock: EvakaClock): List<Child> {
        Audit.CitizenChildrenRead.log()
        accessControl.requirePermissionFor(user, Action.Citizen.Person.READ_CHILDREN, user.id)
        return db.connect { dbc -> dbc.read { it.getChildrenByGuardian(user.id, clock.today()) } }
    }
}
