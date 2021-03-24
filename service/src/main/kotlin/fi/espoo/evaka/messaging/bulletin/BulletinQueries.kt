// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.bulletin

import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.utils.zoneId
import fi.espoo.evaka.shared.withCountMapper
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun Database.Transaction.initBulletin(
    user: AuthenticatedUser,
    receivers: List<BulletinReceiverTriplet>
): UUID {
    // language=sql
    val createBulletinSQL = """
        INSERT INTO bulletin (created_by_employee)
        VALUES (:createdBy)
        RETURNING id
    """.trimIndent()

    // language=sql
    val newBulletinId = this.createQuery(createBulletinSQL)
        .bind("createdBy", user.id)
        .mapTo<UUID>()
        .first()

    // language=sql
    val insertReceiversSQL = """
        INSERT INTO bulletin_receiver
        (bulletin_id, unit_id, group_id, child_id)
        VALUES (:newBulletinId, :unitId, :groupId, :childId)
    """.trimIndent()

    val batch = this.prepareBatch(insertReceiversSQL)

    receivers.forEach { receiver ->
        batch
                .bind("newBulletinId", newBulletinId)
                .bind("unitId", receiver.unitId)
                .bind("groupId", receiver.groupId)
                .bind("childId", receiver.personId)
    }

    batch.execute()

    return newBulletinId
}

fun Database.Transaction.updateDraftBulletin(
    user: AuthenticatedUser,
    id: UUID,
    title: String,
    content: String
) {
    // language=sql
    val updateBulletinSQL = """
        UPDATE bulletin
        SET title = :title, content = :content
        WHERE id = :id AND created_by_employee = :userId AND sent_at IS NULL
    """.trimIndent()

    val updated = this.createUpdate(updateBulletinSQL)
        .bind("id", id)
        .bind("userId", user.id)
        .bind("title", title)
        .bind("content", content)
        .execute()

    if (updated == 0) throw NotFound("No bulletin $id found by user ${user.id} in draft state")
}

fun Database.Transaction.deleteDraftBulletin(
    user: AuthenticatedUser,
    id: UUID
) {
    // language=sql
    val updateBulletinSQL = """
        DELETE FROM bulletin
        WHERE id = :id AND created_by_employee = :userId AND sent_at IS NULL 
    """.trimIndent()

    val deleted = this.createUpdate(updateBulletinSQL)
        .bind("id", id)
        .bind("userId", user.id)
        .execute()

    if (deleted == 0) throw NotFound("No bulletin $id found by user ${user.id} in draft state")
}

fun Database.Transaction.sendBulletin(
    user: AuthenticatedUser,
    id: UUID
) {
    // language=sql
    val updateBulletinSql = """
        UPDATE bulletin
        SET sent_at = :sentAt
        WHERE id = :id AND created_by_employee = :userId AND sent_at IS NULL 
    """.trimIndent()
    val sent = this.createUpdate(updateBulletinSql)
        .bind("id", id)
        .bind("userId", user.id)
        .bind("sentAt", OffsetDateTime.now(zoneId))
        .execute()

    if (sent == 0) throw NotFound("No bulletin $id found by user ${user.id} in draft state")

    // todo: include backup care?
    // language=sql
    val insertBulletinInstancesSql = """
        WITH children AS (
            SELECT pl.child_id, br.id AS bulletin_receiver_id
            FROM bulletin b
            JOIN bulletin_receiver br ON br.bulletin_id = b.id 
            JOIN daycare_group dg ON br.group_id = dg.id
            JOIN daycare_group_placement gpl ON dg.id = gpl.daycare_group_id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :date
            JOIN placement pl ON gpl.daycare_placement_id = pl.id
            WHERE b.id = :bulletinId
        ), receivers AS (
            SELECT g.guardian_id AS receiver_person_id, c.bulletin_receiver_id AS bulletin_receiver_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            WHERE NOT EXISTS(SELECT 1 FROM messaging_blocklist bl WHERE bl.child_id = c.child_id AND bl.blocked_recipient = g.guardian_id)
            
            UNION DISTINCT 
            
            SELECT fc.head_of_child AS receiver_person_id, c.bulletin_receiver_id AS bulletin_receiver_id
            FROM children c
            JOIN fridge_child fc ON fc.child_id = c.child_id AND daterange(fc.start_date, fc.end_date, '[]') @> :date
            WHERE NOT EXISTS(SELECT 1 FROM messaging_blocklist bl WHERE bl.child_id = c.child_id AND bl.blocked_recipient = fc.head_of_child)
        )
        INSERT INTO bulletin_instance (bulletin_receiver_id, receiver_person_id)
        SELECT bulletin_receiver_id, receiver_person_id FROM receivers
    """.trimIndent()

    this.createUpdate(insertBulletinInstancesSql)
        .bind("bulletinId", id)
        .bind("date", LocalDate.now(zoneId))
        .execute()
}

