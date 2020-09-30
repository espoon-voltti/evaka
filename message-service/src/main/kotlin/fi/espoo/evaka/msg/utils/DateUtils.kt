// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.utils

import java.time.LocalDate
import java.time.ZoneId

const val ZONE_ID_FINLAND = "Europe/Helsinki"

fun getCurrentDateInFinland() = LocalDate.now(ZoneId.of(ZONE_ID_FINLAND))
