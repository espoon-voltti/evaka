// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.messaging.message.createPersonMessageAccount
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.db.Database
import java.util.UUID

fun createPerson(tx: Database.Transaction, person: CreatePersonBody): UUID {
    val personId = tx.createPerson(person)
    tx.createPersonMessageAccount(personId)
    return personId
}

fun createEmptyPerson(tx: Database.Transaction): PersonDTO {
    val result = tx.createEmptyPerson()
    tx.createPersonMessageAccount(result.id)
    return result
}

fun createPersonFromVtj(tx: Database.Transaction, person: PersonDTO): PersonDTO {
    val result = tx.createPersonFromVtj(person)
    tx.createPersonMessageAccount(result.id)
    return result
}
