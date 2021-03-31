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
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun Database.Transaction.initBulletin(
    user: AuthenticatedUser,
    sender: String,
    receivers: List<BulletinReceiverTriplet>
): UUID {
    // language=sql
    val createBulletinSQL = """
        INSERT INTO bulletin (sender, created_by_employee)
        VALUES (:sender, :createdBy)
        RETURNING id
    """.trimIndent()

    // language=sql
    val newBulletinId = this.createQuery(createBulletinSQL)
        .bind("sender", sender)
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
            .add()
    }

    batch.execute()

    return newBulletinId
}

fun Database.Transaction.updateDraftBulletin(
    user: AuthenticatedUser,
    id: UUID,
    title: String,
    content: String,
    sender: String
) {
    // language=sql
    val updateBulletinSQL = """
        UPDATE bulletin
        SET title = :title, content = :content, sender = :sender
        WHERE id = :id AND created_by_employee = :userId AND sent_at IS NULL
    """.trimIndent()

    val updated = this.createUpdate(updateBulletinSQL)
        .bind("id", id)
        .bind("userId", user.id)
        .bind("title", title)
        .bind("content", content)
        .bind("sender", sender)
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
            SELECT child_id, bulletin_id FROM bulletin_receiver
            WHERE
            bulletin_id = :bulletinId 
            AND unit_id IS NOT NULL 
            AND group_id IS NOT NULL 
            AND child_id IS NOT NULL
            
            UNION DISTINCT
            
            SELECT pl.child_id, br.bulletin_id
            FROM bulletin b
            JOIN bulletin_receiver br ON br.bulletin_id = b.id 
            JOIN daycare_group dg ON br.group_id = dg.id
            JOIN daycare_group_placement gpl ON dg.id = gpl.daycare_group_id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :date
            JOIN placement pl ON gpl.daycare_placement_id = pl.id
            WHERE b.id = :bulletinId 
            AND br.unit_id IS NOT NULL 
            AND br.group_id IS NOT NULL 
            AND br.child_id IS NULL
            
            UNION DISTINCT
            
            SELECT pl.child_id, br.bulletin_id
            FROM bulletin b
            JOIN bulletin_receiver br ON br.bulletin_id = b.id
            JOIN placement pl ON br.unit_id = pl.unit_id AND daterange(pl.start_date, pl.end_date, '[]') @> :date
            WHERE b.id = :bulletinId 
        ), receivers AS (
            SELECT g.guardian_id AS receiver_person_id, c.bulletin_id AS bulletin_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            WHERE NOT EXISTS(SELECT 1 FROM messaging_blocklist bl WHERE bl.child_id = c.child_id AND bl.blocked_recipient = g.guardian_id)
            
            UNION DISTINCT 
            
            SELECT fc.head_of_child AS receiver_person_id, c.bulletin_id AS bulletin_id
            FROM children c
            JOIN fridge_child fc ON fc.child_id = c.child_id AND daterange(fc.start_date, fc.end_date, '[]') @> :date
            WHERE NOT EXISTS(SELECT 1 FROM messaging_blocklist bl WHERE bl.child_id = c.child_id AND bl.blocked_recipient = fc.head_of_child)
        )
        INSERT INTO bulletin_instance (bulletin_id, receiver_person_id)
        SELECT DISTINCT bulletin_id, receiver_person_id FROM receivers
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
        WITH limited_bulletins AS (
        SELECT
            COUNT(b.id) OVER (),
            b.id,
            b.sender,
            b.title,
            b.content,
            b.sent_at,
            b.created_by_employee,
            concat(e.first_name, ' ', e.last_name) AS created_by_employee_name
            FROM bulletin b 
            JOIN employee e on b.created_by_employee = e.id
            ORDER BY b.sent_at DESC
            LIMIT :pageSize OFFSET (:page - 1) * :pageSize
        )
        SELECT 
            b.*,
            br.bulletin_id,
            d.id as unit_id,
            d.name as unit_name,
            dg.id as group_id,
            dg.name as group_name,
            c.id as child_id,
            c.first_name as child_first_name,
            c.last_name as child_last_name
        FROM limited_bulletins b
        JOIN bulletin_receiver br ON br.bulletin_id = b.id
        JOIN daycare d ON br.unit_id = d.id
        LEFT JOIN daycare_group dg ON br.group_id = dg.id
        LEFT JOIN person c ON br.child_id = c.id
        WHERE br.unit_id = :unitId AND b.sent_at IS NOT NULL
    """.trimIndent()

    val pagedRawBulletinResults = this.createQuery(sql)
        .bind("unitId", unitId)
        .bind("page", page)
        .bind("pageSize", pageSize)
        .map(withCountMapper<BulletinRaw>())
        .let(mapToPaged(pageSize))

    val data: List<Bulletin> = pagedRawBulletinResults.data
        .groupBy { raw -> raw.id }
        .map { (_, rawBulletins) ->
            mapRawBulletin(rawBulletins)
        }
        .filterNotNull()

    return Paged(data, pages = pagedRawBulletinResults.pages, total = pagedRawBulletinResults.total)
}

data class BulletinRaw(
    val id: UUID,
    val sender: String,
    val title: String,
    val content: String,
    val unitId: UUID,
    val unitName: String,
    val groupId: UUID?,
    val groupName: String?,
    val childId: UUID?,
    val childFirstName: String?,
    val childLastName: String?,
    val createdByEmployee: UUID,
    val createdByEmployeeName: String,
    val sentAt: Instant?
)
fun Database.Read.getBulletin(
    id: UUID
): Bulletin? {
    // language=sql
    val sql = """
        SELECT 
            b.id,
            b.sender,
            b.title,
            b.content,
            b.sent_at,
            b.created_by_employee,
            d.id as unit_id,
            d.name as unit_name,
            dg.id as group_id,
            dg.name as group_name,
            c.id as child_id,
            c.first_name as child_first_name,
            c.last_name as child_last_name,
            concat(e.first_name, ' ', e.last_name) AS created_by_employee_name
        FROM bulletin b
        JOIN bulletin_receiver br ON br.bulletin_id = b.id
        JOIN daycare d ON br.unit_id = d.id
        LEFT JOIN daycare_group dg ON br.group_id = dg.id
        LEFT JOIN person c ON br.child_id = c.id
        JOIN employee e ON b.created_by_employee = e.id
        WHERE b.id = :id
    """.trimIndent()

    val rawBulletins = this.createQuery(sql)
        .bind("id", id)
        .mapTo<BulletinRaw>()
        .toList()

    return mapRawBulletin(rawBulletins)
}

private fun mapRawBulletin(rawBulletinsByBulletinId: List<BulletinRaw>): Bulletin? {
    val receiverUnits = rawBulletinsByBulletinId.filter { it.groupId == null && it.childId == null }.map {
        ReceiverUnit(
            unitId = it.unitId,
            unitName = it.unitName
        )
    }.distinctBy { it.unitId }

    val receiverGroups = rawBulletinsByBulletinId.filter { it.groupId != null && it.childId == null }.map {
        ReceiverGroup(
            unitId = it.unitId,
            groupId = it.groupId!!,
            groupName = it.groupName!!
        )
    }.distinctBy { it.groupId }

    val receiverChildren = rawBulletinsByBulletinId.filter { it.groupId != null && it.childId != null }.map {
        ReceiverChild(
            childId = it.childId!!,
            firstName = it.childFirstName!!,
            lastName = it.childLastName!!,
            receiverPersons = listOf()
        )
    }.distinctBy { it.childId }

    return rawBulletinsByBulletinId.firstOrNull()?.let {
        Bulletin(
            id = it.id,
            sender = it.sender,
            title = it.title,
            content = it.content,
            createdByEmployee = it.createdByEmployee,
            createdByEmployeeName = it.createdByEmployeeName,
            receiverUnits = receiverUnits,
            receiverGroups = receiverGroups,
            receiverChildren = receiverChildren,
            sentAt = it.sentAt
        )
    }
}

fun Database.Read.getOwnBulletinDrafts(
    user: AuthenticatedUser,
    unitId: UUID,
    page: Int,
    pageSize: Int
): Paged<Bulletin> {
    // language=sql
    val sql = """
        WITH limited_bulletins AS (
        SELECT
            COUNT(b.id) OVER (),
            b.id,
            b.sender,
            b.title,
            b.content,
            b.sent_at,
            b.created_by_employee,
            concat(e.first_name, ' ', e.last_name) AS created_by_employee_name
            FROM bulletin b 
            JOIN employee e on b.created_by_employee = e.id
            WHERE b.sent_at IS NULL
            ORDER BY b.updated DESC
            LIMIT :pageSize OFFSET (:page - 1) * :pageSize
        )
        SELECT 
            b.*,
            br.bulletin_id,
            d.id as unit_id,
            d.name as unit_name,
            dg.id as group_id,
            dg.name as group_name,
            c.id as child_id,
            c.first_name as child_first_name,
            c.last_name as child_last_name
        FROM limited_bulletins b
        JOIN bulletin_receiver br ON br.bulletin_id = b.id
        JOIN daycare d ON br.unit_id = d.id
        LEFT JOIN daycare_group dg ON br.group_id = dg.id
        LEFT JOIN person c ON br.child_id = c.id
        WHERE b.created_by_employee = :userId AND br.unit_id = :unitId 
    """.trimIndent()

    val pagedRawBulletinResults = this.createQuery(sql)
        .bind("userId", user.id)
        .bind("unitId", unitId)
        .bind("page", page)
        .bind("pageSize", pageSize)
        .map(withCountMapper<BulletinRaw>())
        .let(mapToPaged(pageSize))

    val data: List<Bulletin> = pagedRawBulletinResults.data
        .groupBy { raw -> raw.id }
        .map { (_, rawBulletins) ->
            mapRawBulletin(rawBulletins)
        }
        .filterNotNull()

    return Paged(data, pages = pagedRawBulletinResults.pages, total = pagedRawBulletinResults.total)
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
        SELECT COUNT(bi.*) OVER (), 
            bi.id, 
            b.sent_at, 
            b.sender, 
            b.title, 
            b.content, 
            bi.read_at IS NOT NULL AS is_read
        FROM bulletin_instance bi
        JOIN bulletin b ON bi.bulletin_id = b.id
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
