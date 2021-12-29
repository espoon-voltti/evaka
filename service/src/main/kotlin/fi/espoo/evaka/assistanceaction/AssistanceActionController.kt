// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.created
import fi.espoo.evaka.daycare.controllers.utils.noContent
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
    private val accessControl: AccessControl
) {
    @PostMapping("/children/{childId}/assistance-actions")
    fun createAssistanceAction(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: AssistanceActionRequest
    ): ResponseEntity<AssistanceAction> {
        Audit.ChildAssistanceActionCreate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_ASSISTANCE_ACTION, childId)
        return db.connect { dbc ->
            assistanceActionService.createAssistanceAction(
                dbc,
                user = user,
                childId = childId,
                data = body
            )
        }.let { created(it, URI.create("/children/$childId/assistance-actions/${it.id}")) }
    }

    @GetMapping("/children/{childId}/assistance-actions")
    fun getAssistanceActions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): List<AssistanceAction> {
        Audit.ChildAssistanceActionRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_ASSISTANCE_ACTION, childId)
        return db.connect { dbc ->
            assistanceActionService.getAssistanceActionsByChildId(dbc, childId).filter {
                accessControl.hasPermissionFor(user, Action.AssistanceAction.READ_PRE_PRESCHOOL_ASSISTANCE_ACTION, it.id)
            }
        }
    }

    @PutMapping("/assistance-actions/{id}")
    fun updateAssistanceAction(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("id") assistanceActionId: AssistanceActionId,
        @RequestBody body: AssistanceActionRequest
    ): ResponseEntity<AssistanceAction> {
        Audit.ChildAssistanceActionUpdate.log(targetId = assistanceActionId)
        accessControl.requirePermissionFor(user, Action.AssistanceAction.UPDATE, assistanceActionId)
        return db.connect { dbc ->
            assistanceActionService.updateAssistanceAction(
                dbc,
                user = user,
                id = assistanceActionId,
                data = body
            )
        }.let(::ok)
    }

    @DeleteMapping("/assistance-actions/{id}")
    fun deleteAssistanceAction(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("id") assistanceActionId: AssistanceActionId
    ): ResponseEntity<Unit> {
        Audit.ChildAssistanceActionDelete.log(targetId = assistanceActionId)
        accessControl.requirePermissionFor(user, Action.AssistanceAction.DELETE, assistanceActionId)
        db.connect { dbc -> assistanceActionService.deleteAssistanceAction(dbc, assistanceActionId) }
        return noContent()
    }

    @GetMapping("/assistance-action-options")
    fun getAssistanceActionOptions(db: Database, user: AuthenticatedUser): List<AssistanceActionOption> {
        accessControl.requirePermissionFor(user, Action.Global.READ_ASSISTANCE_BASIS_OPTIONS)
        return db.connect { dbc -> assistanceActionService.getAssistanceActionOptions(dbc) }
    }
}
