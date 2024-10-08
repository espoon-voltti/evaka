// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.JdbiException
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLState

class DaysInRangeSqlTest : PureJdbiTest(resetDbBeforeEach = false) {
    private fun daysInRange(@Language("SQL", prefix = "SELECT ") dateRangeSql: String): Int? =
        db.read { it.createQuery { sql("SELECT days_in_range($dateRangeSql)") }.exactlyOne<Int?>() }

    @Test
    fun `it works correctly with all endpoint inclusivity combinations`() {
        // postgres seems to internally always convert dateranges to `[)` inclusivity and the
        // constructor function does the necessary off-by-one adjustment depending on the input.
        // Even so we test all 4 possible cases to detect any possible regressions in the future
        assertEquals(2, daysInRange("daterange('2020-01-01', '2020-01-02', '[]')"))
        assertEquals(1, daysInRange("daterange('2020-01-01', '2020-01-02', '[)')"))
        assertEquals(1, daysInRange("daterange('2020-01-01', '2020-01-02', '(]')"))
        assertEquals(0, daysInRange("daterange('2020-01-01', '2020-01-02', '()')"))
    }

    @Test
    fun `it returns 0 when the range is empty`() {
        assertEquals(0, daysInRange("'empty'::daterange"))
    }

    @Test
    fun `it returns NULL when the entire range is NULL`() {
        assertNull(daysInRange("NULL::daterange"))
    }

    @Test
    fun `it throws an sql datetime overflow error if either endpoint is NULL, infinity, or -infinity`() {
        fun assertDateTimeOverflow(f: () -> Any?) {
            val psqlException = assertThrows<JdbiException> { f() }.psqlCause()
            assertEquals(PSQLState.DATETIME_OVERFLOW.state, psqlException?.sqlState)
        }
        assertDateTimeOverflow { daysInRange("daterange('2020-01-01', NULL, '[]')") }
        assertDateTimeOverflow { daysInRange("daterange(NULL, '2020-01-02', '[]')") }
        assertDateTimeOverflow { daysInRange("daterange('2020-01-01', 'infinity', '[]')") }
        assertDateTimeOverflow { daysInRange("daterange('-infinity', '2020-01-02', '[]')") }
    }
}
