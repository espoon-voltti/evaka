// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.shared.PersonId
import evaka.core.shared.db.Database

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

fun Database.Read.getNextDvvModificationToken(): String? {
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
        .exactlyOneOrNull<String>()
}

fun Database.Read.getDvvModificationToken(token: String): DvvModificationToken {
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

fun Database.Read.getSsnsOfPersons(ids: Collection<PersonId>): List<String> = createQuery {
    sql(
        """
SELECT social_security_number
FROM person
WHERE id = ANY (${bind(ids)}) AND social_security_number IS NOT NULL
"""
    )
}
    .toList()

data class DvvModificationToken(
    val token: String,
    val nextToken: String,
    val ssnsSent: Int,
    val modificationsReceived: Int,
)
