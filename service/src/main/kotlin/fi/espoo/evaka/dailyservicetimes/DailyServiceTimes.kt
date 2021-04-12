package fi.espoo.evaka.dailyservicetimes

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import org.jdbi.v3.core.result.RowView
import java.time.LocalTime
import java.util.UUID

data class TimeRange(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val start: LocalTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
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
    data class RegularTimes(
        val regularTimes: TimeRange
    ) : DailyServiceTimes(regular = true)

    data class IrregularTimes(
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
        .map { row -> toDailyServiceTimes(row) }
        .firstOrNull()
}

fun toDailyServiceTimes(row: RowView): DailyServiceTimes? {
    val regular: Boolean = row.mapColumn("regular") ?: return null
    return if (regular) {
        DailyServiceTimes.RegularTimes(
            regularTimes = TimeRange(
                row.mapColumn<String>("regular_start"),
                row.mapColumn<String>("regular_end")
            )
        )
    } else {
        DailyServiceTimes.IrregularTimes(
            monday = row.mapColumn<String?>("monday_start")?.let { start -> TimeRange(start, row.mapColumn("monday_end")) },
            tuesday = row.mapColumn<String?>("tuesday_start")?.let { start -> TimeRange(start, row.mapColumn("tuesday_end")) },
            wednesday = row.mapColumn<String?>("wednesday_start")?.let { start -> TimeRange(start, row.mapColumn("wednesday_end")) },
            thursday = row.mapColumn<String?>("thursday_start")?.let { start -> TimeRange(start, row.mapColumn("thursday_end")) },
            friday = row.mapColumn<String?>("friday_start")?.let { start -> TimeRange(start, row.mapColumn("friday_end")) }
        )
    }
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
