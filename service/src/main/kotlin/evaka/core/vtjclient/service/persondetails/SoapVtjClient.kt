// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.persondetails

import evaka.core.shared.domain.NotFound
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.dto.VtjPersonWithRelatives
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.VtjClient.DetailsQuery
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLLETTAVA_HUOLTAJAT
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.PERUSSANOMA3
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.VTJQuery
import evaka.core.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo

class SoapVtjClient(
    private val vtjSoapTransport: VtjSoapTransport,
    private val henkiloMapper: VtjHenkiloMapper,
) : VtjClient {
    override fun fetchWithDependants(query: DetailsQuery): VtjPersonWithRelatives {
        val henkilo = doVtjQuery(query, HUOLTAJA_HUOLLETTAVA)
        return VtjPersonWithRelatives(
            person = henkiloMapper.mapToVtjPerson(henkilo),
            relatives = henkiloMapper.mapDependants(henkilo),
        )
    }

    override fun fetchWithGuardians(query: DetailsQuery): VtjPersonWithRelatives {
        val henkilo = doVtjQuery(query, HUOLLETTAVA_HUOLTAJAT)
        return VtjPersonWithRelatives(
            person = henkiloMapper.mapToVtjPerson(henkilo),
            relatives = henkiloMapper.mapGuardians(henkilo),
        )
    }

    override fun fetchPerson(query: DetailsQuery): VtjPerson =
        henkiloMapper.mapToVtjPerson(doVtjQuery(query, PERUSSANOMA3))

    private fun doVtjQuery(query: DetailsQuery, requestType: RequestType): Henkilo =
        vtjSoapTransport.query(query.mapToVtjQuery(requestType))
            ?: throw NotFound("VTJ person not found")
}

fun DetailsQuery.mapToVtjQuery(type: RequestType) =
    VTJQuery(requestingUserId = requestingUser.raw, ssn = ssn.value, type = type)
