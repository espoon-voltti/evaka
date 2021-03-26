package fi.espoo.evaka.messaging.bulletin

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.utils.zoneId
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RestController
class ChildRecipientsController(
    private val acl: AccessControlList
) {

    @GetMapping("/child/{childId}/recipients")
    fun getRecipients(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<List<Recipient>> {
        Audit.MessagingBlocklistRead.log(childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(
            UserRole.ADMIN, UserRole.FINANCE_ADMIN, UserRole.SERVICE_WORKER,
            UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.STAFF
        )

        return db.read { fetchRecipients(it, Instant.now(), childId) }
            .let { ResponseEntity.ok(it) }
    }

    data class EditRecipientRequest(
        val blocklisted: Boolean
    )
    @PutMapping("/child/{childId}/recipients/{personId}")
    fun editRecipient(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @PathVariable personId: UUID,
        @RequestBody body: EditRecipientRequest
    ): ResponseEntity<Unit> {
        Audit.MessagingBlocklistEdit.log(childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(
            UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR
        )

        db.transaction { tx ->
            if (body.blocklisted) {
                addToBlocklist(tx, childId, personId)
            } else {
                removeFromBlocklist(tx, childId, personId)
            }
        }

        return ResponseEntity.noContent().build()
    }
}

fun addToBlocklist(tx: Database.Transaction, childId: UUID, recipientId: UUID) {
    // language=sql
    val sql = """
        INSERT INTO messaging_blocklist (child_id, blocked_recipient)
        VALUES (:childId, :recipient)
        ON CONFLICT DO NOTHING;
    """.trimIndent()

    tx.createUpdate(sql)
        .bind("childId", childId)
        .bind("recipient", recipientId)
        .execute()
}

fun removeFromBlocklist(tx: Database.Transaction, childId: UUID, recipientId: UUID) {
    // language=sql
    val sql = """
        DELETE FROM messaging_blocklist
        WHERE child_id = :childId AND blocked_recipient = :recipient
    """.trimIndent()

    tx.createUpdate(sql)
        .bind("childId", childId)
        .bind("recipient", recipientId)
        .execute()
}

fun fetchRecipients(tx: Database.Read, now: Instant, childId: UUID): List<Recipient> {
    // language=sql
    val sql = """
        WITH guardians AS (
            SELECT g.guardian_id AS person_id
            FROM guardian g
            WHERE g.child_id = :childId
        ), head_of_child AS (
            SELECT fc.head_of_child AS person_id
            FROM fridge_child fc 
            WHERE fc.child_id = :childId AND daterange(fc.start_date, fc.end_date, '[]') @> :date
        ), recipient_ids AS (
            SELECT person_id FROM guardians
            UNION DISTINCT 
            SELECT person_id FROM head_of_child
        )
        SELECT 
            person_id,
            first_name,
            last_name,
            EXISTS(SELECT 1 FROM guardians g WHERE g.person_id = r.person_id) AS guardian,
            EXISTS(SELECT 1 FROM head_of_child hoc WHERE hoc.person_id = r.person_id) AS head_of_child,
            EXISTS(SELECT 1 FROM messaging_blocklist bl WHERE bl.child_id = :childId AND bl.blocked_recipient = person_id) AS blocklisted
        FROM recipient_ids r
        JOIN person p ON r.person_id = p.id
    """.trimIndent()

    return tx.createQuery(sql)
        .bind("childId", childId)
        .bind("date", LocalDate.ofInstant(now, zoneId))
        .mapTo<Recipient>()
        .list()
}

data class Recipient(
    val personId: String,
    val firstName: String,
    val lastName: String,
    val guardian: Boolean,
    val headOfChild: Boolean,
    val blocklisted: Boolean
)
