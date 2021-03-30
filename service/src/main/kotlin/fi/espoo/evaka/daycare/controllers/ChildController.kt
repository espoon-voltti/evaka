// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.noContent
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.daycare.updateChild
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.Nested
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ChildController(private val acl: AccessControlList) {
    @GetMapping("/children/{childId}/additional-information")
    fun getAdditionalInfo(db: Database.Connection, user: AuthenticatedUser, @PathVariable childId: UUID): ResponseEntity<AdditionalInformation> {
        Audit.ChildAdditionalInformationRead.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
        return db.read { getAdditionalInformation(it.handle, childId) }.let(::ok)
    }

    @PutMapping("/children/{childId}/additional-information")
    fun updateAdditionalInfo(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody data: AdditionalInformation
    ): ResponseEntity<Unit> {
        Audit.ChildAdditionalInformationUpdate.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)
        db.transaction { upsertAdditionalInformation(it.handle, childId, data) }
        return noContent()
    }
}

fun getAdditionalInformation(h: Handle, childId: UUID): AdditionalInformation {
    val child = h.getChild(childId)
    return if (child != null) {
        AdditionalInformation(
            allergies = child.additionalInformation.allergies,
            diet = child.additionalInformation.diet,
            additionalInfo = child.additionalInformation.additionalInfo,
            preferredName = child.additionalInformation.preferredName,
            medication = child.additionalInformation.medication
        )
    } else AdditionalInformation()
}

fun upsertAdditionalInformation(h: Handle, childId: UUID, data: AdditionalInformation) {
    val child = h.getChild(childId)
    if (child != null) {
        h.updateChild(child.copy(additionalInformation = data))
    } else {
        h.createChild(
            Child(
                id = childId,
                additionalInformation = data
            )
        )
    }
}

data class Child(
    val id: UUID,
    @Nested val additionalInformation: AdditionalInformation
)

data class AdditionalInformation(
    val allergies: String = "",
    val diet: String = "",
    val additionalInfo: String = "",
    val preferredName: String = "",
    val medication: String = ""
)
