// SPDX-FileCopyrightText: 2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import evaka.core.PureJdbiTest
import evaka.core.absence.AbsenceCategory
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildAttendance
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevHolidayPeriod
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Verifies that loading a column as its strongly-typed Kotlin value (e.g. [LocalDate]) and pushing
 * it through [convertToCsv] produces the same CSV cell as casting the column to text in SQL and
 * passing the resulting string straight through.
 */
class BiCsvFormatTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val config = BiExportConfig(includePII = true, includeLegacyColumns = true)

    @Test
    fun `LocalDate field matches text-cast date column in CSV`() {
        db.transaction { tx ->
            val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
            listOf(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 12, 31), LocalDate.of(2020, 2, 29))
                .forEach { date ->
                    tx.insert(
                        DevAbsence(
                            childId = childId,
                            date = date,
                            absenceCategory = AbsenceCategory.BILLABLE,
                        )
                    )
                }
        }
        assertCsvColumnsEqual<TypedDate, TextDate>(
            typedSql = "SELECT date FROM absence ORDER BY date",
            textSql = "SELECT date::text AS date FROM absence ORDER BY date",
        )
    }

    @Test
    fun `UUID field matches text-cast uuid column in CSV`() {
        db.transaction { tx ->
            val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
            repeat(3) { i ->
                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2024, 1, 1).plusDays(i.toLong()),
                        absenceCategory = AbsenceCategory.BILLABLE,
                    )
                )
            }
        }
        assertCsvColumnsEqual<TypedId, TextId>(
            typedSql = "SELECT id FROM absence ORDER BY id",
            textSql = "SELECT id::text AS id FROM absence ORDER BY id",
        )
    }

    @Test
    fun `DateRange field matches text-cast daterange column in CSV`() {
        db.transaction { tx ->
            listOf(
                    FiniteDateRange(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30)),
                    FiniteDateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1)),
                )
                .forEach { period ->
                    tx.insert(DevHolidayPeriod(period = period, reservationDeadline = period.start))
                }
        }
        assertCsvColumnsEqual<TypedPeriod, TextPeriod>(
            typedSql = "SELECT period FROM holiday_period ORDER BY period",
            textSql = "SELECT period::text AS period FROM holiday_period ORDER BY period",
        )
    }

    @Test
    fun `LocalTime field matches text-cast time column in CSV`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
            listOf(
                    LocalTime.of(7, 0) to LocalTime.of(15, 0),
                    LocalTime.of(8, 15) to LocalTime.of(16, 30),
                    LocalTime.of(9, 30) to LocalTime.of(17, 0),
                )
                .forEachIndexed { i, (arrived, departed) ->
                    tx.insert(
                        DevChildAttendance(
                            childId = childId,
                            unitId = unitId,
                            date = LocalDate.of(2024, 5, 1).plusDays(i.toLong()),
                            arrived = arrived,
                            departed = departed,
                        )
                    )
                }
        }
        assertCsvColumnsEqual<TypedTime, TextTime>(
            typedSql = "SELECT start_time FROM child_attendance ORDER BY date, start_time",
            textSql =
                "SELECT start_time::text AS start_time FROM child_attendance ORDER BY date, start_time",
        )
    }

    @Test
    @Disabled("TimeRange vs. conversion to string in SQL provide different format")
    fun `TimeRange field matches text-cast timerange column in CSV`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            tx.insert(
                DevDaycare(
                    areaId = areaId,
                    name = "Daycare A",
                    mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 30)),
                )
            )
            tx.insert(
                DevDaycare(
                    areaId = areaId,
                    name = "Daycare B",
                    mealtimeBreakfast = TimeRange(LocalTime.of(7, 15), LocalTime.of(9, 45)),
                )
            )
        }
        assertCsvColumnsEqual<TypedTimeRange, TextTimeRange>(
            typedSql =
                "SELECT mealtime_breakfast FROM daycare WHERE mealtime_breakfast IS NOT NULL ORDER BY name",
            textSql =
                "SELECT mealtime_breakfast::text AS mealtime_breakfast FROM daycare WHERE mealtime_breakfast IS NOT NULL ORDER BY name",
        )
    }

    private inline fun <reified A : Any, reified B : Any> assertCsvColumnsEqual(
        typedSql: String,
        textSql: String,
    ) {
        val typedCsv = db.read { tx ->
            BiQueries.StreamingCsvQuery(A::class) { it.createQuery { sql(typedSql) }.mapTo<A>() }
                .invoke(tx, config) { it.toList() }
        }
        val textCsv = db.read { tx ->
            BiQueries.StreamingCsvQuery(B::class) { it.createQuery { sql(textSql) }.mapTo<B>() }
                .invoke(tx, config) { it.toList() }
        }
        assertEquals(typedCsv, textCsv)
    }

    data class TypedDate(val date: LocalDate)

    data class TextDate(val date: String)

    data class TypedId(val id: UUID)

    data class TextId(val id: String)

    data class TypedPeriod(val period: DateRange)

    data class TextPeriod(val period: String)

    data class TypedTime(val start_time: LocalTime)

    data class TextTime(val start_time: String)

    data class TypedTimeRange(val mealtime_breakfast: TimeRange)

    data class TextTimeRange(val mealtime_breakfast: String)
}
