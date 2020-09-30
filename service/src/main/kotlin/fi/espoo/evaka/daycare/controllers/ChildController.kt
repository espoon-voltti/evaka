// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.noContent
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.daycare.service.AdditionalInformation
import fi.espoo.evaka.daycare.service.ChildService
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.config.Roles.FINANCE_ADMIN
import fi.espoo.evaka.shared.config.Roles.SERVICE_WORKER
import fi.espoo.evaka.shared.config.Roles.UNIT_SUPERVISOR
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ChildController(
    private val childService: ChildService,
    private val acl: AccessControlList
) {
    @GetMapping("/children/{childId}/additional-information")
    fun getAdditionalInfo(user: AuthenticatedUser, @PathVariable childId: UUID): ResponseEntity<AdditionalInformation> {
        Audit.ChildAdditionalInformationRead.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF)
        return childService.getAdditionalInformation(childId).let(::ok)
    }

    @PutMapping("/children/{childId}/additional-information")
    fun updateAdditionalInfo(
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody data: AdditionalInformation
    ): ResponseEntity<Unit> {
        Audit.ChildAdditionalInformationUpdate.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)
        childService.upsertAdditionalInformation(childId, data)
        return noContent()
    }
}
