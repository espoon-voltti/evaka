package fi.espoo.evaka.messaging.bulletin

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.utils.zoneId
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun Database.Transaction.initBulletin(
    user: AuthenticatedUser,
    unitId: UUID
): UUID {
    // language=sql
    val sql = """
        INSERT INTO bulletin (created_by_employee, unit_id)
        VALUES (:createdBy, :unitId)
        RETURNING id
    """.trimIndent()
    return this.createQuery(sql)
        .bind("createdBy", user.id)
        .bind("unitId", unitId)
        .mapTo<UUID>()
        .first()
}

fun Database.Transaction.updateDraftBulletin(
    user: AuthenticatedUser,
    id: UUID,
    groupId: UUID?,
    title: String,
    content: String
) {
    // language=sql
    val sql = """
        UPDATE bulletin
        SET group_id = :groupId, title = :title, content = :content
        WHERE id = :id AND created_by_employee = :userId AND sent_at IS NULL
    """.trimIndent()

    val updated = this.createUpdate(sql)
        .bind("id", id)
        .bind("userId", user.id)
        .bind("groupId", groupId)
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
    val updateBulletinSql = """
        DELETE FROM bulletin
        WHERE id = :id AND created_by_employee = :userId AND sent_at IS NULL 
    """.trimIndent()

    val deleted = this.createUpdate(updateBulletinSql)
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
            SELECT pl.child_id
            FROM bulletin b
            JOIN daycare_group dg ON b.group_id = dg.id
            JOIN daycare_group_placement gpl ON dg.id = gpl.daycare_group_id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :date
            JOIN placement pl ON gpl.daycare_placement_id = pl.id
            WHERE b.id = :bulletinId
        ), receivers AS (
            SELECT g.guardian_id AS receiver_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            
            UNION DISTINCT 
            
            SELECT fc.head_of_child AS receiver_id
            FROM children c
            JOIN fridge_child fc ON fc.child_id = c.child_id AND daterange(fc.start_date, fc.end_date, '[]') @> :date
        )
        INSERT INTO bulletin_instance (bulletin_id, receiver_id)
        SELECT :bulletinId, receiver_id FROM receivers
    """.trimIndent()

    this.createUpdate(insertBulletinInstancesSql)
        .bind("bulletinId", id)
        .bind("date", LocalDate.now(zoneId))
        .execute()
}

fun Database.Read.getSentBulletinsByUnit(
    unitId: UUID
): List<Bulletin> {
    // language=sql
    val sql = """
        SELECT b.*, dg.name AS group_name, concat(e.first_name, ' ', e.last_name) AS created_by_employee_name
        FROM bulletin b
        JOIN employee e on b.created_by_employee = e.id
        JOIN daycare_group dg ON b.group_id = dg.id
        WHERE dg.daycare_id = :unitId AND b.sent_at IS NOT NULL
        ORDER BY b.sent_at DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bind("unitId", unitId)
        .mapTo<Bulletin>()
        .list()
}

fun Database.Read.getBulletin(
    id: UUID
): Bulletin? {
    // language=sql
    val sql = """
        SELECT b.*, dg.name AS group_name, concat(e.first_name, ' ', e.last_name) AS created_by_employee_name
        FROM bulletin b
        JOIN employee e on b.created_by_employee = e.id
        LEFT JOIN daycare_group dg ON b.group_id = dg.id
        
        WHERE b.id = :id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("id", id)
        .mapTo<Bulletin>()
        .firstOrNull()
}

fun Database.Read.getOwnBulletinDrafts(
    user: AuthenticatedUser,
    unitId: UUID
): List<Bulletin> {
    // language=sql
    val sql = """
        SELECT b.*, dg.name AS group_name, concat(e.first_name, ' ', e.last_name) AS created_by_employee_name
        FROM bulletin b
        JOIN employee e on b.created_by_employee = e.id
        LEFT JOIN daycare_group dg ON b.group_id = dg.id
        WHERE b.created_by_employee = :userId AND b.sent_at IS NULL AND b.unit_id = :unitId 
        ORDER BY b.updated DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bind("userId", user.id)
        .bind("unitId", unitId)
        .mapTo<Bulletin>()
        .list()
}

fun Database.Read.getReceivedBulletinsByGuardian(
    user: AuthenticatedUser
): List<ReceivedBulletin> {
    // language=sql
    val sql = """
        SELECT DISTINCT b.id, b.sent_at, b.title, b.content, bi.read_at IS NOT NULL AS is_read, dg.name as sender
        FROM bulletin b
        JOIN bulletin_instance bi ON b.id = bi.bulletin_id
        JOIN daycare_group dg on b.group_id = dg.id
        WHERE bi.receiver_id = :userId AND b.sent_at IS NOT NULL 
        ORDER BY b.sent_at DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bind("userId", user.id)
        .mapTo<ReceivedBulletin>()
        .list()
}

fun Database.Transaction.markBulletinRead(
    user: AuthenticatedUser,
    id: UUID
) {
    // language=sql
    val sql = """
        UPDATE bulletin_instance
        SET read_at = :readAt
        WHERE bulletin_id = :id AND receiver_id = :userId AND read_at IS NULL 
    """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("userId", user.id)
        .bind("readAt", OffsetDateTime.now(zoneId))
        .execute()
}
