package fi.espoo.evaka.pis

import fi.espoo.evaka.messaging.message.createMessageAccountForPerson
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.shared.db.Database
import java.util.UUID

fun createPerson(tx: Database.Transaction, person: PersonIdentityRequest): PersonDTO {
    val result = tx.createPerson(person)
    tx.createMessageAccountForPerson(result.id)
    return result
}

fun createPerson(tx: Database.Transaction, person: CreatePersonBody): UUID {
    val personId = tx.createPerson(person)
    tx.createMessageAccountForPerson(personId)
    return personId
}

fun createEmptyPerson(tx: Database.Transaction): PersonDTO {
    val result = tx.createEmptyPerson()
    tx.createMessageAccountForPerson(result.id)
    return result
}

fun createPersonFromVtj(tx: Database.Transaction, person: PersonDTO): PersonDTO {
    val result = tx.createPersonFromVtj(person)
    tx.createMessageAccountForPerson(result.id)
    return result
}
