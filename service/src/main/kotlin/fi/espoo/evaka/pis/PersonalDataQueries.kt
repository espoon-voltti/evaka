//  SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.updatePersonalDetails(personId: PersonId, body: PersonalDataUpdate) {
    createUpdate {
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

fun Database.Read.getDisabledEmailTypes(personId: PersonId): Set<EmailMessageType> {
    return createQuery {
            sql("SELECT disabled_email_types FROM person WHERE id = ${bind(personId)}")
        }
        .exactlyOne<Set<EmailMessageType>>()
}

fun Database.Transaction.updateDisabledEmailTypes(
    personId: PersonId,
    emailTypes: Set<EmailMessageType>,
) {
    createUpdate {
            sql(
                "UPDATE person SET disabled_email_types = ${bind(emailTypes)} WHERE id = ${bind(personId)}"
            )
        }
        .updateExactlyOne()
}
