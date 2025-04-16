// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.storeSfiGetEventsContinuationToken(continuationToken: String) {
    createUpdate {
            sql(
                """
INSERT INTO sfi_get_events_continuation_token (continuation_token)
VALUES (${bind(continuationToken)})
"""
            )
        }
        .execute()
}

fun Database.Read.getLatestSfiGetEventsContinuationToken(): String? =
    createQuery {
            sql(
                """
SELECT continuation_token, created_at
FROM sfi_get_events_continuation_token
ORDER BY created_at DESC limit 1;
"""
            )
        }
        .mapTo<String>()
        .exactlyOneOrNull()
