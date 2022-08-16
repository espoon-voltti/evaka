// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen")
class PedagogicalDocumentControllerCitizen(
    private val accessControl: AccessControl
) {
    @GetMapping("/children/{childId}/pedagogical-documents")
    fun getPedagogicalDocumentsForChild(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable childId: ChildId
    ): List<PedagogicalDocumentCitizen> {
        Audit.PedagogicalDocumentReadByGuardian.log(targetId = childId)

        return db.connect { dbc ->
            dbc.read {
                val documents = it.getPedagogicalDocumentsByChildForGuardian(childId, user.id).filter { pd ->
                    pd.description.isNotEmpty() || pd.attachments.isNotEmpty()
                }

                if (user.authLevel == CitizenAuthLevel.STRONG)
                    documents
                else
                    documents.map { it.copy(attachments = it.attachments.map { it.copy(name = "") }) }
            }
        }
    }

    @PostMapping("/pedagogical-documents/{documentId}/mark-read")
    fun markPedagogicalDocumentRead(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable documentId: PedagogicalDocumentId
    ) {
        Audit.PedagogicalDocumentUpdate.log(documentId, user.id)
        accessControl.requirePermissionFor(user, Action.Citizen.PedagogicalDocument.READ, documentId)
        return db.connect { dbc ->
            dbc.transaction { it.markDocumentReadByGuardian(documentId, user.id) }
        }
    }

    @GetMapping("/pedagogical-documents/unread-count")
    fun getUnreadPedagogicalDocumentCount(
        db: Database,
        user: AuthenticatedUser.Citizen
    ): Map<ChildId, Int> {
        Audit.PedagogicalDocumentCountUnread.log(user.id)
        return db.connect { dbc ->
            dbc.transaction { it.countUnreadDocumentsByGuardian(user.id) }
        }
    }
}

private fun Database.Transaction.markDocumentReadByGuardian(documentId: PedagogicalDocumentId, guardianId: PersonId) =
    this.createUpdate(
        """
            INSERT INTO pedagogical_document_read (pedagogical_document_id, person_id, read_at)
            VALUES (:documentId, :personId, now())
            ON CONFLICT(pedagogical_document_id, person_id) DO NOTHING;
        """.trimIndent()
    )
        .bind("documentId", documentId)
        .bind("personId", guardianId)
        .execute()

private fun Database.Read.countUnreadDocumentsByGuardian(personId: PersonId): Map<ChildId, Int> =
    this.createQuery(
        """
            WITH ready_documents AS (
                SELECT pd.id, pd.child_id
                FROM pedagogical_document pd
                LEFT JOIN attachment a ON a.pedagogical_document_id = pd.id
                WHERE LENGTH(pd.description) > 0 OR a.id IS NOT NULL
            )
            SELECT d.child_id, count(*) as unread_count
            FROM ready_documents d
            JOIN guardian g ON d.child_id = g.child_id AND g.guardian_id = :personId
            WHERE NOT EXISTS (
                SELECT 1 FROM pedagogical_document_read pdr
                WHERE pdr.pedagogical_document_id = d.id AND pdr.person_id = :personId
            )
            GROUP BY d.child_id
        """.trimIndent()
    )
        .bind("personId", personId)
        .map { row -> row.mapColumn<ChildId>("child_id") to row.mapColumn<Int>("unread_count") }
        .toMap()
