// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.FamilyContact
import fi.espoo.evaka.pis.FamilyContactRole.LOCAL_GUARDIAN
import fi.espoo.evaka.pis.FamilyContactRole.REMOTE_GUARDIAN
import fi.espoo.evaka.pis.service.FamilyOverview
import fi.espoo.evaka.pis.service.FamilyOverviewService
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/family")
class FamilyController(
    private val familyOverviewService: FamilyOverviewService,
    private val acl: AccessControlList
) {
    @GetMapping("/by-adult/{id}")
    fun getFamilyByPerson(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: UUID
    ): ResponseEntity<FamilyOverview> {
        Audit.PisFamilyRead.log(targetId = id)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val result = db.read { familyOverviewService.getFamilyByAdult(it, id) }
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    @GetMapping("/contacts")
    fun getFamilyContactSummary(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam(value = "childId", required = true) childId: UUID
    ): ResponseEntity<List<FamilyContact>> {
        Audit.FamilyContactsRead.log(targetId = childId)
        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(
                UserRole.ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        return db
            .read { it.fetchFamilyContacts(childId) }
            .let { addDefaultPriorities(it) }
            .let { ResponseEntity.ok(it) }
    }

    @PostMapping("/contacts")
    fun updateFamilyContactPriority(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: FamilyContactUpdate
    ): ResponseEntity<Unit> {
        Audit.FamilyContactsUpdate.log(targetId = body.childId, objectId = body.contactPersonId)
        acl.getRolesForChild(user, body.childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction { it.updateFamilyContact(body.childId, body.contactPersonId, body.priority) }

        return ResponseEntity.noContent().build()
    }
}

data class FamilyContactUpdate(
    val childId: UUID,
    val contactPersonId: UUID,
    val priority: Int?
)

private fun Database.Transaction.updateFamilyContact(childId: UUID, contactPersonId: UUID, priority: Int?) {
    this.execute("SET CONSTRAINTS unique_child_contact_person_pair, unique_child_priority_pair DEFERRED")

    this.createUpdate(
        """
WITH deleted_contact AS (
    DELETE FROM family_contact WHERE child_id = :childId AND contact_person_id = :contactPersonId RETURNING priority AS old_priority
)
UPDATE family_contact SET priority = priority - 1
FROM deleted_contact
WHERE child_id = :childId AND priority > old_priority
"""
    )
        .bind("childId", childId)
        .bind("contactPersonId", contactPersonId)
        .execute()

    if (priority == null) return

    this.createUpdate(
        """
UPDATE family_contact SET priority = priority + 1 WHERE child_id = :childId AND priority >= :priority;
INSERT INTO family_contact (child_id, contact_person_id, priority) VALUES (:childId, :contactPersonId, :priority);
"""
    )
        .bind("childId", childId)
        .bind("contactPersonId", contactPersonId)
        .bind("priority", priority)
        .execute()
}

fun Database.Read.fetchFamilyContacts(childId: UUID): List<FamilyContact> {
    // language=sql
    val sql =
        """
WITH contact AS (
    -- adults in the same household
    SELECT
        CASE
            WHEN EXISTS (SELECT 1 FROM guardian g WHERE g.guardian_id = p.id AND g.child_id = :id)
            THEN 'LOCAL_GUARDIAN' ELSE 'LOCAL_ADULT'
        END AS role, p.id, p.first_name, p.last_name, p.email, p.phone, p.backup_phone, p.street_address, p.postal_code, p.post_office, family_contact.priority
    FROM person p
    LEFT JOIN family_contact ON family_contact.contact_person_id = p.id AND family_contact.child_id = :id
    WHERE EXISTS ( -- is either head of child or their partner
        SELECT 1 FROM fridge_child fc
        WHERE fc.child_id = :id AND daterange(fc.start_date, fc.end_date, '[]') @> now()::date AND (
            fc.head_of_child = p.id OR EXISTS(
                SELECT 1 FROM fridge_partner_view fp
                WHERE fp.person_id = fc.head_of_child AND fp.partner_person_id = p.id AND daterange(fp.start_date, fp.end_date, '[]') @> now()::date
            )
        )
    )

    UNION

    -- siblings in the same household
    SELECT 'LOCAL_SIBLING' AS role, p.id, p.first_name, p.last_name, NULL AS email, NULL AS phone, NULL AS backup_phone, p.street_address, p.postal_code, p.post_office, family_contact.priority
    FROM person p
    LEFT JOIN family_contact ON family_contact.contact_person_id = p.id AND family_contact.child_id = :id
    WHERE EXISTS (
        SELECT 1 FROM fridge_child fc1
        WHERE fc1.child_id = :id AND daterange(fc1.start_date, fc1.end_date, '[]') @> now()::date AND EXISTS (
            SELECT 1 FROM fridge_child fc2
            WHERE
                fc2.head_of_child = fc1.head_of_child
                AND fc2.child_id = p.id
                AND fc2.child_id <> fc1.child_id
                AND daterange(fc1.start_date, fc1.end_date, '[]') @> now()::date
        )
    )

    UNION

    -- guardians in other households
    SELECT 'REMOTE_GUARDIAN' AS role, p.id, p.first_name, p.last_name, p.email, p.phone, p.backup_phone, p.street_address, p.postal_code, p.post_office, family_contact.priority
    FROM person p
    LEFT JOIN family_contact ON family_contact.contact_person_id = p.id AND family_contact.child_id = :id
    WHERE
        EXISTS (SELECT 1 FROM guardian g WHERE g.guardian_id = p.id AND g.child_id = :id) -- is a guardian
        AND NOT EXISTS ( -- but is neither head of child nor their partner
            SELECT 1 FROM fridge_child fc
            WHERE fc.child_id = :id AND daterange(fc.start_date, fc.end_date, '[]') @> now()::date AND (
                fc.head_of_child = p.id OR EXISTS (
                    SELECT 1 FROM fridge_partner_view fp
                    WHERE
                        fp.person_id = fc.head_of_child
                        AND fp.partner_person_id = p.id
                        AND daterange(fp.start_date, fp.end_date, '[]') @> now()::date
                )
            )
        )
)
SELECT
    *,
    (CASE role
        WHEN 'LOCAL_GUARDIAN' THEN 1
        WHEN 'LOCAL_ADULT' THEN 2
        WHEN 'LOCAL_SIBLING' THEN 3
        WHEN 'REMOTE_GUARDIAN' THEN 4
    END) AS role_order
FROM contact
ORDER BY priority ASC, role_order ASC
    """

    return createQuery(sql)
        .bind("id", childId)
        .mapTo<FamilyContact>()
        .toList()
}

private val defaultContacts = setOf(LOCAL_GUARDIAN, REMOTE_GUARDIAN)

private fun addDefaultPriorities(contacts: List<FamilyContact>): List<FamilyContact> =
    if (contacts.none { it.priority != null })
        contacts
            .fold(listOf<FamilyContact>()) { acc, contact ->
                acc + if (defaultContacts.contains(contact.role)) {
                    val highestPriority = acc.mapNotNull { it.priority }.maxOrNull() ?: 0
                    contact.copy(priority = highestPriority + 1)
                } else contact
            }
            .sortedWith(compareBy(nullsLast()) { it.priority })
    else contacts
