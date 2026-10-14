// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.dto

import evaka.core.identity.Ssn
import java.time.LocalDate

data class VtjRelative(
    val ssn: Ssn,
    val dateOfBirth: LocalDate,
    val firstNames: String,
    val lastName: String,
)

data class VtjPersonWithRelatives(val person: VtjPerson, val relatives: List<VtjRelative>)
