package fi.espoo.evaka.dailyservicetimes

import com.fasterxml.jackson.annotation.JsonTypeInfo
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

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, property = "regular")
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

fun Database.Transaction.upsertChildDailyServiceTimes(childId: UUID, times: DailyServiceTimes) {
    when (times) {
        is DailyServiceTimes.RegularTimes -> upsertRegularChildDailyServiceTimes(childId, times)
        is DailyServiceTimes.IrregularTimes -> upsertIrregularChildDailyServiceTimes(childId, times)
    }
}

private fun Database.Transaction.upsertRegularChildDailyServiceTimes(childId: UUID, times: DailyServiceTimes.RegularTimes) {
    // language=sql
    val sql = """
        INSERT INTO daily_service_time (
            child_id, regular, 
            regular_start, regular_end, 
            monday_start, monday_end, 
            tuesday_start, tuesday_end,
            wednesday_start, wednesday_end, 
            thursday_start, thursday_end, 
            friday_start, friday_end
        )
        VALUES (
            :childId, TRUE, 
            :regularStart, :regularEnd, 
            NULL, NULL,
            NULL, NULL,
            NULL, NULL,
            NULL, NULL,
            NULL, NULL
        )
        
        ON CONFLICT (child_id) DO UPDATE
        
        SET 
            regular = TRUE,
            regular_start = :regularStart,
            regular_end = :regularEnd, 
            monday_start = NULL,
            monday_end = NULL, 
            tuesday_start = NULL, 
            tuesday_end = NULL,
            wednesday_start = NULL,
            wednesday_end = NULL, 
            thursday_start = NULL, 
            thursday_end = NULL, 
            friday_start = NULL, 
            friday_end = NULL
            
    """.trimIndent()

    this.createUpdate(sql)
        .bind("childId", childId)
        .bind("regularStart", times.regularTimes.start)
        .bind("regularEnd", times.regularTimes.end)
        .execute()
}

private fun Database.Transaction.upsertIrregularChildDailyServiceTimes(childId: UUID, times: DailyServiceTimes.IrregularTimes) {
    // language=sql
    val sql = """
        INSERT INTO daily_service_time (
            child_id, regular, 
            regular_start, regular_end, 
            monday_start, monday_end, 
            tuesday_start, tuesday_end,
            wednesday_start, wednesday_end, 
            thursday_start, thursday_end, 
            friday_start, friday_end
        )
        VALUES (
            :childId, FALSE, 
            NULL, NULL,
            :mondayStart, :mondayEnd, 
            :tuesdayStart, :tuesdayEnd,
            :wednesdayStart, :wednesdayEnd, 
            :thursdayStart, :thursdayEnd, 
            :fridayStart, :fridayEnd
        )
        
        ON CONFLICT (child_id) DO UPDATE
        
        SET 
            regular = FALSE,
            regular_start = NULL,
            regular_end = NULL, 
            monday_start = :mondayStart,
            monday_end = :mondayEnd, 
            tuesday_start = :tuesdayStart, 
            tuesday_end = :tuesdayEnd,
            wednesday_start = :wednesdayStart,
            wednesday_end = :wednesdayEnd, 
            thursday_start = :thursdayStart, 
            thursday_end = :thursdayEnd, 
            friday_start = :fridayStart, 
            friday_end = :fridayEnd
            
    """.trimIndent()

    this.createUpdate(sql)
        .bind("childId", childId)
        .bind("mondayStart", times.monday?.start)
        .bind("mondayEnd", times.monday?.end)
        .bind("tuesdayStart", times.tuesday?.start)
        .bind("tuesdayEnd", times.tuesday?.end)
        .bind("wednesdayStart", times.wednesday?.start)
        .bind("wednesdayEnd", times.wednesday?.end)
        .bind("thursdayStart", times.thursday?.start)
        .bind("thursdayEnd", times.thursday?.end)
        .bind("fridayStart", times.friday?.start)
        .bind("fridayEnd", times.friday?.end)
        .execute()
}

fun Database.Transaction.deleteChildDailyServiceTimes(childId: UUID) {
    // language=sql
    val sql = """
        DELETE FROM daily_service_time
        WHERE child_id = :childId
    """.trimIndent()

    this.createUpdate(sql).bind("childId", childId).execute()
}
