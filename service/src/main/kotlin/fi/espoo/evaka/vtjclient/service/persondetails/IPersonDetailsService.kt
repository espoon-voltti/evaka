// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.vtjclient.dto.VtjPerson

interface IPersonDetailsService {
    fun getPersonWithDependants(query: DetailsQuery): VtjPerson

    fun getPersonWithGuardians(query: DetailsQuery): VtjPerson

    fun getBasicDetailsFor(query: DetailsQuery): VtjPerson

    data class DetailsQuery(
        val requestingUser: EvakaUserId,
        val targetIdentifier: ExternalIdentifier.SSN,
    )
}
