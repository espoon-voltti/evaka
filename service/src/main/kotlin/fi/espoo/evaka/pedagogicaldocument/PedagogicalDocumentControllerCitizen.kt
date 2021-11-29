// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.AttachmentId
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
        db: Database.Connection,
        user: AuthenticatedUser
    ): List<PedagogicalDocumentCitizen> {
        Audit.PedagogicalDocumentReadByGuardian.log(user.id)
        return db.read { it.findPedagogicalDocumentsByGuardian(PersonId(user.id), user.type) }
    }

    @PostMapping("/{documentId}/mark-read")
    fun markPedagogicalDocumentRead(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable documentId: PedagogicalDocumentId
    ) {
        Audit.PedagogicalDocumentUpdate.log(documentId, user.id)
        accessControl.requirePermissionFor(user, Action.PedagogicalDocument.READ, documentId)
        db.transaction { it.markDocumentReadByGuardian(documentId, PersonId(user.id)) }
    }

    @GetMapping("/unread-count")
    fun getUnreadPedagogicalDocumentCount(
        db: Database.Connection,
        user: AuthenticatedUser
    ): Number {
        Audit.PedagogicalDocumentCountUnread.log(user.id)
        return db.transaction { it.countUnreadDocumentsByGuardian(PersonId(user.id)) }
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
                a.id attachment_id,
                a.name attachment_name,
                a.content_type attachment_content_type,
                pdr.read_at is not null is_read,
                chp.first_name,
                chp.preferred_name
            FROM pedagogical_document pd
            JOIN guardian g ON pd.child_id = g.child_id
            LEFT JOIN attachment a ON a.pedagogical_document_id = pd.id
            LEFT JOIN pedagogical_document_read pdr ON pd.id = pdr.pedagogical_document_id AND pdr.person_id = g.guardian_id
            LEFT JOIN person chp ON chp.id = pd.child_id
            WHERE g.guardian_id = :guardian_id
            AND (LENGTH(pd.description) > 0 OR a.id IS NOT NULL)
            ORDER BY pd.created DESC
        """.trimIndent()
    )
        .bind("guardian_id", guardianId)
        .map { row -> mapPedagogicalDocumentCitizen(row, userType) }
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

fun Database.Read.citizenHasPermissionForPedagogicalDocument(user: AuthenticatedUser, documentId: PedagogicalDocumentId): Boolean =
    this.createQuery(
        """
            SELECT EXISTS (
                SELECT 1 FROM pedagogical_document pd
                JOIN guardian g ON pd.child_id = g.child_id
                WHERE pd.id = :documentId
                AND g.guardian_id = :userId
            )
        """.trimIndent()
    )
        .bind("documentId", documentId)
        .bind("userId", user.id)
        .mapTo<Boolean>()
        .first()

data class PedagogicalDocumentCitizen(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String,
    val attachment: Attachment?,
    val created: HelsinkiDateTime,
    val isRead: Boolean,
    val childFirstName: String,
    val childPreferredName: String?
)

fun mapPedagogicalDocumentCitizen(row: RowView, userType: AuthenticatedUserType): PedagogicalDocumentCitizen? {
    val id: PedagogicalDocumentId = row.mapColumn("id") ?: return null
    val hasAttachment: Boolean = row.mapColumn<AttachmentId?>("attachment_id") != null

    return PedagogicalDocumentCitizen(
        id = id,
        childId = row.mapColumn("child_id"),
        description = row.mapColumn("description"),
        attachment = if (hasAttachment) Attachment(
            id = row.mapColumn("attachment_id"),
            name = if (userType === AuthenticatedUserType.citizen_weak) "" else row.mapColumn("attachment_name"),
            contentType = row.mapColumn("attachment_content_type"),
        ) else null,
        created = row.mapColumn("created"),
        isRead = row.mapColumn("is_read"),
        childFirstName = row.mapColumn("first_name"),
        childPreferredName = row.mapColumn("preferred_name")

    )
}
