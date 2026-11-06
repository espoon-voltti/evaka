// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.shared.EvakaUserId
import evaka.core.vtj.VtjSoapTransport.RequestType
import evaka.core.vtj.VtjSoapTransport.RequestType.HUOLLETTAVA_HUOLTAJAT
import evaka.core.vtj.VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA
import evaka.core.vtj.VtjSoapTransport.RequestType.PERUSSANOMA3
import evaka.core.vtj.VtjSoapTransport.VTJQuery
import evaka.core.vtj.soap.VTJHenkiloVastaussanoma.Henkilo

class SoapVtjClient(private val vtjSoapTransport: VtjSoapTransport) : VtjClient {
    override fun fetchPersons(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        query(requestingUser, ssns, PERUSSANOMA3) { it.mapToVtjPerson() }

    override fun fetchWithGuardians(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        query(requestingUser, ssns, HUOLLETTAVA_HUOLTAJAT) {
            VtjPersonAndGuardians(person = it.mapToVtjPerson(), guardians = it.mapGuardians())
        }

    override fun fetchWithDependants(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        query(requestingUser, ssns, HUOLTAJA_HUOLLETTAVA) {
            VtjPersonAndDependants(person = it.mapToVtjPerson(), dependants = it.mapDependants())
        }

    private fun <T> query(
        requestingUser: EvakaUserId,
        ssns: Collection<Ssn>,
        type: RequestType,
        map: (Henkilo) -> T,
    ): Map<Ssn, T> =
        ssns
            .distinct()
            .mapNotNull { ssn ->
                vtjSoapTransport
                    .query(
                        VTJQuery(
                            requestingUserId = requestingUser.raw,
                            ssn = ssn.value,
                            type = type,
                        )
                    )
                    ?.let { ssn to map(it) }
            }
            .toMap()
}
