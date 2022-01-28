// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.holidays

import de.jollyday.HolidayCalendar
import de.jollyday.HolidayManager
import de.jollyday.ManagerParameters
import java.time.LocalDate

fun main() {
    getHolidays(2022).forEach {
        println("$it")
    }
}

fun getHolidays(year: Int): List<LocalDate> {
    val holidayManager = HolidayManager.getInstance(ManagerParameters.create(HolidayCalendar.FINLAND))
    return holidayManager.getHolidays(year).map { it.date }.sorted()
}
