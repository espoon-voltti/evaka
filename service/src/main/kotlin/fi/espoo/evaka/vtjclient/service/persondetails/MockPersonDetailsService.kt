// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import org.springframework.core.io.ClassPathResource

class MockPersonDetailsService : IPersonDetailsService {

    companion object {
        val allPersons: ConcurrentMap<String, VtjPerson> = allPersonsFrom(readPersonsFromFile())
        var readCounts: ConcurrentMap<String, Int> = ConcurrentHashMap<String, Int>()

        private fun allPersonsFrom(persons: List<VtjPerson>): ConcurrentMap<String, VtjPerson> {
            val allPersons = ConcurrentHashMap<String, VtjPerson>()
            persons.forEach {
                allPersons[it.socialSecurityNumber] = it
                it.dependants.forEach { dependant ->
                    if (dependant.guardians.isEmpty()) {
                        allPersons[dependant.socialSecurityNumber] =
                            dependant.copy(guardians = listOf(it))
                    } else if (
                        dependant.guardians.none { g ->
                            g.socialSecurityNumber == it.socialSecurityNumber
                        }
                    ) {
                        allPersons[dependant.socialSecurityNumber] =
                            dependant.copy(guardians = dependant.guardians + it)
                    } else {
                        allPersons[dependant.socialSecurityNumber] = dependant
                    }
                }
                it.guardians.forEach { guardian ->
                    allPersons[guardian.socialSecurityNumber] = guardian
                }
            }
            return allPersons
        }

        private fun readPersonsFromFile(): List<VtjPerson> {
            val content =
                ClassPathResource("mock-vtj-data.json").inputStream.use {
                    it.bufferedReader().readText()
                }
            return defaultJsonMapperBuilder().build().readValue(content)
        }

        fun upsertPerson(person: VtjPerson) {
            allPersonsFrom(listOf(person)).values.forEach { newPerson ->
                allPersons[newPerson.socialSecurityNumber] = newPerson
            }
        }

        fun addPerson(person: VtjPerson) {
            allPersons[person.socialSecurityNumber] = person
        }

        fun addPerson(person: DevPerson) {
            MockPersonDetailsService.addPerson(
                VtjPerson(
                    socialSecurityNumber = person.ssn!!,
                    firstNames = person.firstName,
                    lastName = person.lastName,
                    address =
                        if (person.streetAddress.isNullOrBlank()) {
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
                    nativeLanguage = NativeLanguage(languageName = "FI", code = "fi"),
                    restrictedDetails =
                        RestrictedDetails(
                            enabled = person.restrictedDetailsEnabled,
                            endDate = person.restrictedDetailsEndDate
                        ),
                    guardians = person.guardians.map(::asVtjPerson),
                    dependants = person.dependants.map(::asVtjPerson)
                )
            )
        }

        fun getPerson(ssn: String) = allPersons[ssn]

        fun deletePerson(ssn: String) = allPersons.remove(ssn)

        fun getReadCount(ssn: String) = readCounts.get(ssn) ?: 0

        fun resetReadCounts() {
            readCounts = ConcurrentHashMap<String, Int>()
        }

        private fun asVtjPerson(person: DevPerson): VtjPerson =
            VtjPerson(
                socialSecurityNumber = person.ssn!!,
                firstNames = person.firstName,
                lastName = person.lastName,
                nativeLanguage = NativeLanguage(languageName = "FI", code = "fi"),
                restrictedDetails =
                    RestrictedDetails(
                        enabled = person.restrictedDetailsEnabled,
                        endDate = person.restrictedDetailsEndDate
                    )
            )
    }

    override fun getPersonWithDependants(query: IPersonDetailsService.DetailsQuery): VtjPerson {
        return getMockPerson(query.targetIdentifier.ssn)
    }

    override fun getPersonWithGuardians(query: IPersonDetailsService.DetailsQuery): VtjPerson {
        return getMockPerson(query.targetIdentifier.ssn)
    }

    override fun getBasicDetailsFor(query: IPersonDetailsService.DetailsQuery): VtjPerson {
        return getMockPerson(query.targetIdentifier.ssn)
    }

    private fun getMockPerson(ssn: String): VtjPerson {
        readCounts.compute(ssn) { _, value -> value?.plus(1) ?: 1 }
        return allPersons[ssn] ?: error("Mock VTJ person with $ssn not found")
    }
}
