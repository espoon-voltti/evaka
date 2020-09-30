// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

val helsinkiZone: ZoneId = ZoneId.of("Europe/Helsinki")

fun currentDateInFinland(): LocalDate = LocalDate.now(helsinkiZone)
fun OffsetDateTime.toHelsinkiZonedDateTime(): ZonedDateTime = this.atZoneSameInstant(helsinkiZone)
fun OffsetDateTime.toHelsinkiLocalDateTime(): LocalDateTime = this.toHelsinkiZonedDateTime().toLocalDateTime()
