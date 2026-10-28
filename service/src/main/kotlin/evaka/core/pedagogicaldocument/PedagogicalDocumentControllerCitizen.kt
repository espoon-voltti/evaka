// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pedagogicaldocument

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.ChildId
import evaka.core.shared.PedagogicalDocumentId
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen")
class PedagogicalDocumentControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping("/children/{childId}/pedagogical-documents")
    fun getPedagogicalDocumentsForChild(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): List<PedagogicalDocumentCitizen> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_PEDAGOGICAL_DOCUMENTS,
                        childId,
                    )
                    tx.getChildPedagogicalDocuments(childId, user.id).filter { pd ->
                        pd.description.isNotEmpty() || pd.attachments.isNotEmpty()
                    }
                }
            }
            .also {
                Audit.PedagogicalDocumentReadByGuardian.log(
                    targetId = AuditId(childId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @PostMapping("/pedagogical-documents/{documentId}/mark-read")
    fun markPedagogicalDocumentRead(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable documentId: PedagogicalDocumentId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Citizen.PedagogicalDocument.READ,
                    documentId,
                )
                it.markDocumentReadByGuardian(clock, documentId, user.id)
            }
        }
        Audit.PedagogicalDocumentUpdate.log(targetId = AuditId(documentId))
    }

    @GetMapping("/pedagogical-documents/unread-count")
    fun getUnreadPedagogicalDocumentCount(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): Map<ChildId, Int> {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Person.READ_PEDAGOGICAL_DOCUMENT_UNREAD_COUNTS,
                        user.id,
                    )
                    it.countUnreadDocumentsByUser(clock.today(), user.id)
                }
            }
            .also { Audit.PedagogicalDocumentCountUnread.log(targetId = AuditId(user.id)) }
    }
}

private fun Database.Transaction.markDocumentReadByGuardian(
    clock: EvakaClock,
    documentId: PedagogicalDocumentId,
    guardianId: PersonId,
) = createUpdate {
    sql(
        """
INSERT INTO pedagogical_document_read (pedagogical_document_id, person_id, read_at)
VALUES (${bind(documentId)}, ${bind(guardianId)}, ${bind(clock.now())})
ON CONFLICT (pedagogical_document_id, person_id) DO NOTHING
"""
    )
}
    .execute()
