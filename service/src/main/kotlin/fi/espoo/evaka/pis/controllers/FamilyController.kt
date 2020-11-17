// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.FamilyContact
import fi.espoo.evaka.pis.service.FamilyOverview
import fi.espoo.evaka.pis.service.FamilyOverviewService
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
    fun getFamilyByPerson(db: Database, user: AuthenticatedUser, @PathVariable(value = "id") id: UUID): ResponseEntity<FamilyOverview> {
        Audit.PisFamilyRead.log(targetId = id)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val result = db.read { familyOverviewService.getFamilyByAdult(it, id) }
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    @GetMapping("/contacts")
    fun getFamilyContactSummary(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam(value = "childId", required = true) childId: UUID
    ): ResponseEntity<List<FamilyContact>> {
        Audit.FamilyContactsRead.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(Roles.ADMIN, Roles.STAFF)
        return db
            .read { it.fetchFamilyContacts(childId) }
            .let { ResponseEntity.ok(it) }
    }
}

private fun Database.Read.fetchFamilyContacts(childId: UUID): List<FamilyContact> {
    // language=sql
    val sql =
        """
        -- adults in the same household
        SELECT 
            CASE
                WHEN EXISTS (SELECT 1 FROM guardian g WHERE g.guardian_id = p.id AND g.child_id = :id)
                THEN 'LOCAL_GUARDIAN' ELSE 'LOCAL_ADULT'
            END AS role, p.first_name, p.last_name, p.email, p.phone, p.street_address, p.postal_code, p.post_office
        FROM person p
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
        SELECT 'LOCAL_SIBLING' AS role, p.first_name, p.last_name, NULL AS email, NULL AS phone, p.street_address, p.postal_code, p.post_office
        FROM person p
        WHERE EXISTS (
            SELECT 1 FROM fridge_child fc1
            WHERE fc1.child_id = :id AND daterange(fc1.start_date, fc1.end_date, '[]') @> now()::date AND EXISTS(
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
        SELECT 'REMOTE_GUARDIAN' AS role, p.first_name, p.last_name, p.email, p.phone, p.street_address, p.postal_code, p.post_office
        FROM person p
        WHERE 
            EXISTS (SELECT 1 FROM guardian g WHERE g.guardian_id = p.id AND g.child_id = :id) -- is a guardian
            AND NOT EXISTS ( -- but is neither head of child nor their partner
                SELECT 1 FROM fridge_child fc
                WHERE fc.child_id = :id AND daterange(fc.start_date, fc.end_date, '[]') @> now()::date AND (
                    fc.head_of_child = p.id OR EXISTS(
                        SELECT 1 FROM fridge_partner_view fp
                        WHERE 
                            fp.person_id = fc.head_of_child 
                            AND fp.partner_person_id = p.id 
                            AND daterange(fp.start_date, fp.end_date, '[]') @> now()::date
                    )
                )
            )
    """

    return createQuery(sql)
        .bind("id", childId)
        .mapTo(FamilyContact::class.java)
        .list()
}
