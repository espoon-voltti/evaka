// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.person.Person
import evaka.core.shared.EvakaUserId
import evaka.core.shared.dev.DevPerson
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
    val residentialAddress: PersonAddress? = null,
    val mailingAddressPeriods: List<MailingAddressPeriod> = emptyList(),
    val nativeLanguage: NativeLanguage? = null,
    val dateOfDeath: LocalDate? = null,
    val residenceCode: String? = null,
    val municipalityOfResidence: String? = null,
) {
    fun toVtjPerson(): VtjPerson =
        VtjPerson(
            firstNames = firstNames,
            lastName = lastName,
            ssn = Ssn.from(socialSecurityNumber),
            restrictedDetails = restrictedDetails,
            residentialAddress = residentialAddress,
            mailingAddressPeriods = mailingAddressPeriods,
            nativeLanguage = nativeLanguage,
            dateOfDeath = dateOfDeath,
            residenceCode = residenceCode,
            municipalityOfResidence = municipalityOfResidence,
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
                        endDate = person.restrictedDetailsEndDate,
                    ),
                residentialAddress =
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

        fun from(person: Person): MockVtjPerson =
            MockVtjPerson(
                firstNames = person.firstName,
                lastName = person.lastName,
                socialSecurityNumber =
                    checkNotNull(person.ssn) { "Person ${person.id} has no SSN" }.value,
                restrictedDetails =
                    RestrictedDetails(
                        person.restrictedDetailsEnabled,
                        person.restrictedDetailsEndDate,
                    ),
                residentialAddress =
                    PersonAddress(
                        streetAddress = person.streetAddress,
                        postalCode = person.postalCode,
                        postOffice = person.postOffice,
                        streetAddressSe = null,
                        postOfficeSe = null,
                    ),
                nativeLanguage = NativeLanguage(code = person.language ?: ""),
                dateOfDeath = person.dateOfDeath,
                residenceCode = person.residenceCode,
                municipalityOfResidence = person.municipalityOfResidence,
            )
    }
}

class MockVtjClient : VtjClient {
    companion object {
        private val readCounts: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
        private val callCounts: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

        private val persons: ConcurrentHashMap<String, MockVtjPerson> = ConcurrentHashMap()
        private val dependantsOfGuardian: ConcurrentHashMap<String, Set<String>> =
            ConcurrentHashMap()
        private val guardiansOfDependant: ConcurrentHashMap<String, Set<String>> =
            ConcurrentHashMap()

        fun reset() {
            readCounts.clear()
            callCounts.clear()
            persons.clear()
            dependantsOfGuardian.clear()
            guardiansOfDependant.clear()
        }

        fun getGuardians(ssn: String): List<Ssn> =
            (guardiansOfDependant[ssn] ?: emptySet())
                .filter { persons.containsKey(it) }
                .map { Ssn.from(it) }

        fun getDependants(ssn: String): List<Ssn> =
            (dependantsOfGuardian[ssn] ?: emptySet())
                .filter { persons.containsKey(it) }
                .map { Ssn.from(it) }

        /** The number of client calls of each function, for example `fetchPersons`. */
        fun getCallCounts(): Map<String, Int> = callCounts.toMap()

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

        fun addPersons(vararg persons: Person) {
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

        fun addDependants(guardian: Person, vararg dependants: Person) {
            addDependants(guardian.ssn!!.value, dependants.asSequence().map { it.ssn!!.value })
        }

        fun addDependants(guardian: DevPerson, vararg dependants: DevPerson) {
            addDependants(guardian.ssn!!, dependants.asSequence().map { it.ssn!! })
        }
    }

    override fun fetchPersons(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch("fetchPersons", ssns) { it }

    override fun fetchWithGuardians(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch("fetchWithGuardians", ssns) { VtjPersonAndGuardians(it, getGuardians(it.ssn.value)) }

    override fun fetchWithDependants(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch("fetchWithDependants", ssns) {
            VtjPersonAndDependants(it, getDependants(it.ssn.value))
        }

    private fun <T> fetch(
        function: String,
        ssns: Collection<Ssn>,
        map: (VtjPerson) -> T,
    ): Map<Ssn, T> {
        if (ssns.isEmpty()) return emptyMap()
        callCounts.merge(function, 1, Int::plus)
        return ssns
            .distinct()
            .mapNotNull { ssn ->
                readCounts.merge(ssn.value, 1, Int::plus)
                persons[ssn.value]?.let { ssn to map(it.toVtjPerson()) }
            }
            .toMap()
    }
}
