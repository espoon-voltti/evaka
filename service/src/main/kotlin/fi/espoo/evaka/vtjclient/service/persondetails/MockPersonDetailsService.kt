// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import org.springframework.core.io.ClassPathResource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class MockPersonDetailsService() : IPersonDetailsService {

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
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            return mapper.readValue(content)
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

    override fun getPersonWithDependants(query: IPersonDetailsService.DetailsQuery): PersonDetails {
        return getMockPerson(query.targetIdentifier.ssn, IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA)
    }

    override fun getPersonWithGuardians(query: IPersonDetailsService.DetailsQuery): PersonDetails {
        return getMockPerson(query.targetIdentifier.ssn, IVtjClientService.RequestType.HUOLLETTAVA_HUOLTAJAT)
    }

    override fun getBasicDetailsFor(query: IPersonDetailsService.DetailsQuery): PersonDetails {
        return getMockPerson(query.targetIdentifier.ssn, IVtjClientService.RequestType.PERUSSANOMA3)
    }

    private fun getMockPerson(ssn: String, requestType: IVtjClientService.RequestType): PersonDetails {
        println("MockPersonDetailsService: $ssn-$requestType requested")
        return when (val result = allPersons[ssn]) {
            null -> PersonDetails.PersonNotFound()
            else -> PersonDetails.Result(result)
        }
    }
}
