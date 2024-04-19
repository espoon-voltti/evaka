// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.user

import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.db.DatabaseEnum
import org.jdbi.v3.core.mapper.PropagateNull

enum class EvakaUserType : DatabaseEnum {
    SYSTEM,
    CITIZEN,
    EMPLOYEE,
    MOBILE_DEVICE,
    UNKNOWN;

    override val sqlType: String = "evaka_user_type"
}

data class EvakaUser(@PropagateNull val id: EvakaUserId, val name: String, val type: EvakaUserType)
