// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.FamilyContact
import fi.espoo.evaka.pis.FamilyContactRole.LOCAL_FOSTER_PARENT
import fi.espoo.evaka.pis.FamilyContactRole.LOCAL_GUARDIAN
import fi.espoo.evaka.pis.FamilyContactRole.REMOTE_FOSTER_PARENT
import fi.espoo.evaka.pis.FamilyContactRole.REMOTE_GUARDIAN
import fi.espoo.evaka.pis.service.FamilyOverview
import fi.espoo.evaka.pis.service.FamilyOverviewService
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/family")
class FamilyController(
    private val familyOverviewService: FamilyOverviewService,
    private val personService: PersonService,
    private val accessControl: AccessControl
) {
    @GetMapping("/by-adult/{id}")
    fun getFamilyByPerson(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") id: PersonId
    ): FamilyOverview {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.READ_FAMILY_OVERVIEW,
                        id
                    )
                    val includeIncome =
                        accessControl.hasPermissionFor(
                            it,
                            user,
                            clock,
                            Action.Person.READ_INCOME,
                            id
                        )

                    val overview = familyOverviewService.getFamilyByAdult(it, clock, id)
                    if (includeIncome) {
                        overview
                    } else {
                        overview?.copy(
                            headOfFamily = overview.headOfFamily.copy(income = null),
                            partner = overview.partner?.copy(income = null),
                            totalIncome = null
                        )
                    }
                } ?: throw NotFound("No family overview found for person $id")
            }
            .also { Audit.PisFamilyRead.log(targetId = id) }
    }

    @GetMapping("/contacts")
    fun getFamilyContactSummary(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam(value = "childId", required = true) childId: ChildId
    ): List<FamilyContact> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.READ_FAMILY_CONTACTS,
                        childId
                    )
                    it.fetchFamilyContacts(clock.today(), childId)
                }
            }
            .also {
                Audit.FamilyContactsRead.log(targetId = childId, meta = mapOf("count" to it.size))
            }
    }

    @PostMapping("/contacts")
    fun updateFamilyContactDetails(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: FamilyContactUpdate
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Child.UPDATE_FAMILY_CONTACT_DETAILS,
                    body.childId
                )
                if (
                    !it.isFamilyContactForChild(clock.today(), body.childId, body.contactPersonId)
                ) {
                    throw BadRequest("Invalid child or contact person")
                }
                personService.patchUserDetails(
                    it,
                    body.contactPersonId,
                    PersonPatch(
                        email = body.email,
                        phone = body.phone,
                        backupPhone = body.backupPhone
                    )
                )
            }
        }
        Audit.FamilyContactsUpdate.log(targetId = body.childId, objectId = body.contactPersonId)
    }

    @PostMapping("/contacts/priority")
    fun updateFamilyContactPriority(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: FamilyContactPriorityUpdate
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Child.UPDATE_FAMILY_CONTACT_PRIORITY,
                    body.childId
                )
                if (
                    !it.isFamilyContactForChild(clock.today(), body.childId, body.contactPersonId)
                ) {
                    throw BadRequest("Invalid child or contact person")
                }
                it.updateFamilyContactPriority(body.childId, body.contactPersonId, body.priority)
            }
        }
        Audit.FamilyContactsUpdate.log(targetId = body.childId, objectId = body.contactPersonId)
    }
}

data class FamilyContactUpdate(
    val childId: ChildId,
    val contactPersonId: PersonId,
    val email: String?,
    val phone: String?,
    val backupPhone: String?
)

data class FamilyContactPriorityUpdate(
    val childId: ChildId,
    val contactPersonId: PersonId,
    val priority: Int?
)

