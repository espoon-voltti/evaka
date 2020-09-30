// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService.DetailsQuery

class PartialMockPersonDetailsService(
    private val mockPersonDetailsService: IPersonDetailsService,
    private val vtjPersonDetailsService: IPersonDetailsService
) : IPersonDetailsService {
    override fun getPersonWithDependants(query: DetailsQuery): PersonDetails =
        when (val result = mockPersonDetailsService.getPersonWithDependants(query)) {
            is PersonDetails.PersonNotFound -> vtjPersonDetailsService.getPersonWithDependants(query)
            else -> result
        }

    override fun getPersonWithGuardians(query: DetailsQuery): PersonDetails =
        when (val result = mockPersonDetailsService.getPersonWithGuardians(query)) {
            is PersonDetails.PersonNotFound -> vtjPersonDetailsService.getPersonWithGuardians(query)
            else -> result
        }

    override fun getBasicDetailsFor(query: DetailsQuery): PersonDetails =
        when (val result = mockPersonDetailsService.getBasicDetailsFor(query)) {
            is PersonDetails.PersonNotFound -> vtjPersonDetailsService.getBasicDetailsFor(query)
            else -> result
        }
}
