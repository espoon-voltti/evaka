// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.shared.config.defaultJsonMapper
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import org.springframework.core.io.ClassPathResource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class MockPersonDetailsService : IPersonDetailsService {

    companion object {
        val allPersons: ConcurrentMap<String, VtjPerson> = allPersonsFrom(readPersonsFromFile())

        private fun allPersonsFrom(persons: List<VtjPerson>): ConcurrentMap<String, VtjPerson> {
            val allPersons = ConcurrentHashMap<String, VtjPerson>()
            persons.forEach {
                allPersons[it.socialSecurityNumber] = it
                it.dependants.forEach { dependant ->
                    allPersons[dependant.socialSecurityNumber] = dependant
                }
                it.guardians.forEach { guardian ->
                    allPersons[guardian.socialSecurityNumber] = guardian
                }
            }
            return allPersons
        }

        private fun readPersonsFromFile(): List<VtjPerson> {
            val content = ClassPathResource("mock-vtj-data.json").inputStream.use { it.bufferedReader().readText() }
            return defaultJsonMapper().readValue(content)
        }

        fun upsertPerson(person: VtjPerson) {
            allPersonsFrom(listOf(person)).values.forEach { newPerson ->
                allPersons[newPerson.socialSecurityNumber] = newPerson
            }
        }

        fun addPerson(person: VtjPerson) {
            allPersons[person.socialSecurityNumber] = person
        }

        fun getPerson(ssn: String) = allPersons[ssn]

        fun deletePerson(ssn: String) = allPersons.remove(ssn)
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
        return allPersons[ssn] ?: error("Mock VTJ person with $ssn not found")
    }
}