private fun Database.Transaction.updateFamilyContactPriority(
    childId: ChildId,
    contactPersonId: PersonId,
    priority: Int?
) {
    this.execute(
        "SET CONSTRAINTS unique_child_contact_person_pair, unique_child_priority_pair DEFERRED"
    )

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

fun Database.Read.isFamilyContactForChild(
    today: LocalDate,
    childId: ChildId,
    personId: PersonId
): Boolean {
    return createQuery(
            """
SELECT EXISTS (
    -- is a guardian
    SELECT 1 FROM guardian
    WHERE child_id = :childId AND guardian_id = :personId
) OR EXISTS (
    -- is either a head of child or their partner
    SELECT 1 FROM fridge_child fc
    WHERE 
        fc.child_id = :childId
        AND daterange(fc.start_date, fc.end_date, '[]') @> :today 
        AND (
            fc.head_of_child = :personId 
            OR EXISTS (
                SELECT 1 FROM fridge_partner_view fp
                WHERE 
                    fp.person_id = fc.head_of_child 
                    AND fp.partner_person_id = :personId 
                    AND daterange(fp.start_date, fp.end_date, '[]') @> :today
            )
        )
)
"""
        )
        .bind("today", today)
        .bind("childId", childId)
        .bind("personId", personId)
        .exactlyOne<Boolean>()
}

fun Database.Read.fetchFamilyContacts(today: LocalDate, childId: ChildId): List<FamilyContact> =
    createQuery<Any> {
            sql(
                """
WITH child_guardian AS (
    SELECT guardian_id AS id FROM guardian
    WHERE child_id = ${bind(childId)}
), child_foster_parent AS (
    SELECT parent_id AS id FROM foster_parent
    WHERE child_id = ${bind(childId)} AND valid_during @> ${bind(today)}
), head_of_child AS (
    SELECT head_of_child AS id FROM fridge_child fc
    WHERE fc.child_id = ${bind(childId)} AND daterange(fc.start_date, fc.end_date, '[]') @> ${bind(today)}
), head_of_child_partner AS (
    SELECT partner_person_id AS id FROM head_of_child hoc
    JOIN fridge_partner_view fp ON hoc.id = fp.person_id AND daterange(fp.start_date, fp.end_date, '[]') @> ${bind(today)}
), same_household_adult AS (
    SELECT id FROM head_of_child
    UNION ALL
    SELECT id FROM head_of_child_partner
), contact AS (
    -- adults in the same household
    SELECT
        id,
        CASE
            WHEN EXISTS (SELECT FROM child_guardian WHERE id = adult.id) THEN 'LOCAL_GUARDIAN'
            WHEN EXISTS (SELECT FROM child_foster_parent WHERE id = adult.id) THEN 'LOCAL_FOSTER_PARENT'
            ELSE 'LOCAL_ADULT'
        END AS role
    FROM same_household_adult adult

    UNION ALL

    -- siblings in the same household
    SELECT sibling.child_id AS id, 'LOCAL_SIBLING' AS role
    FROM head_of_child hoc
    JOIN fridge_child sibling ON hoc.id = sibling.head_of_child
    WHERE sibling.child_id != ${bind(childId)}
    AND daterange(sibling.start_date, sibling.end_date, '[]') @> ${bind(today)}

    UNION ALL

    -- guardians in other households
    SELECT id, 'REMOTE_GUARDIAN' AS role
    FROM (SELECT id FROM child_guardian EXCEPT ALL (SELECT id FROM same_household_adult)) adult

    UNION ALL

    -- foster parents in other households
    SELECT id, 'REMOTE_FOSTER_PARENT' AS role
    FROM (SELECT id FROM child_foster_parent EXCEPT ALL (SELECT id FROM same_household_adult)) adult
)
SELECT
    contact.id, contact.role,
    first_name, last_name, email, phone, backup_phone, street_address, postal_code, post_office,
    priority
FROM contact
JOIN person p USING (id)
LEFT JOIN family_contact ON family_contact.contact_person_id = contact.id AND family_contact.child_id = ${bind(childId)}
"""
            )
        }
        .toList<FamilyContact>()
        .let(::addDefaultPriorities)
        .sortedWith(compareBy({ it.priority ?: Int.MAX_VALUE }, { it.role.ordinal }))

private val defaultContacts =
    setOf(LOCAL_GUARDIAN, LOCAL_FOSTER_PARENT, REMOTE_GUARDIAN, REMOTE_FOSTER_PARENT)

private fun addDefaultPriorities(contacts: List<FamilyContact>): List<FamilyContact> =
    if (contacts.all { it.priority == null }) {
        contacts.fold(listOf()) { acc, contact ->
            acc +
                if (defaultContacts.contains(contact.role)) {
                    val highestPriority = acc.mapNotNull { it.priority }.maxOrNull() ?: 0
                    contact.copy(priority = highestPriority + 1)
                } else {
                    contact
                }
        }
    } else {
        contacts
    }
