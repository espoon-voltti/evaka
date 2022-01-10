// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
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
        @PathVariable childId: ChildId
    ): List<Recipient> {
        Audit.MessagingBlocklistRead.log(childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_CHILD_RECIPIENTS, childId)

        return db.connect { dbc -> dbc.read { it.fetchRecipients(childId) } }
    }

    data class EditRecipientRequest(
        val blocklisted: Boolean
    )
    @PutMapping("/child/{childId}/recipients/{personId}")
    fun editRecipient(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @PathVariable personId: PersonId,
        @RequestBody body: EditRecipientRequest
    ) {
        Audit.MessagingBlocklistEdit.log(childId)
        accessControl.requirePermissionFor(user, Action.Child.UPDATE_CHILD_RECIPIENT, childId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                if (body.blocklisted) {
                    tx.addToBlocklist(childId, personId)
                } else {
                    tx.removeFromBlocklist(childId, personId)
                }
            }
        }
    }
}
