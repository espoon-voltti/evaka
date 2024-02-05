// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma
import java.util.concurrent.ConcurrentHashMap

class MockVtjClientService : IVtjClientService {

    companion object {
        private var queryRequestResponse:
            ConcurrentHashMap<
                Pair<String, IVtjClientService.RequestType>,
                VTJHenkiloVastaussanoma.Henkilo
            > =
            ConcurrentHashMap()

        private var queryCounts:
            ConcurrentHashMap<Pair<String, IVtjClientService.RequestType>, Int> =
            ConcurrentHashMap()

        fun resetQueryCounts() {
            this.queryCounts.clear()
            this.queryRequestResponse.clear()
        }

        fun addPERUSSANOMA3RequestExpectation(person: DevPerson) {
            queryRequestResponse[Pair(person.ssn!!, IVtjClientService.RequestType.PERUSSANOMA3)] =
                devPersonToVTJHenkiloVastaussanomaHenkilo(person)
        }

        fun addHUOLTAJAHUOLLETTAVARequestExpectation(person: DevPerson, children: List<DevPerson>) {
            // HUOLTAJA_HUOLLETTAVA request
            queryRequestResponse[
                Pair(person.ssn!!, IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA)] =
                devPersonToVTJHenkiloVastaussanomaHenkilo(person).also { huoltaja ->
                    huoltaja.huollettava.addAll(
                        0,
                        children.map { child ->
                            VTJHenkiloVastaussanoma.Henkilo.Huollettava().also {
                                it.henkilotunnus = child.ssn

                                it.nykyisetEtunimet =
                                    VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet()
                                        .also { it.etunimet = child.firstName }

                                it.nykyinenSukunimi =
                                    VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi()
                                        .also { it.sukunimi = child.lastName }
                            }
                        }
                    )
                }
        }

        fun getPERUSSANOMA3RequestCount(person: DevPerson): Int {
            return queryCounts[Pair(person.ssn!!, IVtjClientService.RequestType.PERUSSANOMA3)] ?: 0
        }

        fun getHUOLTAJAHUOLLETTAVARequestCount(person: DevPerson): Int {
            return queryCounts[
                Pair(person.ssn!!, IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA)] ?: 0
        }

        private fun devPersonToVTJHenkiloVastaussanomaHenkilo(
            person: DevPerson
        ): VTJHenkiloVastaussanoma.Henkilo =
            VTJHenkiloVastaussanoma.Henkilo().also {
                it.henkilotunnus =
                    VTJHenkiloVastaussanoma.Henkilo.Henkilotunnus().also { ht ->
                        ht.value = person.ssn
                    }
                it.vakinainenKotimainenLahiosoite =
                    VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite().also {
                        it.postitoimipaikkaS = person.streetAddress
                        it.postitoimipaikkaS = person.postOffice
                        it.postinumero = person.postalCode
                    }
                it.nykyisetEtunimet =
                    VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet().also {
                        it.etunimet = person.firstName
                    }
                it.nykyinenSukunimi =
                    VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi().also {
                        it.sukunimi = person.lastName
                    }
                it.aidinkieli = VTJHenkiloVastaussanoma.Henkilo.Aidinkieli()
                it.turvakielto = VTJHenkiloVastaussanoma.Henkilo.Turvakielto()
                it.kuolintiedot = VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot()
            }
    }

    override fun query(query: IVtjClientService.VTJQuery): VTJHenkiloVastaussanoma.Henkilo? {
        queryCounts.compute(Pair(query.ssn, query.type)) { _, value -> value?.plus(1) ?: 1 }
        return queryRequestResponse[Pair(query.ssn, query.type)]
    }
}
