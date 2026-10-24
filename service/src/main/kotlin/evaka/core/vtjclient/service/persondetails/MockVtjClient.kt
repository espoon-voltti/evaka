// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.persondetails

import evaka.core.identity.Ssn
import evaka.core.identity.getDobFromSsn
import evaka.core.pis.service.PersonDTO
import evaka.core.shared.dev.DevPerson
import evaka.core.vtjclient.dto.Nationality
import evaka.core.vtjclient.dto.NativeLanguage
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.RestrictedDetails
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.dto.VtjPersonAndDependants
import evaka.core.vtjclient.dto.VtjPersonAndGuardians
import evaka.core.vtjclient.dto.VtjRelative
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

data class MockVtjDataset(
    val persons: List<MockVtjPerson>,
    val guardianDependants: Map<String, List<String>>,
)

data class MockVtjPerson(
    val firstNames: String,
    val lastName: String,
    val socialSecurityNumber: String,
    val restrictedDetails: RestrictedDetails?,
    val address: PersonAddress? = null,
    val nativeLanguage: NativeLanguage? = null,
    val nationalities: List<Nationality> = emptyList(),
    val dateOfDeath: LocalDate? = null,
    val residenceCode: String? = null,
    val municipalityOfResidence: String? = null,
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
            municipalityOfResidence = municipalityOfResidence,
        )

    fun toVtjRelative(): VtjRelative {
        val ssn = checkNotNull(Ssn.parse(socialSecurityNumber)) { "Invalid mock VTJ SSN" }
        return VtjRelative(
            ssn = ssn,
            dateOfBirth = getDobFromSsn(ssn.value),
            firstNames = firstNames,
            lastName = lastName,
        )
    }

    companion object {
        fun from(person: DevPerson): MockVtjPerson =
            MockVtjPerson(
                socialSecurityNumber = person.ssn!!,
                firstNames = person.firstName,
                lastName = person.lastName,
                restrictedDetails =
                    RestrictedDetails(
                        enabled = person.restrictedDetailsEnabled,
                        endDate = person.restrictedDetailsEndDate,
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
                            streetAddressSe = person.streetAddress,
                        )
                    },
                residenceCode = person.residenceCode,
                municipalityOfResidence = person.municipalityOfResidence,
                nativeLanguage = NativeLanguage(languageName = "FI", code = "fi"),
            )

        fun from(dto: PersonDTO): MockVtjPerson =
            MockVtjPerson(
                firstNames = dto.firstName,
                lastName = dto.lastName,
                socialSecurityNumber =
                    checkNotNull(dto.ssn) { "Person ${dto.id} has no SSN" }.value,
                restrictedDetails =
                    RestrictedDetails(dto.restrictedDetailsEnabled, dto.restrictedDetailsEndDate),
                address =
                    PersonAddress(
                        streetAddress = dto.streetAddress,
                        postalCode = dto.postalCode,
                        postOffice = dto.postOffice,
                        streetAddressSe = null,
                        postOfficeSe = null,
                    ),
                nativeLanguage = NativeLanguage(code = dto.language ?: ""),
                nationalities = dto.nationalities.map { Nationality(it, it) },
                dateOfDeath = dto.dateOfDeath,
                residenceCode = dto.residenceCode,
                municipalityOfResidence = dto.municipalityOfResidence,
            )
    }
}

class MockVtjClient : VtjClient {
    companion object {
        private val readCounts: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

        private val persons: ConcurrentHashMap<String, MockVtjPerson> = ConcurrentHashMap()
        private val dependantsOfGuardian: ConcurrentHashMap<String, Set<String>> =
            ConcurrentHashMap()
        private val guardiansOfDependant: ConcurrentHashMap<String, Set<String>> =
            ConcurrentHashMap()

        fun reset() {
            readCounts.clear()
            persons.clear()
            dependantsOfGuardian.clear()
            guardiansOfDependant.clear()
        }

        fun getGuardians(ssn: String): List<VtjRelative> =
            (guardiansOfDependant[ssn] ?: emptySet()).mapNotNull { persons[it]?.toVtjRelative() }

        fun getDependants(ssn: String): List<VtjRelative> =
            (dependantsOfGuardian[ssn] ?: emptySet()).mapNotNull { persons[it]?.toVtjRelative() }

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

        fun addDependants(guardian: String, dependants: Sequence<String>) {
            require(persons.containsKey(guardian)) { "Guardian $guardian not found" }
            dependants.forEach { dependant ->
                require(persons.containsKey(dependant)) { "Dependant $dependant not found" }
                dependantsOfGuardian.merge(guardian, linkedSetOf(dependant), Set<String>::plus)
                guardiansOfDependant.merge(dependant, linkedSetOf(guardian), Set<String>::plus)
            }
        }

        fun addDependants(guardian: String, vararg dependants: String) {
            addDependants(guardian, dependants.asSequence())
        }

        fun addDependants(guardian: PersonDTO, vararg dependants: PersonDTO) {
            addDependants(guardian.ssn!!.value, dependants.asSequence().map { it.ssn!!.value })
        }

        fun addDependants(guardian: DevPerson, vararg dependants: DevPerson) {
            addDependants(guardian.ssn!!, dependants.asSequence().map { it.ssn!! })
        }
    }

    override fun fetchWithDependants(query: VtjClient.DetailsQuery): VtjPersonAndDependants =
        VtjPersonAndDependants(fetchPerson(query), getDependants(query.ssn.value))

    override fun fetchWithGuardians(query: VtjClient.DetailsQuery): VtjPersonAndGuardians =
        VtjPersonAndGuardians(fetchPerson(query), getGuardians(query.ssn.value))

    override fun fetchPerson(query: VtjClient.DetailsQuery): VtjPerson {
        val ssn = query.ssn.value
        readCounts.merge(ssn, 1, Int::plus)
        return persons[ssn]?.toVtjPerson() ?: error("Mock VTJ person with ${query.ssn} not found")
    }
}
