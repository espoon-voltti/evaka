// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.AuthenticatedUserType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.result.RowView
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/pedagogical-documents")
class PedagogicalDocumentControllerCitizen(
    private val accessControl: AccessControl
) {
    @GetMapping
    fun getPedagogicalDocumentsByGuardian(
        db: Database,
        user: AuthenticatedUser
    ): List<PedagogicalDocumentCitizen> {
        Audit.PedagogicalDocumentReadByGuardian.log(user.id)
        return db.connect { dbc ->
            dbc.read {
                it.findPedagogicalDocumentsByGuardian(PersonId(user.id), user.type).filter { pd ->
                    pd.description.isNotEmpty() || pd.attachments.isNotEmpty()
                }
            }
        }
    }

    @PostMapping("/{documentId}/mark-read")
    fun markPedagogicalDocumentRead(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable documentId: PedagogicalDocumentId
    ) {
        Audit.PedagogicalDocumentUpdate.log(documentId, user.id)
        accessControl.requirePermissionFor(user, Action.Citizen.PedagogicalDocument.READ, documentId)
        return db.connect { dbc ->
            dbc.transaction { it.markDocumentReadByGuardian(documentId, PersonId(user.id)) }
        }
    }

    @GetMapping("/unread-count")
    fun getUnreadPedagogicalDocumentCount(
        db: Database,
        user: AuthenticatedUser
    ): Number {
        Audit.PedagogicalDocumentCountUnread.log(user.id)
        return db.connect { dbc ->
            dbc.transaction { it.countUnreadDocumentsByGuardian(PersonId(user.id)) }
        }
    }
}

private fun Database.Read.findPedagogicalDocumentsByGuardian(
    guardianId: PersonId,
    userType: AuthenticatedUserType
): List<PedagogicalDocumentCitizen> {
    return this.createQuery(
        """
            SELECT 
                pd.id,
                pd.child_id,
                pd.description,
                pd.created,
                pd.updated,
                pdr.read_at is not null is_read,
                chp.first_name,
                chp.preferred_name
            FROM pedagogical_document pd
            JOIN guardian g ON pd.child_id = g.child_id
            LEFT JOIN pedagogical_document_read pdr ON pd.id = pdr.pedagogical_document_id AND pdr.person_id = g.guardian_id
            LEFT JOIN person chp ON chp.id = pd.child_id
            WHERE g.guardian_id = :guardian_id
            ORDER BY pd.created DESC
        """.trimIndent()
    )
        .bind("guardian_id", guardianId)
        .map { row -> mapPedagogicalDocumentCitizen(row, userType, getPedagogicalDocumentAttachments(row.mapColumn("id"))) }
        .filterNotNull()
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

private fun Database.Read.countUnreadDocumentsByGuardian(personId: PersonId): Int =
    this.createQuery(
        """
            WITH ready_documents AS (
                SELECT pd.id, pd.child_id
                FROM pedagogical_document pd
                LEFT JOIN attachment a ON a.pedagogical_document_id = pd.id
                WHERE LENGTH(pd.description) > 0 OR a.id IS NOT NULL
            )
            SELECT count(*)
            FROM ready_documents d
            JOIN guardian g ON d.child_id = g.child_id AND g.guardian_id = :personId
            WHERE NOT EXISTS (
                SELECT 1 FROM pedagogical_document_read pdr
                WHERE pdr.pedagogical_document_id = d.id AND pdr.person_id = :personId
            )
        """.trimIndent()
    )
        .bind("personId", personId)
        .mapTo<Int>()
        .first()

data class PedagogicalDocumentCitizen(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String,
    val attachments: List<Attachment> = emptyList(),
    val created: HelsinkiDateTime,
    val isRead: Boolean,
    val childFirstName: String,
    val childPreferredName: String?
)

fun mapPedagogicalDocumentCitizen(row: RowView, userType: AuthenticatedUserType, attachments: List<Attachment>): PedagogicalDocumentCitizen? {
    val id: PedagogicalDocumentId = row.mapColumn("id") ?: return null

    return PedagogicalDocumentCitizen(
        id = id,
        childId = row.mapColumn("child_id"),
        description = row.mapColumn("description"),
        attachments = attachments.map { if (userType === AuthenticatedUserType.citizen_weak) it.copy(name = "") else it },
        created = row.mapColumn("created"),
        isRead = row.mapColumn("is_read"),
        childFirstName = row.mapColumn("first_name"),
        childPreferredName = row.mapColumn("preferred_name")

    )
}
