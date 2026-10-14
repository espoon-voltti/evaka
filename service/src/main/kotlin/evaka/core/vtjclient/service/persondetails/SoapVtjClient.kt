// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.persondetails

import evaka.core.identity.Ssn
import evaka.core.shared.domain.NotFound
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.VtjClient.DetailsQuery
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLLETTAVA_HUOLTAJAT
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.PERUSSANOMA3
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.VTJQuery

class SoapVtjClient(
    private val vtjSoapTransport: VtjSoapTransport,
    private val henkiloMapper: VtjHenkiloMapper,
) : VtjClient {
    /*
       Fetches person with dependants from VTJ, then for each dependant, fetches
       the dependant basic details from VTJ.
    */
    override fun fetchWithDependants(query: DetailsQuery): VtjPerson {
        val guardianResult = doVtjQuery(query, HUOLTAJA_HUOLLETTAVA)

        val dependants =
            guardianResult.dependants
                .map {
                    DetailsQuery(
                        requestingUser = query.requestingUser,
                        ssn =
                            Ssn.parse(it.socialSecurityNumber)
                                ?: error("VTJ returned an invalid SSN"),
                    )
                }
                .map { doVtjQuery(it, PERUSSANOMA3) }

        return guardianResult.copy(dependants = dependants)
    }

    /*
        Fetches person with guardians from VTJ, then for each guardian, fetches
        the guardian basic details from VTJ.
    */
    override fun fetchWithGuardians(query: DetailsQuery): VtjPerson {
        val childResult = doVtjQuery(query, HUOLLETTAVA_HUOLTAJAT)

        val guardians =
            childResult.guardians
                .map {
                    DetailsQuery(
                        requestingUser = query.requestingUser,
                        ssn =
                            Ssn.parse(it.socialSecurityNumber)
                                ?: error("VTJ returned an invalid SSN"),
                    )
                }
                .map { doVtjQuery(it, PERUSSANOMA3) }

        return childResult.copy(guardians = guardians)
    }

    override fun fetchPerson(query: DetailsQuery): VtjPerson {
        return doVtjQuery(query, PERUSSANOMA3)
    }

    private fun doVtjQuery(query: DetailsQuery, requestType: RequestType): VtjPerson {
        return doVtjQuery(query.mapToVtjQuery(requestType))
    }

    private fun doVtjQuery(vtjQuery: VTJQuery): VtjPerson {
        return vtjSoapTransport.query(vtjQuery)?.let(henkiloMapper::mapToVtjPerson)
            ?: throw NotFound("VTJ person not found")
    }
}

fun DetailsQuery.mapToVtjQuery(type: RequestType) =
    VTJQuery(requestingUserId = requestingUser.raw, ssn = ssn.value, type = type)
