// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.domain.getHolidays
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.time.LocalDate

@Deprecated("Instead of mocking, use real holidays like Jan 6th, May 1st or Dec 6th")
fun <T> withHolidays(holidays: Set<LocalDate>, fn: () -> T): T {
    mockkStatic(::getHolidays)
    every { getHolidays(any()) } returns holidays
    return try {
        fn()
    } finally {
        unmockkAll()
    }
}
