// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.db.Database

fun Database.Read.isBlacklistSourceUpToDate(source: PasswordBlacklistSource): Boolean =
    createQuery {
            sql(
                """
SELECT EXISTS (
    SELECT FROM password_blacklist_source
    WHERE name = ${bind(source.name)}
    AND imported_at = ${bind(source.updatedAt)}
)
"""
            )
        }
        .exactlyOne()

fun Database.Transaction.upsertPasswordBlacklist(
    source: PasswordBlacklistSource,
    passwords: Sequence<String>,
) {
    val sourceId: Int =
        createUpdate {
                sql(
                    """
INSERT INTO password_blacklist_source (name, imported_at)
VALUES (${bind(source.name)}, ${bind(source.updatedAt)})
ON CONFLICT (name) DO UPDATE
    SET imported_at = excluded.imported_at
RETURNING id
"""
                )
            }
            .executeAndReturnGeneratedKeys()
            .exactlyOne()
    executeBatch(passwords) {
        sql(
            """
INSERT INTO password_blacklist (password, source)
VALUES (${bind { it }}, ${bind(sourceId)})
ON CONFLICT DO NOTHING
"""
        )
    }
}

fun Database.Read.isPasswordBlacklisted(password: Sensitive<String>): Boolean =
    createQuery {
            sql(
                "SELECT EXISTS(SELECT FROM password_blacklist WHERE password = ${bind(password.value)})"
            )
        }
        .exactlyOne()
