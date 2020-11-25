// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo

fun storeDvvModificationToken(h: Handle, token: String, nextToken: String, ssnsSent: Int, modificationsReceived: Int) {
    h.createUpdate(
        """
INSERT INTO dvv_modification_token (token, next_token, ssns_sent, modifications_received) 
VALUES (:token, :nextToken, :ssnsSent, :modificationsReceived)
        """.trimIndent()
    )
        .bind("token", token)
        .bind("nextToken", nextToken)
        .bind("ssnsSent", ssnsSent)
        .bind("modificationsReceived", modificationsReceived)
        .execute()
}

fun getNextDvvModificationToken(h: Handle): String {
    return h.createQuery(
        """
SELECT next_token 
FROM dvv_modification_token
ORDER BY created DESC
LIMIT 1
        """.trimIndent()
    ).mapTo<String>().one()
}

fun getDvvModificationToken(h: Handle, token: String): DvvModificationToken? {
    return h.createQuery("""SELECT * FROM dvv_modification_token WHERE token = :token""")
        .bind("token", token)
        .mapTo<DvvModificationToken>()
        .one()
}

fun deleteDvvModificationToken(h: Handle, token: String) {
    h.createUpdate("""DELETE FROM dvv_modification_token WHERE token = :token""").bind("token", token).execute()
}

data class DvvModificationToken(
    val token: String,
    val nextToken: String,
    val ssnsSent: Int,
    val modificationsReceived: Int
)
