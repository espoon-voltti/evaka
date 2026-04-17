// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.persondetails

import evaka.core.identity.ExternalIdentifier
import evaka.core.shared.EvakaUserId
import evaka.core.vtjclient.dto.VtjPerson

interface IPersonDetailsService {
    fun getPersonWithDependants(query: DetailsQuery): VtjPerson

    fun getPersonWithGuardians(query: DetailsQuery): VtjPerson

    fun getBasicDetailsFor(query: DetailsQuery): VtjPerson

    data class DetailsQuery(
        val requestingUser: EvakaUserId,
        val targetIdentifier: ExternalIdentifier.SSN,
    )
}
