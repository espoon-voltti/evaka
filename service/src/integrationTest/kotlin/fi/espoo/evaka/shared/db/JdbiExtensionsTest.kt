// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.utils.europeHelsinki
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

private inline fun <reified T : Any> Database.Read.passThrough(input: T) = createQuery(
    // language=SQL
    "SELECT :input AS output"
).bind("input", input)
    .map { row -> row.mapColumn<T>("output") }
    .single()

private inline fun <reified T : Any> Database.Read.checkMatch(@Language("sql") sql: String, input: T) = createQuery(
    sql
).bind("input", input)
    .mapTo<Boolean>()
    .single()

class JdbiExtensionsTest : PureJdbiTest() {
    private val utc: ZoneId = ZoneId.of("UTC")

    @Test
    fun testCoordinate() {
        val input = Coordinate(lat = 11.0, lon = 22.0)

        val match = db.read { it.checkMatch("SELECT :input ~= point(22.0, 11.0)", input) }
        assertTrue(match)

        val output = db.read { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testNullCoordinate() {
        data class QueryResult(val value: Coordinate?)

        val result = db.read {
            it.createQuery("SELECT NULL::point AS value").mapTo<QueryResult>().single()
        }
        assertNull(result.value)
    }

    @Test
    fun testCoordinateListResult() {
        data class QueryResult(val values: List<Coordinate>)

        val result = db.read {
            it.createQuery("SELECT array[point(22.0, 11.0), point(44.0, 33.0)]::point[] AS values").mapTo<QueryResult>().single()
        }
        assertEquals(listOf(Coordinate(lat = 11.0, lon = 22.0), Coordinate(lat = 33.0, lon = 44.0)), result.values)
    }

    @Test
    fun testDateRange() {
        val input = DateRange(LocalDate.of(2020, 1, 2), LocalDate.of(2020, 3, 4))

        val match = db.read { it.checkMatch("SELECT :input = daterange('2020-01-02', '2020-03-04', '[]')", input) }
        assertTrue(match)

        val output = db.read { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testDateRangeWithoutEnd() {
        val input = DateRange(LocalDate.of(2020, 6, 7), null)

        val match = db.read { it.checkMatch("SELECT :input = daterange('2020-06-07', NULL, '[]')", input) }
        assertTrue(match)

        val output = db.read { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testNullDateRange() {
        data class QueryResult(val value: DateRange?)

        val result = db.read {
            it.createQuery("SELECT NULL::daterange AS value").mapTo<QueryResult>().single()
        }
        assertNull(result.value)
    }

    @Test
    fun testDateRangeListResult() {
        data class QueryResult(val values: List<DateRange>)

        val result = db.read {
            it.createQuery("SELECT array[daterange('2020-06-07', NULL, '[]'), daterange('2021-01-01', '2021-01-02', '[]')]::daterange[] AS values").mapTo<QueryResult>().single()
        }
        assertEquals(listOf(DateRange(LocalDate.of(2020, 6, 7), null), DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2))), result.values)
    }

    @Test
    fun testFiniteDateRange() {
        val input = FiniteDateRange(LocalDate.of(2020, 9, 10), LocalDate.of(2020, 11, 12))

        val match = db.read { it.checkMatch("SELECT :input = daterange('2020-09-10', '2020-11-12', '[]')", input) }
        assertTrue(match)

        val output = db.read { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testNullFiniteDateRange() {
        data class QueryResult(val value: FiniteDateRange?)

        val result = db.read {
            it.createQuery("SELECT NULL::daterange AS value").mapTo<QueryResult>().single()
        }
        assertNull(result.value)
    }

    @Test
    fun testFiniteDateRangeListResult() {
        data class QueryResult(val values: List<FiniteDateRange>)

        val result = db.read {
            it.createQuery("SELECT array[daterange('2020-06-07', '2021-01-01', '[]'), daterange('2021-01-01', '2021-01-02', '[]')]::daterange[] AS values").mapTo<QueryResult>().single()
        }
        assertEquals(listOf(FiniteDateRange(LocalDate.of(2020, 6, 7), LocalDate.of(2021, 1, 1)), FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2))), result.values)
    }

    @Test
    fun testExternalId() {
        val input = ExternalId.of(namespace = "test", value = "123456")

        val match = db.read { it.checkMatch("SELECT :input = 'test:123456'", input) }
        assertTrue(match)

        val output = db.read { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testNullExternalId() {
        data class QueryResult(val value: ExternalId?)

        val result = db.read {
            it.createQuery("SELECT NULL::text AS value").mapTo<QueryResult>().single()
        }
        assertNull(result.value)
    }

    @Test
    fun testExternalIdListResult() {
        data class QueryResult(val values: List<ExternalId>)

        val result = db.read {
            it.createQuery("SELECT array['test:123456', 'more:42']::text[] AS values").mapTo<QueryResult>().single()
        }
        assertEquals(listOf(ExternalId.of(namespace = "test", value = "123456"), ExternalId.of(namespace = "more", value = "42")), result.values)
    }

    @Test
    fun testId() {
        val input = PersonId(UUID.fromString("5ea2618c-3e9d-4fd3-8094-8d2f35311962"))

        val match = db.read { it.checkMatch("SELECT :input = '5ea2618c-3e9d-4fd3-8094-8d2f35311962'", input) }
        assertTrue(match)

        val output = db.read { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testNullId() {
        data class QueryResult(val value: PersonId?)

        val result = db.read {
            it.createQuery("SELECT NULL::uuid AS value").mapTo<QueryResult>().single()
        }
        assertNull(result.value)
    }

    @Test
    fun testIdListResult() {
        data class QueryResult(val values: List<PersonId>)

        val result = db.read {
            it.createQuery("SELECT array['5ea2618c-3e9d-4fd3-8094-8d2f35311962', '2db6c1c7-402f-4d86-a308-a7f1b19bb313']::uuid[] AS values").mapTo<QueryResult>().single()
        }
        assertEquals(listOf(PersonId(UUID.fromString("5ea2618c-3e9d-4fd3-8094-8d2f35311962")), PersonId(UUID.fromString("2db6c1c7-402f-4d86-a308-a7f1b19bb313"))), result.values)
    }

    @Test
    fun testIdInJsonb() {
        data class JsonbObject(val value: PersonId)
        data class QueryResult(@Json val jsonb: JsonbObject)

        val result = db.read {
            it.createQuery("SELECT jsonb_build_object('value', '5ea2618c-3e9d-4fd3-8094-8d2f35311962'::uuid) AS jsonb").mapTo<QueryResult>().single()
        }
        val expected = PersonId(UUID.fromString("5ea2618c-3e9d-4fd3-8094-8d2f35311962"))
        assertEquals(expected, result.jsonb.value)
    }

    @Test
    fun testHelsinkiDateTime() {
        val input = HelsinkiDateTime.from(ZonedDateTime.of(LocalDate.of(2020, 5, 7), LocalTime.of(13, 59), europeHelsinki))

        val match = db.read { it.checkMatch("SELECT :input = '2020-05-07T10:59Z'", input) }
        assertTrue(match)

        val output = db.read { it.passThrough(input) }
        assertEquals(input, output)
    }

    @Test
    fun testNullHelsinkiDateTime() {
        data class QueryResult(val value: HelsinkiDateTime?)

        val result = db.read {
            it.createQuery("SELECT NULL::timestamptz AS value").mapTo<QueryResult>().single()
        }
        assertNull(result.value)
    }

    @Test
    fun testHelsinkiDateTimeListResult() {
        data class QueryResult(val values: List<HelsinkiDateTime>)

        val result = db.read {
            it.createQuery("SELECT array['2020-05-07T10:59Z', '2021-01-10T06:42Z']::timestamptz[] AS values").mapTo<QueryResult>().single()
        }
        val values = result.values.map { it.toZonedDateTime().withZoneSameInstant(utc) }
        assertEquals(
            listOf(
                ZonedDateTime.of(LocalDate.of(2020, 5, 7), LocalTime.of(10, 59), utc),
                ZonedDateTime.of(LocalDate.of(2021, 1, 10), LocalTime.of(6, 42), utc)
            ),
            values
        )
    }

    @Test
    fun testHelsinkiDateTimeInJsonb() {
        data class JsonbObject(val value: HelsinkiDateTime)
        data class QueryResult(@Json val jsonb: JsonbObject)

        val result = db.read {
            it.createQuery("SELECT jsonb_build_object('value', '2020-05-07T10:59Z'::timestamptz) AS jsonb").mapTo<QueryResult>().single()
        }
        val expected = ZonedDateTime.of(LocalDate.of(2020, 5, 7), LocalTime.of(10, 59), utc)
        assertEquals(expected, result.jsonb.value.toZonedDateTime().withZoneSameInstant(utc))
    }
}
