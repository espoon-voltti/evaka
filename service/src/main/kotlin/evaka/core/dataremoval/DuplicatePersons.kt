// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.application.ApplicationStatus
import evaka.core.koski.childSentToKoski
import evaka.core.koski.freezeKoskiSync
import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.varda.childSentToVarda
import java.time.LocalDate
import java.time.Period

/** A duplicate and its original stay linked until neither has been placed for this long */
val DUPLICATE_LINK_PERIOD: Period = Period.ofYears(10)

/**
 * A child and the duplicate that was made of it, so that the child could have two placements at the
 * same time. A run handles neither of them while they are linked. Duplicates can no longer be
 * created, so this only handles legacy data, and it can be removed once no duplicates remain.
 */
data class DuplicateLink(
    val duplicateId: PersonId,
    val originalId: PersonId,
    /** Of either person, or null if neither has been placed */
    val lastPlacementEnd: LocalDate?,
    /** Of the person created later */
    val lastCreated: LocalDate,
    /** Of either person, or null if neither has a pending application */
    val lastPendingApplicationSent: LocalDate?,
    val duplicate: IntegrationFacts,
    val original: IntegrationFacts,
) {
    fun otherThan(personId: PersonId): PersonId =
        if (personId == duplicateId) originalId else duplicateId

    fun mayBeCleared(today: LocalDate): Boolean {
        val inactive =
            if (lastPlacementEnd != null) lastPlacementEnd.plus(DUPLICATE_LINK_PERIOD) < today
            else lastCreated.plusMonths(1) < today
        // An application pending longer is abandoned, as in the rule of the application
        val applicationPending =
            lastPendingApplicationSent != null && lastPendingApplicationSent.plusYears(1) >= today
        // Koski reads the link, so clearing it is treated like deleting the Koski data of both
        return inactive &&
            !applicationPending &&
            listOf(duplicate, original).all { it.safeToDeleteFor(Integration.KOSKI, today) }
    }
}

/** The link of the person to its duplicate or its original. A person may have only one. */
fun Database.Read.getDuplicateLink(personId: PersonId): DuplicateLink? {
    val links = createQuery {
        sql(
            """
SELECT
    d.id AS duplicate_id,
    o.id AS original_id,
    (SELECT max(end_date) FROM placement WHERE child_id IN (d.id, o.id)) AS last_placement_end,
    greatest(d.created, o.created) AS last_created,
    (
        SELECT max(coalesce(sentdate, (created_at AT TIME ZONE 'Europe/Helsinki')::date))
        FROM application
        WHERE child_id IN (d.id, o.id) AND status = ANY(${bind(ApplicationStatus.pending)})
    ) AS last_pending_application_sent,
    d.date_of_birth AS duplicate_date_of_birth,
    ${predicate(childSentToKoski.forTable("d"))} AS duplicate_sent_to_koski,
    ${predicate(childSentToVarda.forTable("d"))} AS duplicate_sent_to_varda,
    o.date_of_birth AS original_date_of_birth,
    ${predicate(childSentToKoski.forTable("o"))} AS original_sent_to_koski,
    ${predicate(childSentToVarda.forTable("o"))} AS original_sent_to_varda
FROM person d
JOIN person o ON o.id = d.duplicate_of
WHERE ${bind(personId)} IN (d.id, o.id)
"""
        )
    }
        .toList {
            DuplicateLink(
                duplicateId = column("duplicate_id"),
                originalId = column("original_id"),
                lastPlacementEnd = column("last_placement_end"),
                lastCreated = column<HelsinkiDateTime>("last_created").toLocalDate(),
                lastPendingApplicationSent = column("last_pending_application_sent"),
                duplicate =
                    IntegrationFacts(
                        dateOfBirth = column("duplicate_date_of_birth"),
                        sentToKoski = column("duplicate_sent_to_koski"),
                        sentToVarda = column("duplicate_sent_to_varda"),
                    ),
                original =
                    IntegrationFacts(
                        dateOfBirth = column("original_date_of_birth"),
                        sentToKoski = column("original_sent_to_koski"),
                        sentToVarda = column("original_sent_to_varda"),
                    ),
            )
        }
    check(links.size <= 1) {
        "Person $personId is linked to several duplicates or originals, so it is refused"
    }
    return links.singleOrNull()
}

/**
 * Koski reads the link, so the sync of each person that has been sent is frozen. The children
 * frozen are returned.
 */
fun Database.Transaction.clearDuplicateLink(
    link: DuplicateLink,
    now: HelsinkiDateTime,
): List<ChildId> {
    createUpdate {
        sql("UPDATE person SET duplicate_of = NULL WHERE id = ${bind(link.duplicateId)}")
    }
        .updateExactlyOne()
    return freezeKoskiSync(listOf(link.duplicateId, link.originalId), now)
}