fun Database.Read.getSentBulletinsByUnit(
    unitId: UUID,
    page: Int,
    pageSize: Int
): Paged<Bulletin> {
    // language=sql
    val sql = """
        SELECT COUNT(b.*) OVER (), b.*, br.group_id AS group_id, dg.name AS group_name, concat(e.first_name, ' ', e.last_name) AS created_by_employee_name
        FROM bulletin b
        JOIN bulletin_receiver br ON br.bulletin_id = b.id
        JOIN employee e on b.created_by_employee = e.id
        JOIN daycare_group dg ON br.group_id = dg.id
        WHERE dg.daycare_id = :unitId AND b.sent_at IS NOT NULL
        ORDER BY b.sent_at DESC
        LIMIT :pageSize OFFSET (:page - 1) * :pageSize
    """.trimIndent()

    return this.createQuery(sql)
        .bind("unitId", unitId)
        .bind("page", page)
        .bind("pageSize", pageSize)
        .map(withCountMapper<Bulletin>())
        .let(mapToPaged(pageSize))
}

fun Database.Read.getBulletin(
    id: UUID
): Bulletin? {
    // language=sql
    val sql = """
        SELECT b.*, br.group_id AS group_id, br.unit_id AS unit_id, dg.name AS group_name, concat(e.first_name, ' ', e.last_name) AS created_by_employee_name
        FROM bulletin b
        JOIN employee e on b.created_by_employee = e.id
        LEFT JOIN bulletin_receiver br ON b.id = br.bulletin_id
        LEFT JOIN daycare_group dg ON br.group_id = dg.id
        
        WHERE b.id = :id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("id", id)
        .mapTo<Bulletin>()
        .firstOrNull()
}

fun Database.Read.getOwnBulletinDrafts(
    user: AuthenticatedUser,
    unitId: UUID,
    page: Int,
    pageSize: Int
): Paged<Bulletin> {
    // language=sql
    val sql = """
        SELECT COUNT(b.*) OVER (), b.*, br.group_id AS group_id, dg.name AS group_name, concat(e.first_name, ' ', e.last_name) AS created_by_employee_name
        FROM bulletin b
        JOIN bulletin_receiver br ON br.bulletin_id = b.id
        JOIN employee e on b.created_by_employee = e.id
        LEFT JOIN daycare_group dg ON br.group_id = dg.id
        WHERE b.created_by_employee = :userId AND b.sent_at IS NULL AND br.unit_id = :unitId 
        ORDER BY b.updated DESC
        LIMIT :pageSize OFFSET (:page - 1) * :pageSize
    """.trimIndent()

    return this.createQuery(sql)
        .bind("userId", user.id)
        .bind("unitId", unitId)
        .bind("page", page)
        .bind("pageSize", pageSize)
        .map(withCountMapper<Bulletin>())
        .let(mapToPaged(pageSize))
}

data class BulletinReceiversResult(
    val childId: UUID,
    val groupId: UUID,
    val groupName: String,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val receiverId: UUID,
    val receiverFirstName: String,
    val receiverLastName: String
)
fun Database.Read.getReceiversForNewBulletin(
    user: AuthenticatedUser,
    unitId: UUID
): List<BulletinControllerEmployee.BulletinReceiversResponse> {
    // language=sql
    val sql = """
        WITH children AS (
            SELECT pl.child_id, dg.id group_id, dg.name group_name
            FROM daycare_group dg
            JOIN daycare_group_placement gpl ON dg.id = gpl.daycare_group_id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :date
            JOIN placement pl ON gpl.daycare_placement_id = pl.id
        ), receivers AS (
            SELECT c.child_id, c.group_id, c.group_name, g.guardian_id AS receiver_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            
            UNION DISTINCT
            
            SELECT c.child_id, c.group_id, c.group_name, fc.head_of_child AS receiver_id
            FROM children c
            JOIN fridge_child fc ON fc.child_id = c.child_id AND daterange(fc.start_date, fc.end_date, '[]') @> :date
        )
        SELECT
            r.receiver_id,
            r.group_id,
            r.group_name,
            p.first_name receiver_first_name,
            p.last_name receiver_last_name,
            r.child_id,
            c.first_name child_first_name,
            c.last_name child_last_name,
            c.date_of_birth child_date_of_birth
        FROM receivers r
        JOIN person p ON r.receiver_id = p.id
        JOIN person c ON r.child_id = c.id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("date", LocalDate.now(zoneId))
        .bind("userId", user.id)
        .bind("unitId", unitId)
        .mapTo<BulletinReceiversResult>()
        .toList()
        .groupBy { it.groupId }
        .map { (groupId, receiverChildren) ->
            BulletinControllerEmployee.BulletinReceiversResponse(
                groupId = groupId,
                groupName = receiverChildren.first().groupName,
                receivers = receiverChildren.groupBy { it.childId }
                    .map { (childId, receivers) ->
                        BulletinControllerEmployee.BulletinReceiver(
                            childId = childId,
                            childFirstName = receivers.first().childFirstName,
                            childLastName = receivers.first().childLastName,
                            childDateOfBirth = receivers.first().childDateOfBirth,
                            receiverPersons = receivers.map { it ->
                                BulletinControllerEmployee.BulletinReceiverPerson(
                                    receiverId = it.receiverId,
                                    receiverFirstName = it.receiverFirstName,
                                    receiverLastName = it.receiverLastName
                                )
                            }
                        )
                    }
            )
        }
}

