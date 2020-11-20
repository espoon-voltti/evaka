// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.shared.utils

import java.time.LocalDate
import java.time.ZoneId

val zoneId: ZoneId = ZoneId.of("Europe/Helsinki")

fun dateNow(): LocalDate = LocalDate.now(zoneId)
