// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.VolttiIdentifier
import fi.espoo.evaka.pis.addSSNToPerson
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.createEmptyPerson
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.getDeceasedPeople
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.searchPeople
import fi.espoo.evaka.pis.service.ContactInfo
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.pis.updatePersonContactInfo
import fi.espoo.evaka.pis.updatePersonDetails
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.shared.db.withSpringHandle
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Component
@Transactional
class PersonDAO(private val dataSource: DataSource) {

    fun createEmpty(): PersonDTO = withSpringHandle(dataSource) { h -> h.createEmptyPerson() }

    fun getPersonByExternalId(id: ExternalIdentifier.SSN): PersonDTO? = withSpringHandle(dataSource) { h ->
        h.getPersonBySSN(id.ssn)
    }

    fun findBySearchTerms(searchTerms: String, orderBy: String, sortDirection: String): List<PersonDTO> =
        withSpringHandle(dataSource) { h -> h.searchPeople(searchTerms, orderBy, sortDirection) }

    fun getPersonByVolttiId(volttiId: VolttiIdentifier): PersonDTO? = withSpringHandle(dataSource) { h ->
        h.getPersonById(volttiId)
    }

    fun getOrCreatePersonIdentity(person: PersonIdentityRequest): PersonDTO {
        return getPersonByExternalId(person.identity) ?: createPerson(person)
    }

    fun createPersonFromVtj(personDTO: PersonDTO): PersonDTO = withSpringHandle(dataSource) { h ->
        h.createPersonFromVtj(personDTO)
    }

    fun updatePersonFromVtj(personDTO: PersonDTO): PersonDTO = withSpringHandle(dataSource) { h ->
        h.updatePersonFromVtj(personDTO)
    }

    fun updateEndUsersContactInfo(id: VolttiIdentifier, contactInfo: ContactInfo): Boolean =
        withSpringHandle(dataSource) { h -> h.updatePersonContactInfo(id, contactInfo) }

    fun updateEndUserDetails(id: VolttiIdentifier, data: PersonPatch): Boolean = withSpringHandle(dataSource) { h ->
        h.updatePersonDetails(id, data)
    }

    fun createPerson(person: CreatePersonBody): UUID = withSpringHandle(dataSource) { h ->
        h.createPerson(person)
    }

    private fun createPerson(person: PersonIdentityRequest): PersonDTO = withSpringHandle(dataSource) { h ->
        h.createPerson(person)
    }

    fun addSsn(id: VolttiIdentifier, ssn: String) = withSpringHandle(dataSource) { h -> h.addSSNToPerson(id, ssn) }

    fun getDeceased(sinceDate: LocalDate): List<PersonDTO> = withSpringHandle(dataSource) { h ->
        h.getDeceasedPeople(sinceDate)
    }
}
