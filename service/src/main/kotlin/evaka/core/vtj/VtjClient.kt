// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.shared.EvakaUserId

/**
 * Each function returns a map whose key is the requested SSN. A person that VTJ does not find has
 * no key. For a corrected SSN, the person in the value has the active SSN.
 */
interface VtjClient {
    fun fetchPersons(requestingUser: EvakaUserId, ssns: Collection<Ssn>): Map<Ssn, VtjPerson>

    fun fetchWithGuardians(
        requestingUser: EvakaUserId,
        ssns: Collection<Ssn>,
    ): Map<Ssn, VtjPersonAndGuardians>

    fun fetchWithDependants(
        requestingUser: EvakaUserId,
        ssns: Collection<Ssn>,
    ): Map<Ssn, VtjPersonAndDependants>
}
