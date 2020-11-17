package fi.espoo.evaka.shared.utils

import java.time.LocalDate
import java.time.ZoneId

val zoneId: ZoneId = ZoneId.of("Europe/Helsinki")

fun dateNow(): LocalDate = LocalDate.now(zoneId)
