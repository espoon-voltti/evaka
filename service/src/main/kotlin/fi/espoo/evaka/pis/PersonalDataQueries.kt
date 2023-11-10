//  SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.updatePersonalDetails(personId: PersonId, body: PersonalDataUpdate) {
    createUpdate<DatabaseTable> {
            sql(
                """
                UPDATE person SET
                    preferred_name = ${bind(body.preferredName)},
                    phone = ${bind(body.phone)},
                    backup_phone = ${bind(body.backupPhone)},
                    email = ${bind(body.email)}
                WHERE id = ${bind(personId)}
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Read.getEnabledEmailTypes(personId: PersonId): List<EmailMessageType> {
    return createQuery<DatabaseTable> {
            sql("SELECT enabled_email_types FROM person WHERE id = ${bind(personId)}")
        }
        .exactlyOne<List<EmailMessageType>?>() ?: EmailMessageType.values().toList()
}

fun Database.Transaction.updateEnabledEmailTypes(
    personId: PersonId,
    emailTypes: List<EmailMessageType>
) {
    createUpdate<DatabaseTable> {
            sql(
                "UPDATE person SET enabled_email_types = ${bind(emailTypes)} WHERE id = ${bind(personId)}"
            )
        }
        .updateExactlyOne()
}
