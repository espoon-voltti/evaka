// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.user

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.EncodedPassword
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import org.jdbi.v3.json.Json

fun Database.Transaction.updateLastStrongLogin(clock: EvakaClock, id: PersonId) =
    createUpdate {
            sql(
                """
INSERT INTO citizen_user (id, last_strong_login)
VALUES (${bind(id)}, ${bind(clock.now())})
ON CONFLICT (id) DO UPDATE SET last_strong_login = excluded.last_strong_login
"""
            )
        }
        .updateExactlyOne()

data class CitizenWeakLoginDetails(
    val id: PersonId,
    val username: String,
    @Json val password: EncodedPassword,
)

fun Database.Read.getCitizenWeakLoginDetails(username: String): CitizenWeakLoginDetails? =
    createQuery {
            sql(
                """
SELECT id, username, password
FROM citizen_user
WHERE username = ${bind(username)}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Transaction.updateLastWeakLogin(clock: EvakaClock, id: PersonId) =
    createUpdate {
            sql(
                """
UPDATE citizen_user SET last_weak_login = ${bind(clock.now())}
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.updatePasswordWithoutTimestamp(id: PersonId, password: EncodedPassword) =
    createUpdate {
            sql(
                """
UPDATE citizen_user
SET password = ${bindJson(password)}
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.updateWeakLoginCredentials(
    clock: EvakaClock,
    id: PersonId,
    username: String?, // null = don't update
    password: EncodedPassword?, // null = don't update
) =
    createUpdate {
            sql(
                """
UPDATE citizen_user
SET
    username = coalesce(${bind(username)}, username),
    username_updated_at = coalesce(${bind(clock.now().takeIf { username != null })}, username_updated_at),
    password = coalesce(${bindJson(password)}, password),
    password_updated_at = coalesce(${bind(clock.now().takeIf { password != null })}, password_updated_at)
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()
