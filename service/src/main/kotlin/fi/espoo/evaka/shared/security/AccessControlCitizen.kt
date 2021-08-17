// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AccessControlCitizen(private val jdbi: Jdbi) {
    fun getPermittedFeatures(user: AuthenticatedUser): CitizenFeatures {
        return Database(jdbi).read {
            val hasAccessToMessaging = it.citizenHasAccessToMessaging(user.id)
            CitizenFeatures(
                applications = true,
                decisions = true,
                messages = hasAccessToMessaging
            )
        }
    }

    private fun Database.Read.citizenHasAccessToMessaging(personId: UUID): Boolean {
        // language=sql
        val sql = """
WITH placement_ids AS (
    SELECT pl.unit_id
    FROM guardian g
    JOIN placement pl
    ON g.child_id = pl.child_id
    WHERE guardian_id = :personId
    AND Daterange(pl.start_date, pl.end_date, '[]') @> current_date
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
    AND Daterange(pl.start_date, pl.end_date, '[]') @> current_date
    AND Daterange(fg.start_date, fg.end_date, '[]') @> current_date
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
    FROM placement_ids
    JOIN daycare
    ON placement_ids.unit_id = daycare.id
    WHERE 'MESSAGING' = ANY(enabled_pilot_features)
)
        """.trimIndent()
        return createQuery(sql).bind("personId", personId).mapTo<Boolean>().first()
    }
}
