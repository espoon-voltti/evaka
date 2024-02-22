// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.CitizenCalendarEnv
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import org.springframework.stereotype.Service

@Service
class AccessControlCitizen(val citizenCalendarEnv: CitizenCalendarEnv) {
    fun getPermittedFeatures(
        tx: Database.Read,
        clock: EvakaClock,
        citizen: PersonId
    ): CitizenFeatures {
        val messaging = tx.citizenHasAccessToMessaging(clock, citizen)
        return CitizenFeatures(
            messages = messaging,
            composeNewMessage = messaging && tx.citizenHasChildWithActivePlacement(clock, citizen),
            reservations =
                tx.citizenHasAccessToReservations(
                    clock,
                    citizen,
                    citizenCalendarEnv.calendarOpenBeforePlacementDays
                ),
            childDocumentation = tx.citizenHasAccessToChildDocumentation(clock, citizen)
        )
    }

    private fun Database.Read.citizenHasAccessToMessaging(
        clock: EvakaClock,
        userId: PersonId
    ): Boolean {
        // language=sql
        val sql =
            """
WITH children AS (
    SELECT child_id, guardian_id AS parent_id FROM guardian WHERE guardian_id = :userId
    UNION ALL
    SELECT child_id, parent_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
)
SELECT EXISTS (
    SELECT 1
    FROM children c
    JOIN placement pl ON c.child_id = pl.child_id
    JOIN daycare u ON pl.unit_id = u.id
    WHERE daterange(pl.start_date, pl.end_date, '[]') @> :today
    AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    AND NOT EXISTS (
        SELECT 1
        FROM messaging_blocklist
        WHERE child_id = c.child_id
        AND blocked_recipient = c.parent_id
    )
    UNION 
    SELECT 1
    FROM person p 
    JOIN message_account ma ON p.id = ma.person_id
    JOIN message_recipients mr ON ma.id = mr.recipient_id
    JOIN message m ON mr.message_id = m.id
    JOIN application app ON p.id = app.guardian_id
    WHERE app.status = 'SENT' AND p.id = :userId AND mr.id IS NOT NULL AND m.sent_at IS NOT NULL
)
"""
        @Suppress("DEPRECATION")
        return createQuery(sql)
            .bind("today", clock.today())
            .bind("userId", userId)
            .exactlyOne<Boolean>()
    }

    private fun Database.Read.citizenHasChildWithActivePlacement(
        clock: EvakaClock,
        userId: PersonId
    ): Boolean {
        // language=sql
        val sql =
            """
WITH children AS (
    SELECT child_id, guardian_id AS parent_id FROM guardian WHERE guardian_id = :userId
    UNION ALL
    SELECT child_id, parent_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
)
SELECT EXISTS (
    SELECT 1
    FROM children c
    JOIN placement pl ON c.child_id = pl.child_id
    WHERE daterange(pl.start_date, pl.end_date, '[]') @> :today
)
"""
        @Suppress("DEPRECATION")
        return createQuery(sql)
            .bind("today", clock.today())
            .bind("userId", userId)
            .exactlyOne<Boolean>()
    }

    private fun Database.Read.citizenHasAccessToReservations(
        clock: EvakaClock,
        userId: PersonId,
        calendarOpenBeforePlacementDays: Int = 0
    ): Boolean {
        // language=sql
        val sql =
            """
WITH children AS (
    SELECT child_id, guardian_id AS parent_id FROM guardian WHERE guardian_id = :userId
    UNION ALL
    SELECT child_id, parent_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
)
SELECT EXISTS (
    SELECT 1
    FROM children c
    JOIN placement pl ON c.child_id = pl.child_id AND daterange((pl.start_date - :calendarOpenBeforePlacementDays), pl.end_date, '[]') @> :today
    JOIN daycare ON pl.unit_id = daycare.id
    WHERE 'RESERVATIONS' = ANY(enabled_pilot_features)
)
"""
        @Suppress("DEPRECATION")
        return createQuery(sql)
            .bind("today", clock.today())
            .bind("userId", userId)
            .bind("calendarOpenBeforePlacementDays", calendarOpenBeforePlacementDays)
            .exactlyOne<Boolean>()
    }

    private fun Database.Read.citizenHasAccessToChildDocumentation(
        clock: EvakaClock,
        userId: PersonId
    ): Boolean {
        // language=sql
        val sql =
            """
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
        @Suppress("DEPRECATION")
        return createQuery(sql)
            .bind("today", clock.today())
            .bind("userId", userId)
            .exactlyOne<Boolean>()
    }
}