fun Database.Read.getReceivedBulletinsByGuardian(
    user: AuthenticatedUser,
    page: Int,
    pageSize: Int
): Paged<ReceivedBulletin> {
    // language=sql
    val sql = """
        SELECT COUNT(bi.*) OVER (), bi.id, b.sent_at, b.title, b.content, bi.read_at IS NOT NULL AS is_read, dg.name as sender
        FROM bulletin_instance bi
        JOIN bulletin_receiver br ON bi.bulletin_receiver_id = br.id
        JOIN bulletin b ON br.bulletin_id = b.id
        JOIN daycare_group dg on br.group_id = dg.id
        WHERE bi.receiver_person_id = :userId AND b.sent_at IS NOT NULL 
        ORDER BY b.sent_at DESC
        LIMIT :pageSize OFFSET (:page - 1) * :pageSize
    """.trimIndent()

    return this.createQuery(sql)
        .bind("userId", user.id)
        .bind("page", page)
        .bind("pageSize", pageSize)
        .map(withCountMapper<ReceivedBulletin>())
        .let(mapToPaged(pageSize))
}

fun Database.Read.getUnreadBulletinCountByGuardian(
    user: AuthenticatedUser
): Int {
    // language=sql
    val sql = """
        SELECT count(distinct bi.id) AS count
        FROM bulletin_instance bi
        WHERE bi.receiver_person_id = :userId AND bi.read_at IS NULL
    """.trimIndent()

    return this.createQuery(sql)
        .bind("userId", user.id)
        .mapTo<Int>()
        .first()
}

fun Database.Transaction.markBulletinRead(
    user: AuthenticatedUser,
    id: UUID
) {
    // language=sql
    val sql = """
        UPDATE bulletin_instance
        SET read_at = :readAt
        WHERE id = :id AND receiver_person_id = :userId AND read_at IS NULL
    """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("userId", user.id)
        .bind("readAt", OffsetDateTime.now(zoneId))
        .execute()
}
