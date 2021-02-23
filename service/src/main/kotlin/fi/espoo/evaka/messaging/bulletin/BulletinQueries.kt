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
    user: AuthenticatedUser
): UUID {
    // language=sql
    val sql = """
        INSERT INTO bulletin (created_by_employee)
        VALUES (:createdBy)
        RETURNING id
    """.trimIndent()
    return this.createQuery(sql)
        .bind("createdBy", user.id)
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
        INSERT INTO bulletin_instance (bulletin_id, guardian_id)
        SELECT DISTINCT m.id, g.guardian_id
        FROM bulletin m
        JOIN daycare_group dg ON m.group_id = dg.id
        JOIN daycare_group_placement gpl ON dg.id = gpl.daycare_group_id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :date
        JOIN placement pl ON gpl.daycare_placement_id = pl.id
        JOIN guardian g ON pl.child_id = g.child_id
        WHERE m.id = :bulletinId
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
        SELECT m.*
        FROM bulletin m
        JOIN daycare_group dg ON m.group_id = dg.id
        WHERE dg.daycare_id = :unitId AND m.sent_at IS NOT NULL
        ORDER BY m.sent_at DESC
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
        SELECT m.*
        FROM bulletin m
        WHERE m.id = :id
        ORDER BY m.updated DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bind("id", id)
        .mapTo<Bulletin>()
        .firstOrNull()
}

fun Database.Read.getOwnBulletinDrafts(
    user: AuthenticatedUser
): List<Bulletin> {
    // language=sql
    val sql = """
        SELECT m.*
        FROM bulletin m
        WHERE m.created_by_employee = :userId AND m.sent_at IS NULL 
        ORDER BY m.updated DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bind("userId", user.id)
        .mapTo<Bulletin>()
        .list()
}

fun Database.Read.getReceivedBulletinsByGuardian(
    user: AuthenticatedUser
): List<ReceivedBulletin> {
    // language=sql
    val sql = """
        SELECT DISTINCT m.id, m.sent_at, m.title, m.content, mi.read_at IS NOT NULL AS is_read, dg.name as sender
        FROM bulletin m
        JOIN bulletin_instance mi ON m.id = mi.bulletin_id
        JOIN daycare_group dg on m.group_id = dg.id
        WHERE mi.guardian_id = :userId
        ORDER BY m.sent_at DESC
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
        WHERE bulletin_id = :id AND guardian_id = :userId AND read_at IS NULL 
    """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("userId", user.id)
        .bind("readAt", OffsetDateTime.now(zoneId))
        .execute()
}
