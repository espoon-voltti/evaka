// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.created
import fi.espoo.evaka.daycare.controllers.utils.noContent
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.config.Roles.FINANCE_ADMIN
import fi.espoo.evaka.shared.config.Roles.SERVICE_WORKER
import fi.espoo.evaka.shared.config.Roles.UNIT_SUPERVISOR
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
class ServiceNeedController(
    private val serviceNeedService: ServiceNeedService,
    private val acl: AccessControlList
) {
    @PostMapping("/children/{childId}/service-needs")
    fun createServiceNeed(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: ServiceNeedRequest
    ): ResponseEntity<ServiceNeed> {
        Audit.ChildServiceNeedCreate.log(targetId = childId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)
        return serviceNeedService.createServiceNeed(
            db,
            user = user,
            childId = childId,
            data = body
        ).let { created(it, URI.create("/children/$childId/service-needs/${it.id}")) }
    }

    @GetMapping("/children/{childId}/service-needs")
    fun getServiceNeeds(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<List<ServiceNeed>> {
        Audit.ChildServiceNeedRead.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF)
        return serviceNeedService.getServiceNeedsByChildId(db, childId).let(::ok)
    }

    @PutMapping("/service-needs/{id}")
    fun updateServiceNeed(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: ServiceNeedRequest
    ): ResponseEntity<ServiceNeed> {
        Audit.ChildServiceNeedUpdate.log(targetId = id)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)
        return serviceNeedService.updateServiceNeed(
            db,
            user = user,
            id = id,
            data = body
        ).let(::ok)
    }

    @DeleteMapping("/service-needs/{id}")
    fun deleteServiceNeed(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        Audit.ChildServiceNeedDelete.log(targetId = id)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)
        serviceNeedService.deleteServiceNeed(db, id)
        return noContent()
    }
}
