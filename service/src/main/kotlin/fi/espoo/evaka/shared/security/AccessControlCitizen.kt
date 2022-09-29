// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.CitizenCalendarEnv
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import org.springframework.stereotype.Service

@Service
class AccessControlCitizen(
    val citizenCalendarEnv: CitizenCalendarEnv
) {
    fun getPermittedFeatures(tx: Database.Read, user: AuthenticatedUser.Citizen, clock: EvakaClock): CitizenFeatures {
        return CitizenFeatures(
            messages = tx.citizenHasAccessToMessaging(clock, user.id),
            reservations = tx.citizenHasAccessToReservations(clock, user.id, citizenCalendarEnv.calendarOpenBeforePlacementDays),
            childDocumentation = tx.citizenHasAccessToChildDocumentation(clock, user.id)
        )
    }

    private fun Database.Read.citizenHasAccessToMessaging(clock: EvakaClock, personId: PersonId): Boolean {
        // language=sql
        val sql = """
WITH child_placements AS (
    SELECT pl.unit_id
    FROM guardian g
    JOIN placement pl
    ON g.child_id = pl.child_id
    WHERE guardian_id = :personId
    AND Daterange(pl.start_date, pl.end_date, '[]') @> :today
    AND NOT EXISTS (
        SELECT 1
        FROM messaging_blocklist
        WHERE child_id = pl.child_id
        AND blocked_recipient = guardian_id
    )

    UNION

    SELECT pl.unit_id
    FROM fridge_child fg
    JOIN placement pl
    ON fg.child_id = pl.child_id
    WHERE head_of_child = :personId
    AND Daterange(pl.start_date, pl.end_date, '[]') @> :today
    AND Daterange(fg.start_date, fg.end_date, '[]') @> :today
    AND fg.conflict = false
    AND NOT EXISTS (
        SELECT 1
        FROM messaging_blocklist
        WHERE child_id = pl.child_id
        AND blocked_recipient = head_of_child
    )

)
SELECT EXISTS (
    SELECT 1
    FROM child_placements
    JOIN daycare
    ON child_placements.unit_id = daycare.id
    WHERE 'MESSAGING' = ANY(enabled_pilot_features)
)
        """.trimIndent()
        return createQuery(sql).bind("today", clock.today()).bind("personId", personId).mapTo<Boolean>().first()
    }

    private fun Database.Read.citizenHasAccessToReservations(clock: EvakaClock, personId: PersonId, calendarOpenBeforePlacementDays: Int = 0): Boolean {
        // language=sql
        val sql = """
SELECT EXISTS (
    SELECT 1
    FROM guardian g
    JOIN placement pl ON g.child_id = pl.child_id AND Daterange((pl.start_date - :calendarOpenBeforePlacementDays), pl.end_date, '[]') @> :today
    JOIN daycare ON pl.unit_id = daycare.id
    WHERE guardian_id = :personId AND 'RESERVATIONS' = ANY(enabled_pilot_features)
)
        """.trimIndent()
        return createQuery(sql)
            .bind("today", clock.today())
            .bind("personId", personId)
            .bind("calendarOpenBeforePlacementDays", calendarOpenBeforePlacementDays)
            .mapTo<Boolean>()
            .first()
    }

    private fun Database.Read.citizenHasAccessToChildDocumentation(clock: EvakaClock, userId: PersonId): Boolean {
        // language=sql
        val sql = """
WITH children AS (
    SELECT child_id, guardian_id AS parent_id FROM guardian WHERE guardian_id = :userId
    UNION ALL
    SELECT child_id, parent_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
)
SELECT EXISTS (
    SELECT 1
    FROM children c
    JOIN placement pl ON c.child_id = pl.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> :today
    JOIN daycare ON pl.unit_id = daycare.id
    WHERE 'VASU_AND_PEDADOC' = ANY(enabled_pilot_features)
)
"""
        return createQuery(sql).bind("today", clock.today()).bind("userId", userId).mapTo<Boolean>().first()
    }
}
