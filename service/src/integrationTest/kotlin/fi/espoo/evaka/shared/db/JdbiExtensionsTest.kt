// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.Period
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

private inline fun <reified T : Any> Handle.passThrough(input: T) = createQuery(
    // language=SQL
    "SELECT :input AS output"
).bind("input", input)
    .map { rs, ctx -> ctx.mapColumn<T>(rs, "output") }
    .asSequence()
    .first()

private inline fun <reified T : Any> Handle.checkMatch(@Language("sql") sql: String, input: T) = createQuery(
    sql
).bind("input", input)
    .mapTo<Boolean>()
    .asSequence()
    .first()

class JdbiExtensionsTest : PureJdbiTest() {
    @Test
    fun testCoordinate() {
        val input = Coordinate(lat = 11.0, lon = 22.0)

        val match = jdbi.handle { it.checkMatch("SELECT :input ~= point(22.0, 11.0)", input) }
        assertTrue(match)

        val output = jdbi.handle { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testPeriod() {
        val input = Period(LocalDate.of(2020, 1, 2), LocalDate.of(2020, 3, 4))

        val match = jdbi.handle { it.checkMatch("SELECT :input = daterange('2020-01-02', '2020-03-04', '[]')", input) }
        assertTrue(match)

        val output = jdbi.handle { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testPeriodWithoutEnd() {
        val input = Period(LocalDate.of(2020, 6, 7), null)

        val match = jdbi.handle { it.checkMatch("SELECT :input = daterange('2020-06-07', NULL, '[]')", input) }
        assertTrue(match)

        val output = jdbi.handle { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testClosedPeriod() {
        val input = ClosedPeriod(LocalDate.of(2020, 9, 10), LocalDate.of(2020, 11, 12))

        val match = jdbi.handle { it.checkMatch("SELECT :input = daterange('2020-09-10', '2020-11-12', '[]')", input) }
        assertTrue(match)

        val output = jdbi.handle { it.passThrough(input) }
        assertEquals(input, output)
    }
}
