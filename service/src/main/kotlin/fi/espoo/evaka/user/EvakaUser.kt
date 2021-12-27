// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.user

import fi.espoo.evaka.shared.EvakaUserId
import org.jdbi.v3.core.mapper.PropagateNull

enum class EvakaUserType {
    SYSTEM, CITIZEN, EMPLOYEE, MOBILE_DEVICE, UNKNOWN
}

data class EvakaUser(
    @PropagateNull
    val id: EvakaUserId,
    val name: String,
    val type: EvakaUserType
)
