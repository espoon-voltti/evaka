// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children.consent

import fi.espoo.evaka.shared.ChildConsentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapRow
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Read.getChildConsentsByChild(childId: ChildId): List<ChildConsent> =
    this.createQuery(
        """
SELECT cc.id, cc.child_id, cc.type, cc.given, cc.given_at,
       (p.first_name || ' ' || p.last_name) given_by_guardian,
       (e.first_name || ' ' || e.last_name) given_by_employee
FROM child_consent cc
LEFT JOIN person p ON p.id = cc.given_by_guardian
LEFT JOIN employee e ON e.id = cc.given_by_employee
WHERE child_id = :childId
        """.trimIndent()
    )
        .bind("childId", childId)
        .mapTo<ChildConsent>()
        .list()

fun Database.Read.getCitizenChildConsentsForGuardian(guardianId: PersonId): Map<ChildId, List<CitizenChildConsent>> =
    this.createQuery(
        """
SELECT g.child_id, cc.type, cc.given
FROM guardian g
LEFT JOIN child_consent cc ON cc.child_id = g.child_id
WHERE g.guardian_id = :guardianId
        """.trimIndent()
    )
        .bind("guardianId", guardianId)
        .map { row -> Pair(row.mapColumn<ChildId>("child_id"), row.mapRow<CitizenChildConsent?>()) }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.filterNotNull() }

fun Database.Transaction.upsertChildConsentEmployee(
    childId: ChildId,
    type: ChildConsentType,
    given: Boolean,
    givenBy: EmployeeId,
    givenAt: HelsinkiDateTime
) {
    this.createUpdate(
        """
INSERT INTO child_consent (child_id, type, given, given_by_employee, given_at)
VALUES (:childId, :type, :given, :givenBy, :givenAt)
ON CONFLICT (child_id, type)
DO UPDATE SET given = :given, given_by_guardian = NULL, given_by_employee = :givenBy, given_at = now()
        """.trimIndent()
    )
        .bind("childId", childId)
        .bind("type", type)
        .bind("given", given)
        .bind("givenBy", givenBy)
        .bind("givenAt", givenAt)
        .updateExactlyOne()
}

fun Database.Transaction.deleteChildConsentEmployee(
    childId: ChildId,
    type: ChildConsentType
) {
    this.createUpdate(
        """
DELETE FROM child_consent
WHERE child_id = :childId AND type = :type
        """.trimIndent()
    )
        .bind("childId", childId)
        .bind("type", type)
        .updateExactlyOne()
}

fun Database.Transaction.insertChildConsentCitizen(
    childId: ChildId,
    type: ChildConsentType,
    given: Boolean,
    givenBy: PersonId
): Boolean =
    this.createQuery(
        """
INSERT INTO child_consent (child_id, type, given, given_by_guardian, given_at)
VALUES (:childId, :type, :given, :givenBy, now())
ON CONFLICT DO NOTHING
RETURNING id
        """.trimIndent()
    )
        .bind("childId", childId)
        .bind("type", type)
        .bind("given", given)
        .bind("givenBy", givenBy)
        .mapTo<ChildConsentId>()
        .firstOrNull() != null
