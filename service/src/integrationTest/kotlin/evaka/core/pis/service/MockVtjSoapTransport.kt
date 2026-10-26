// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.shared.dev.DevPerson
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport
import evaka.core.vtjclient.soap.VTJHenkiloVastaussanoma
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals

class MockVtjSoapTransport : VtjSoapTransport {

    companion object {
        private var queryRequestResponse:
            ConcurrentHashMap<
                Pair<String, VtjSoapTransport.RequestType>,
                VTJHenkiloVastaussanoma.Henkilo,
            > =
            ConcurrentHashMap()

        private var queryCounts:
            ConcurrentHashMap<Pair<String, VtjSoapTransport.RequestType>, Int> =
            ConcurrentHashMap()

        fun resetQueryCounts() {
            this.queryCounts.clear()
            this.queryRequestResponse.clear()
        }

        fun addPERUSSANOMA3RequestExpectation(person: DevPerson) {
            queryRequestResponse[Pair(person.ssn!!, VtjSoapTransport.RequestType.PERUSSANOMA3)] =
                devPersonToVTJHenkiloVastaussanomaHenkilo(person)
        }

        fun addHUOLTAJAHUOLLETTAVARequestExpectation(person: DevPerson, children: List<DevPerson>) {
            // HUOLTAJA_HUOLLETTAVA request
            queryRequestResponse[
                Pair(person.ssn!!, VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA)] =
                devPersonToVTJHenkiloVastaussanomaHenkilo(person).also { huoltaja ->
                    huoltaja.huollettava.addAll(
                        0,
                        children.map { child ->
                            VTJHenkiloVastaussanoma.Henkilo.Huollettava().also {
                                it.henkilotunnus = child.ssn

                                it.nykyisetEtunimet =
                                    VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet()
                                        .also { e -> e.etunimet = child.firstName }

                                it.nykyinenSukunimi =
                                    VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi()
                                        .also { s -> s.sukunimi = child.lastName }
                            }
                        },
                    )
                }
        }

        fun addHUOLLETTAVAHUOLTAJATRequestExpectation(
            person: DevPerson,
            guardians: List<DevPerson>,
        ) {
            queryRequestResponse[
                Pair(person.ssn!!, VtjSoapTransport.RequestType.HUOLLETTAVA_HUOLTAJAT)] =
                devPersonToVTJHenkiloVastaussanomaHenkilo(person).also { huollettava ->
                    huollettava.huoltaja.addAll(
                        0,
                        guardians.map { guardian ->
                            VTJHenkiloVastaussanoma.Henkilo.Huoltaja().also {
                                it.henkilotunnus = guardian.ssn

                                it.nykyisetEtunimet =
                                    VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyisetEtunimet()
                                        .also { e -> e.etunimet = guardian.firstName }

                                it.nykyinenSukunimi =
                                    VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyinenSukunimi()
                                        .also { s -> s.sukunimi = guardian.lastName }
                            }
                        },
                    )
                }
        }

        fun removeRequestExpectation(person: DevPerson, type: VtjSoapTransport.RequestType) {
            queryRequestResponse.remove(Pair(person.ssn!!, type))
        }

        fun getPERUSSANOMA3RequestCount(person: DevPerson): Int {
            return queryCounts[Pair(person.ssn!!, VtjSoapTransport.RequestType.PERUSSANOMA3)] ?: 0
        }

        fun getHUOLTAJAHUOLLETTAVARequestCount(person: DevPerson): Int {
            return queryCounts[
                Pair(person.ssn!!, VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA)] ?: 0
        }

        /**
         * Asserts that exactly the given requests were made since the last reset. A request that
         * was made N times must be given N times.
         */
        fun assertRequests(vararg requests: Pair<VtjSoapTransport.RequestType, DevPerson>) {
            assertEquals(
                requests.groupingBy { (type, person) -> Pair(person.ssn!!, type) }.eachCount(),
                queryCounts.toMap(),
            )
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
                    VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite().also { o ->
                        o.lahiosoiteS = person.streetAddress
                        o.postitoimipaikkaS = person.postOffice
                        o.postinumero = person.postalCode
                    }
                it.nykyisetEtunimet =
                    VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet().also { e ->
                        e.etunimet = person.firstName
                    }
                it.nykyinenSukunimi =
                    VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi().also { s ->
                        s.sukunimi = person.lastName
                    }
                it.aidinkieli = VTJHenkiloVastaussanoma.Henkilo.Aidinkieli()
                it.turvakielto =
                    VTJHenkiloVastaussanoma.Henkilo.Turvakielto().also { t ->
                        if (person.restrictedDetailsEnabled) t.turvakieltoTieto = "1"
                    }
                it.kuolintiedot =
                    VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot().also { k ->
                        k.kuolinpvm = person.dateOfDeath?.format(DateTimeFormatter.BASIC_ISO_DATE)
                    }
            }
    }

    override fun query(query: VtjSoapTransport.VTJQuery): VTJHenkiloVastaussanoma.Henkilo? {
        queryCounts.compute(Pair(query.ssn, query.type)) { _, value -> value?.plus(1) ?: 1 }
        return queryRequestResponse[Pair(query.ssn, query.type)]
    }
}
