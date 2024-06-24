// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

typealias Ssn = String

data class MockVtjDataset(
    val persons: List<MockVtjPerson>,
    val guardianDependants: Map<Ssn, List<Ssn>>
)

data class MockVtjPerson(
    val firstNames: String,
    val lastName: String,
    val socialSecurityNumber: Ssn,
    val restrictedDetails: RestrictedDetails?,
    val address: PersonAddress? = null,
    val nativeLanguage: NativeLanguage? = null,
    val nationalities: List<Nationality> = emptyList(),
    val dateOfDeath: LocalDate? = null,
    val residenceCode: String? = null
) {
    fun toVtjPerson(): VtjPerson =
        VtjPerson(
            firstNames = firstNames,
            lastName = lastName,
            socialSecurityNumber = socialSecurityNumber,
            restrictedDetails = restrictedDetails,
            address = address,
            nativeLanguage = nativeLanguage,
            nationalities = nationalities,
            dateOfDeath = dateOfDeath,
            residenceCode = residenceCode,
            dependants = emptyList(),
            guardians = emptyList()
        )

    companion object {
        fun from(person: DevPerson): MockVtjPerson =
            MockVtjPerson(
                socialSecurityNumber = person.ssn!!,
                firstNames = person.firstName,
                lastName = person.lastName,
                restrictedDetails =
                    RestrictedDetails(
                        enabled = person.restrictedDetailsEnabled,
                        endDate = person.restrictedDetailsEndDate
                    ),
                address =
                    if (person.streetAddress.isBlank()) {
                        null
                    } else {
                        PersonAddress(
                            streetAddress = person.streetAddress,
                            postalCode = person.postalCode,
                            postOffice = person.postOffice,
                            postOfficeSe = person.postOffice,
                            streetAddressSe = person.streetAddress
                        )
                    },
                residenceCode = person.residenceCode,
                nativeLanguage = NativeLanguage(languageName = "FI", code = "fi")
            )

        fun from(dto: PersonDTO): MockVtjPerson =
            MockVtjPerson(
                firstNames = dto.firstName,
                lastName = dto.lastName,
                socialSecurityNumber = dto.identity.toString(),
                restrictedDetails =
                    RestrictedDetails(dto.restrictedDetailsEnabled, dto.restrictedDetailsEndDate),
                address =
                    PersonAddress(
                        streetAddress = dto.streetAddress,
                        postalCode = dto.postalCode,
                        postOffice = dto.postOffice,
                        streetAddressSe = null,
                        postOfficeSe = null
                    ),
                nativeLanguage = NativeLanguage(code = dto.language ?: ""),
                nationalities = dto.nationalities.map { Nationality(it, it) },
                dateOfDeath = dto.dateOfDeath,
                residenceCode = dto.residenceCode
            )
    }
}

class MockPersonDetailsService : IPersonDetailsService {
    companion object {
        private val readCounts: ConcurrentHashMap<Ssn, Int> = ConcurrentHashMap()

        private val persons: ConcurrentHashMap<Ssn, MockVtjPerson> = ConcurrentHashMap()
        private val dependantsOfGuardian: ConcurrentHashMap<Ssn, Set<Ssn>> = ConcurrentHashMap()
        private val guardiansOfDependant: ConcurrentHashMap<Ssn, Set<Ssn>> = ConcurrentHashMap()

        fun reset() {
            readCounts.clear()
            persons.clear()
            dependantsOfGuardian.clear()
            guardiansOfDependant.clear()
        }

        fun getPerson(ssn: Ssn) =
            persons[ssn]
                ?.toVtjPerson()
                ?.copy(guardians = getGuardians(ssn), dependants = getDependants(ssn))

        fun getAllPersons(): List<VtjPerson> =
            persons.values.map {
                it
                    .toVtjPerson()
                    .copy(
                        guardians = getGuardians(it.socialSecurityNumber),
                        dependants = getDependants(it.socialSecurityNumber)
                    )
            }

        fun getGuardians(ssn: Ssn): List<VtjPerson> = (guardiansOfDependant[ssn] ?: emptySet()).mapNotNull { persons[it]?.toVtjPerson() }

        fun getDependants(ssn: Ssn): List<VtjPerson> = (dependantsOfGuardian[ssn] ?: emptySet()).mapNotNull { persons[it]?.toVtjPerson() }

        fun add(dataset: MockVtjDataset) {
            dataset.persons.forEach(::addPerson)
            dataset.guardianDependants.forEach { (guardian, dependants) ->
                addDependants(guardian, dependants.asSequence())
            }
        }

        fun addPerson(person: MockVtjPerson) {
            this.persons[person.socialSecurityNumber] = person
        }

        fun addPersons(vararg persons: MockVtjPerson) {
            persons.forEach(::addPerson)
        }

        fun addPersons(vararg persons: DevPerson) {
            persons.forEach { addPerson(MockVtjPerson.from(it)) }
        }

        fun addPersons(vararg persons: PersonDTO) {
            persons.forEach { addPerson(MockVtjPerson.from(it)) }
        }

        fun addDependants(
            guardian: Ssn,
            dependants: Sequence<Ssn>
        ) {
            require(persons.containsKey(guardian)) { "Guardian $guardian not found" }
            dependants.forEach { dependant ->
                require(persons.containsKey(dependant)) { "Dependant $dependant not found" }
                dependantsOfGuardian.merge(guardian, linkedSetOf(dependant), Set<Ssn>::plus)
                guardiansOfDependant.merge(dependant, linkedSetOf(guardian), Set<Ssn>::plus)
            }
        }

        fun addDependants(
            guardian: Ssn,
            vararg dependants: Ssn
        ) {
            addDependants(guardian, dependants.asSequence())
        }

        fun addDependants(
            guardian: ExternalIdentifier,
            vararg dependants: ExternalIdentifier
        ) {
            addDependants(guardian.toString(), dependants.asSequence().map { it.toString() })
        }

        fun addDependants(
            guardian: DevPerson,
            vararg dependants: DevPerson
        ) {
            addDependants(guardian.ssn!!, dependants.asSequence().map { it.ssn!! })
        }
    }

    override fun getPersonWithDependants(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        getBasicDetailsFor(query).copy(dependants = getDependants(query.targetIdentifier.ssn))

    override fun getPersonWithGuardians(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        getBasicDetailsFor(query).copy(guardians = getGuardians(query.targetIdentifier.ssn))

    override fun getBasicDetailsFor(query: IPersonDetailsService.DetailsQuery): VtjPerson {
        val ssn = query.targetIdentifier.ssn
        readCounts.merge(ssn, 1, Int::plus)
        return persons[query.targetIdentifier.ssn]?.toVtjPerson()
            ?: error("Mock VTJ person with ${query.targetIdentifier.ssn} not found")
    }
}
