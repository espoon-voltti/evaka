// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.messaging.createPersonMessageAccount
import evaka.core.pis.controllers.CreatePersonBody
import evaka.core.pis.service.PersonDTO
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database

fun createPerson(tx: Database.Transaction, person: CreatePersonBody): PersonId {
    val personId = tx.createPerson(person)
    tx.createPersonMessageAccount(personId)
    return personId
}

fun createPersonFromVtj(tx: Database.Transaction, person: PersonDTO): PersonDTO {
    val result = tx.createPersonFromVtj(person)
    tx.createPersonMessageAccount(result.id)
    return result
}
