// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildRecipientsController(private val accessControl: AccessControl) {

    @GetMapping("/child/{childId}/recipients")
    fun getRecipients(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<Recipient> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.READ_CHILD_RECIPIENTS,
                        childId
                    )
                    it.fetchRecipients(childId)
                }
            }
            .also {
                Audit.MessagingBlocklistRead.log(
                    targetId = AuditId(childId),
                    meta = mapOf("count" to it.size)
                )
            }
    }

    data class EditRecipientRequest(val blocklisted: Boolean)

    @PutMapping("/child/{childId}/recipients/{personId}")
    fun editRecipient(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @PathVariable personId: PersonId,
        @RequestBody body: EditRecipientRequest
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Child.UPDATE_CHILD_RECIPIENT,
                    childId
                )
                if (body.blocklisted) {
                    tx.addToBlocklist(childId, personId)
                } else {
                    tx.removeFromBlocklist(childId, personId)
                }
            }
        }
        Audit.MessagingBlocklistEdit.log(targetId = AuditId(childId), objectId = AuditId(personId))
    }
}
