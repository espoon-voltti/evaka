// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.shared.EvakaUserId

interface VtjClient {
    fun fetchPerson(query: DetailsQuery): VtjPerson

    fun fetchWithGuardians(query: DetailsQuery): VtjPersonAndGuardians

    fun fetchWithDependants(query: DetailsQuery): VtjPersonAndDependants

    data class DetailsQuery(val requestingUser: EvakaUserId, val ssn: Ssn)
}
