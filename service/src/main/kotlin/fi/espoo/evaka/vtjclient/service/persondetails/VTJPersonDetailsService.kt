// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.mapper.VtjHenkiloMapper
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService.DetailsQuery
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.HUOLLETTAVA_HUOLTAJAT
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.PERUSSANOMA3
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.VTJQuery

class VTJPersonDetailsService(
    private val vtjClientService: IVtjClientService,
    private val henkiloMapper: VtjHenkiloMapper,
) : IPersonDetailsService {
    /*
       Fetches person with dependants from VTJ, then for each dependant, fetches
       the dependant basic details from VTJ.
    */
    override fun getPersonWithDependants(query: DetailsQuery): VtjPerson {
        val guardianResult = doVtjQuery(query, HUOLTAJA_HUOLLETTAVA)

        val dependants =
            guardianResult.dependants
                .map {
                    DetailsQuery(
                        requestingUser = query.requestingUser,
                        targetIdentifier =
                            ExternalIdentifier.SSN.getInstance(it.socialSecurityNumber),
                    )
                }
                .map { doVtjQuery(it, PERUSSANOMA3) }

        return guardianResult.copy(dependants = dependants)
    }

    /*
        Fetches person with guardians from VTJ, then for each guardian, fetches
        the guardian basic details from VTJ.
    */
    override fun getPersonWithGuardians(query: DetailsQuery): VtjPerson {
        val childResult = doVtjQuery(query, HUOLLETTAVA_HUOLTAJAT)

        val guardians =
            childResult.guardians
                .map {
                    DetailsQuery(
                        requestingUser = query.requestingUser,
                        targetIdentifier =
                            ExternalIdentifier.SSN.getInstance(it.socialSecurityNumber),
                    )
                }
                .map { doVtjQuery(it, PERUSSANOMA3) }

        return childResult.copy(guardians = guardians)
    }

    override fun getBasicDetailsFor(query: DetailsQuery): VtjPerson {
        return doVtjQuery(query, PERUSSANOMA3)
    }

    private fun doVtjQuery(query: DetailsQuery, requestType: RequestType): VtjPerson {
        return doVtjQuery(query.mapToVtjQuery(requestType))
    }

    private fun doVtjQuery(vtjQuery: VTJQuery): VtjPerson {
        return vtjClientService.query(vtjQuery)?.let(henkiloMapper::mapToVtjPerson)
            ?: throw NotFound("VTJ person not found")
    }
}

fun DetailsQuery.mapToVtjQuery(type: RequestType) =
    VTJQuery(requestingUserId = requestingUser.raw, ssn = targetIdentifier.ssn, type = type)
