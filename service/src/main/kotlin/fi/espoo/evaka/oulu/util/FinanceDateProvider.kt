// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.util

import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class FinanceDateProvider(val evakaClock: EvakaClock = RealEvakaClock()) {
    fun currentDate(): String {
        val invoiceIdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return evakaClock.today().format(invoiceIdFormatter)
    }

    fun previousMonth(): String {
        val previousMonth = evakaClock.today().minusMonths(1)
        val titleFormatter = DateTimeFormatter.ofPattern("MM.yyyy")
        return previousMonth.format(titleFormatter)
    }

    fun currentDateWithAbbreviatedYear(): String {
        val invoiceIdFormatter = DateTimeFormatter.ofPattern("yyMMdd")
        return evakaClock.today().format(invoiceIdFormatter)
    }

    fun previousMonthLastDate(): String {
        val previousMonthlastDate = YearMonth.from(evakaClock.today()).minusMonths(1).atEndOfMonth()
        val invoiceIdFormatter = DateTimeFormatter.ofPattern("yyMMdd")
        return previousMonthlastDate.format(invoiceIdFormatter)
    }

    fun previousMonthYYMM(): String {
        val previousMonth = evakaClock.today().minusMonths(1)
        val titleFormatter = DateTimeFormatter.ofPattern("yyMM")
        return previousMonth.format(titleFormatter)
    }
}
