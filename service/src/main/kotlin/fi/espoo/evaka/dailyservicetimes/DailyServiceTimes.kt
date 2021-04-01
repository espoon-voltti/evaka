package fi.espoo.evaka.dailyservicetimes

import fi.espoo.evaka.shared.db.Database
import java.time.LocalTime
import java.util.UUID

data class TimeRange(
    val start: LocalTime,
    val end: LocalTime
) {
    constructor(startText: String, endText: String) : this(
        start = LocalTime.parse(startText),
        end = LocalTime.parse(endText)
    )
}

sealed class DailyServiceTimes(
    val regular: Boolean
) {

    class RegularTimes(
        val regularTimes: TimeRange
    ) : DailyServiceTimes(regular = true)

    class IrregularTimes(
        val monday: TimeRange?,
        val tuesday: TimeRange?,
        val wednesday: TimeRange?,
        val thursday: TimeRange?,
        val friday: TimeRange?
    ) : DailyServiceTimes(regular = false)
}

fun Database.Read.getChildDailyServiceTimes(childId: UUID): DailyServiceTimes? {
    // language=sql
    val sql = """
        SELECT *
        FROM daily_service_time
        WHERE child_id = :childId
    """.trimIndent()

    return this.createQuery(sql)
        .bind("childId", childId)
        .map { rs, _ ->
            val regular = rs.getBoolean("regular")
            if (regular) {
                DailyServiceTimes.RegularTimes(
                    regularTimes = TimeRange(
                        rs.getString("regular_start"),
                        rs.getString("regular_end")
                    )
                )
            } else {
                DailyServiceTimes.IrregularTimes(
                    monday = rs.getString("monday_start")?.let { start ->
                        TimeRange(start, rs.getString("monday_end"))
                    },
                    tuesday = rs.getString("tuesday_start")?.let { start ->
                        TimeRange(start, rs.getString("tuesday_end"))
                    },
                    wednesday = rs.getString("wednesday_start")?.let { start ->
                        TimeRange(start, rs.getString("wednesday_end"))
                    },
                    thursday = rs.getString("thursday_start")?.let { start ->
                        TimeRange(start, rs.getString("thursday_end"))
                    },
                    friday = rs.getString("friday_start")?.let { start ->
                        TimeRange(start, rs.getString("friday_end"))
                    },
                )
            }
        }
        .firstOrNull()
}
