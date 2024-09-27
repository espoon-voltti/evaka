// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.storeDvvModificationToken(
    token: String,
    nextToken: String,
    ssnsSent: Int,
    modificationsReceived: Int,
) {
    createUpdate {
            sql(
                """
INSERT INTO dvv_modification_token (token, next_token, ssns_sent, modifications_received) 
VALUES (${bind(token)}, ${bind(nextToken)}, ${bind(ssnsSent)}, ${bind(modificationsReceived)})
"""
            )
        }
        .execute()
}

fun Database.Read.getNextDvvModificationToken(): String {
    return createQuery {
            sql(
                """
SELECT next_token
FROM dvv_modification_token
ORDER BY created DESC
LIMIT 1
"""
            )
        }
        .exactlyOne<String>()
}

fun Database.Read.getDvvModificationToken(token: String): DvvModificationToken? {
    return createQuery {
            sql(
                """
SELECT token, next_token, ssns_sent, modifications_received
FROM dvv_modification_token
WHERE token = ${bind(token)}
"""
            )
        }
        .exactlyOne<DvvModificationToken>()
}

fun Database.Transaction.deleteDvvModificationToken(token: String) {
    createUpdate { sql("DELETE FROM dvv_modification_token WHERE token = ${bind(token)}") }
        .execute()
}

fun Database.Read.getPersonIdsBySsns(ssns: List<String>): List<PersonId> {
    return createQuery {
            sql(
                """
SELECT id 
FROM person
WHERE social_security_number = ANY (${bind(ssns)})
"""
            )
        }
        .toList<PersonId>()
}

data class DvvModificationToken(
    val token: String,
    val nextToken: String,
    val ssnsSent: Int,
    val modificationsReceived: Int,
)
