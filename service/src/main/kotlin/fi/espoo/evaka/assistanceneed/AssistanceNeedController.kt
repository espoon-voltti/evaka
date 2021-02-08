// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

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
class AssistanceNeedController(
    private val assistanceNeedService: AssistanceNeedService,
    private val acl: AccessControlList
) {
    @PostMapping("/children/{childId}/assistance-needs")
    fun createAssistanceNeed(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: AssistanceNeedRequest
    ): ResponseEntity<AssistanceNeed> {
        Audit.ChildAssistanceNeedCreate.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return assistanceNeedService.createAssistanceNeed(
            db,
            user = user,
            childId = childId,
            data = body
        ).let { created(it, URI.create("/children/$childId/assistance-needs/${it.id}")) }
    }

    @GetMapping("/children/{childId}/assistance-needs")
    fun getAssistanceNeeds(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<List<AssistanceNeed>> {
        Audit.ChildAssistanceNeedRead.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return assistanceNeedService.getAssistanceNeedsByChildId(db, childId).let(::ok)
    }

    @PutMapping("/assistance-needs/{id}")
    fun updateAssistanceNeed(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") assistanceNeedId: UUID,
        @RequestBody body: AssistanceNeedRequest
    ): ResponseEntity<AssistanceNeed> {
        Audit.ChildAssistanceNeedUpdate.log(targetId = assistanceNeedId)
        acl.getRolesForAssistanceNeed(user, assistanceNeedId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return assistanceNeedService.updateAssistanceNeed(
            db,
            user = user,
            id = assistanceNeedId,
            data = body
        ).let(::ok)
    }

    @DeleteMapping("/assistance-needs/{id}")
    fun deleteAssistanceNeed(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") assistanceNeedId: UUID
    ): ResponseEntity<Unit> {
        Audit.ChildAssistanceNeedDelete.log(targetId = assistanceNeedId)
        acl.getRolesForAssistanceNeed(user, assistanceNeedId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        assistanceNeedService.deleteAssistanceNeed(db, assistanceNeedId)
        return noContent()
    }
}
