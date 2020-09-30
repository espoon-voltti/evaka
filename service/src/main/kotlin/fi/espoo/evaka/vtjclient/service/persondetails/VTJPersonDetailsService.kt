// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.mapper.IVtjHenkiloMapper
import fi.espoo.evaka.vtjclient.mapper.VtjPersonNotFoundException
import fi.espoo.evaka.vtjclient.service.cache.VtjCache
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService.DetailsQuery
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.HUOLLETTAVA_HUOLTAJAT
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.PERUSSANOMA3
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.VTJQuery
import mu.KotlinLogging

class VTJPersonDetailsService(
    private val vtjClientService: IVtjClientService,
    private val henkiloMapper: IVtjHenkiloMapper,
    private val vtjCache: VtjCache
) : IPersonDetailsService {

    private val logger = KotlinLogging.logger {}

    /*
        Fetches person with dependants from VTJ, then for each dependant, fetches
        the dependant basic details from VTJ.
     */
    override fun getPersonWithDependants(query: DetailsQuery): PersonDetails {
        val guardianResult = doCacheBackedUpVtjQuery(query, HUOLTAJA_HUOLLETTAVA)
        if (guardianResult is PersonDetails.Result) {
            val childQueries = guardianResult.vtjPerson.dependants
                .map {
                    DetailsQuery(
                        requestingUser = query.requestingUser,
                        targetIdentifier = ExternalIdentifier.SSN.getInstance(it.socialSecurityNumber)
                    )
                }

            val dependants = childQueries
                .map { childQuery -> doCacheBackedUpVtjQuery(childQuery, PERUSSANOMA3) }.mapNotNull {
                    when (it) {
                        is PersonDetails.QueryError -> {
                            logger.error { "Error retrieving child details for guardian: ${it.message}" }
                            null
                        }
                        is PersonDetails.PersonNotFound -> {
                            logger.error { "Child was not found from VTJ based on VTJ data?" }
                            null
                        }
                        is PersonDetails.Result -> it.vtjPerson
                    }
                }

            guardianResult.vtjPerson.dependants = dependants
        }

        return guardianResult
    }

    /*
        Fetches person with guardians from VTJ, then for each guardian, fetches
        the guardian basic details from VTJ.
    */
    override fun getPersonWithGuardians(query: DetailsQuery): PersonDetails {
        val childResult = doCacheBackedUpVtjQuery(query, HUOLLETTAVA_HUOLTAJAT)
        if (childResult is PersonDetails.Result) {
            val guardianQueries = childResult.vtjPerson.guardians
                .map {
                    DetailsQuery(
                        requestingUser = query.requestingUser,
                        targetIdentifier = ExternalIdentifier.SSN.getInstance(it.socialSecurityNumber)
                    )
                }

            val guardians = guardianQueries
                .map { guardianQuery -> doCacheBackedUpVtjQuery(guardianQuery, PERUSSANOMA3) }.mapNotNull {
                    when (it) {
                        is PersonDetails.QueryError -> {
                            logger.error { "Error retrieving guardian details for child: ${it.message}" }
                            null
                        }
                        is PersonDetails.PersonNotFound -> {
                            logger.error { "Guardian was not found from VTJ based on VTJ data?" }
                            null
                        }
                        is PersonDetails.Result -> it.vtjPerson
                    }
                }

            childResult.vtjPerson.guardians = guardians
        }

        return childResult
    }

    override fun getBasicDetailsFor(query: DetailsQuery): PersonDetails {
        return doCacheBackedUpVtjQuery(query, PERUSSANOMA3)
    }

    private fun doCacheBackedUpVtjQuery(query: DetailsQuery, requestType: RequestType): PersonDetails {
        val cacheKey = "${query.targetIdentifier.ssn}-$requestType"
        val cachePerson: VtjPerson? = vtjCache.getPerson(cacheKey)
        if (cachePerson != null) {
            return PersonDetails.Result(cachePerson)
        }

        val vtjResult = doVtjQuery(query.mapToVtjQuery(requestType))
        if (vtjResult is PersonDetails.Result) {
            vtjCache.save(cacheKey, vtjResult.vtjPerson)
        }
        return vtjResult
    }

    private fun doVtjQuery(vtjQuery: VTJQuery): PersonDetails {
        val personDetails = try {
            vtjClientService.query(vtjQuery) ?: return PersonDetails.PersonNotFound()
        } catch (e: VtjPersonNotFoundException) {
            return PersonDetails.PersonNotFound()
        } catch (e: Exception) {
            logger.error("Error fetching VTJ data: $e")
            return PersonDetails.QueryError(e.message ?: "")
        }.let(henkiloMapper::mapToVtjPerson)

        return PersonDetails.Result(personDetails)
    }
}

fun DetailsQuery.mapToVtjQuery(type: RequestType) =
    VTJQuery(
        requestingUserId = requestingUser.id,
        ssn = targetIdentifier.ssn,
        type = type
    )
