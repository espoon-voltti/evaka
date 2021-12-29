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
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.pis.service.hideNonPermittedPersonData
import fi.espoo.evaka.pis.updatePreferredName
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.mapper.Nested
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ChildController(private val accessControl: AccessControl) {
    @GetMapping("/children/{childId}")
    fun getChild(db: Database, user: AuthenticatedUser, @PathVariable childId: UUID): ChildResponse {
        Audit.PersonDetailsRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ, childId)
        val child = db.connect { dbc -> dbc.read { it.getPersonById(childId) } }
            ?.hideNonPermittedPersonData(
                includeInvoiceAddress = accessControl.hasPermissionFor(
                    user,
                    Action.Person.READ_INVOICE_ADDRESS,
                    PersonId(childId)
                ),
                includeOphOid = accessControl.hasPermissionFor(user, Action.Person.READ_OPH_OID, PersonId(childId))
            )
            ?: throw NotFound("Child $childId not found")
        return ChildResponse(
            person = PersonJSON.from(child),
            permittedActions = accessControl.getPermittedChildActions(user, listOf(ChildId(childId))).values.first(),
            permittedPersonActions = accessControl.getPermittedPersonActions(user, listOf(PersonId(childId)))
                .values.first()
        )
    }

    @GetMapping("/children/{childId}/additional-information")
    fun getAdditionalInfo(db: Database, user: AuthenticatedUser, @PathVariable childId: UUID): ResponseEntity<AdditionalInformation> {
        Audit.ChildAdditionalInformationRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_ADDITIONAL_INFO, childId)
        return db.connect { dbc -> dbc.read { it.getAdditionalInformation(childId) } }.let(::ok)
    }

    @PutMapping("/children/{childId}/additional-information")
    fun updateAdditionalInfo(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody data: AdditionalInformation
    ): ResponseEntity<Unit> {
        Audit.ChildAdditionalInformationUpdate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.UPDATE_ADDITIONAL_INFO, childId)
        db.connect { dbc -> dbc.transaction { it.upsertAdditionalInformation(childId, data) } }
        return noContent()
    }

    data class ChildResponse(
        val person: PersonJSON,
        val permittedActions: Set<Action.Child>,
        val permittedPersonActions: Set<Action.Person>
    )
}

fun Database.Read.getAdditionalInformation(childId: UUID): AdditionalInformation {
    val child = getChild(childId)
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

fun Database.Transaction.upsertAdditionalInformation(childId: UUID, data: AdditionalInformation) {
    updatePreferredName(childId, data.preferredName)
    val child = getChild(childId)
    if (child != null) {
        updateChild(child.copy(additionalInformation = data))
    } else {
        createChild(
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
