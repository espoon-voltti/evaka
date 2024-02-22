// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.storeDvvModificationToken(
    token: String,
    nextToken: String,
    ssnsSent: Int,
    modificationsReceived: Int
) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
INSERT INTO dvv_modification_token (token, next_token, ssns_sent, modifications_received) 
VALUES (:token, :nextToken, :ssnsSent, :modificationsReceived)
        """
                .trimIndent()
        )
        .bind("token", token)
        .bind("nextToken", nextToken)
        .bind("ssnsSent", ssnsSent)
        .bind("modificationsReceived", modificationsReceived)
        .execute()
}

fun Database.Read.getNextDvvModificationToken(): String {
    @Suppress("DEPRECATION")
    return createQuery(
            """
SELECT next_token
FROM dvv_modification_token
ORDER BY created DESC
LIMIT 1
        """
                .trimIndent()
        )
        .exactlyOne<String>()
}

fun Database.Read.getDvvModificationToken(token: String): DvvModificationToken? {
    @Suppress("DEPRECATION")
    return createQuery(
            """
SELECT token, next_token, ssns_sent, modifications_received
FROM dvv_modification_token
WHERE token = :token
        """
                .trimIndent()
        )
        .bind("token", token)
        .exactlyOne<DvvModificationToken>()
}

fun Database.Transaction.deleteDvvModificationToken(token: String) {
    @Suppress("DEPRECATION")
    createUpdate("""DELETE FROM dvv_modification_token WHERE token = :token""")
        .bind("token", token)
        .execute()
}

data class DvvModificationToken(
    val token: String,
    val nextToken: String,
    val ssnsSent: Int,
    val modificationsReceived: Int
)
