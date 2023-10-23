// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.notes

import fi.espoo.evaka.application.ApplicationNote
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.db.Database

fun Database.Read.getApplicationNotes(applicationId: ApplicationId): List<ApplicationNote> {
    // language=SQL
    val sql =
        """
SELECT 
    n.id, n.application_id, n.content, 
    n.created, n.created_by, (SELECT name FROM evaka_user WHERE id = n.created_by) AS created_by_name,
    n.updated, n.updated_by, (SELECT name FROM evaka_user WHERE id = n.updated_by) AS updated_by_name,
    n.message_content_id, m.thread_id as message_thread_id
FROM application_note n
LEFT JOIN message m ON m.content_id = n.message_content_id
WHERE application_id = :applicationId
ORDER BY n.created
        """
            .trimIndent()

    return createQuery(sql).bind("applicationId", applicationId).toList<ApplicationNote>()
}

fun Database.Read.getApplicationSpecialEducationTeacherNotes(
    applicationId: ApplicationId
): List<ApplicationNote> {
    // language=SQL
    val sql =
        """
SELECT
    n.id, n.application_id, n.content,
    n.created, n.created_by, (SELECT name FROM evaka_user WHERE id = n.created_by) AS created_by_name,
    n.updated, n.updated_by, (SELECT name FROM evaka_user WHERE id = n.updated_by) AS updated_by_name
FROM application_note n
WHERE application_id = :applicationId
AND created_by IN (SELECT employee_id FROM daycare_acl WHERE role = 'SPECIAL_EDUCATION_TEACHER'::user_role)
ORDER BY n.created
        """
            .trimIndent()

    return createQuery(sql).bind("applicationId", applicationId).toList<ApplicationNote>()
}

fun Database.Transaction.createApplicationNote(
    applicationId: ApplicationId,
    content: String,
    createdBy: EvakaUserId,
    messageContentId: MessageContentId? = null
): ApplicationNote {
    // language=SQL
    val sql =
        """
WITH new_note AS (
    INSERT INTO application_note (application_id, content, created_by, updated_by, message_content_id) VALUES (:applicationId, :content, :createdBy, :createdBy, :messageContentId)
    RETURNING *
) 
SELECT
    n.id,
    n.application_id,
    n.content,
    n.created_by,
    n.updated_by,
    eu.name AS created_by_name,
    eu.name AS updated_by_name,
    n.created,
    n.updated
FROM new_note n
LEFT JOIN evaka_user eu ON n.created_by = eu.id
        """
            .trimIndent()

    return createQuery(sql)
        .bind("applicationId", applicationId)
        .bind("content", content)
        .bind("createdBy", createdBy)
        .bind("messageContentId", messageContentId)
        .exactlyOne<ApplicationNote>()
}

fun Database.Transaction.updateApplicationNote(
    id: ApplicationNoteId,
    content: String,
    updatedBy: EvakaUserId
): ApplicationNote {
    // language=SQL
    val sql =
        """
WITH updated_note AS (
    UPDATE application_note SET content = :content, updated_by = :updatedBy WHERE id = :id
    RETURNING *
)
SELECT 
    n.id, n.application_id, n.content, 
    n.created, n.created_by, (SELECT name FROM evaka_user WHERE id = n.created_by) AS created_by_name,
    n.updated, n.updated_by, (SELECT name FROM evaka_user WHERE id = n.updated_by) AS updated_by_name
FROM updated_note n
        """

    return createQuery(sql)
        .bind("content", content)
        .bind("updatedBy", updatedBy)
        .bind("id", id)
        .exactlyOne<ApplicationNote>()
}

fun Database.Transaction.updateServiceWorkerApplicationNote(id: ApplicationId, content: String) {
    return createUpdate("UPDATE application SET service_worker_note = :content WHERE id = :id")
        .bind("id", id)
        .bind("content", content)
        .updateExactlyOne()
}

fun Database.Transaction.deleteApplicationNote(id: ApplicationNoteId) {
    // language=SQL
    val sql = "DELETE FROM application_note WHERE id = :id"

    createUpdate(sql).bind("id", id).execute()
}
