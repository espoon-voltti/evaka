// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.persondetails

import evaka.core.identity.Ssn
import evaka.core.shared.EvakaUserId
import evaka.core.vtjclient.dto.VtjPerson

interface VtjClient {
    fun fetchPerson(query: DetailsQuery): VtjPerson

    fun fetchWithGuardians(query: DetailsQuery): VtjPerson

    fun fetchWithDependants(query: DetailsQuery): VtjPerson

    data class DetailsQuery(val requestingUser: EvakaUserId, val ssn: Ssn)
}
