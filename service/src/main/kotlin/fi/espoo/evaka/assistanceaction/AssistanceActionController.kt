// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.created
import fi.espoo.evaka.daycare.controllers.utils.noContent
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
class AssistanceActionController(
    private val assistanceActionService: AssistanceActionService,
    private val acl: AccessControlList
) {
    @PostMapping("/children/{childId}/assistance-actions")
    fun createAssistanceAction(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: AssistanceActionRequest
    ): ResponseEntity<AssistanceAction> {
        Audit.ChildAssistanceActionCreate.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return assistanceActionService.createAssistanceAction(
            db,
            user = user,
            childId = childId,
            data = body
        ).let { created(it, URI.create("/children/$childId/assistance-actions/${it.id}")) }
    }

    @GetMapping("/children/{childId}/assistance-actions")
    fun getAssistanceActions(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<List<AssistanceAction>> {
        Audit.ChildAssistanceActionRead.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return assistanceActionService.getAssistanceActionsByChildId(db, childId).let(::ok)
    }

    @PutMapping("/assistance-actions/{id}")
    fun updateAssistanceAction(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") assistanceActionId: UUID,
        @RequestBody body: AssistanceActionRequest
    ): ResponseEntity<AssistanceAction> {
        Audit.ChildAssistanceActionUpdate.log(targetId = assistanceActionId)
        acl.getRolesForAssistanceAction(user, assistanceActionId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return assistanceActionService.updateAssistanceAction(
            db,
            user = user,
            id = assistanceActionId,
            data = body
        ).let(::ok)
    }

    @DeleteMapping("/assistance-actions/{id}")
    fun deleteAssistanceAction(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") assistanceActionId: UUID
    ): ResponseEntity<Unit> {
        Audit.ChildAssistanceActionDelete.log(targetId = assistanceActionId)
        acl.getRolesForAssistanceAction(user, assistanceActionId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        assistanceActionService.deleteAssistanceAction(db, assistanceActionId)
        return noContent()
    }
}
