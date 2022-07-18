// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dailyservicetimes

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.TimeRange
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.PropagateNull
import java.time.DayOfWeek
import java.time.LocalDate

enum class DailyServiceTimesType {
    REGULAR,
    IRREGULAR,
    VARIABLE_TIME
}

data class DailyServiceTimesWithId(val id: DailyServiceTimesId, val times: DailyServiceTimes)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class DailyServiceTimes(
    val type: DailyServiceTimesType,
) {
    abstract val validityPeriod: DateRange

    abstract fun asUpdateRow(): DailyServiceTimeUpdateRow

    @JsonTypeName("REGULAR")
    data class RegularTimes(
        val regularTimes: TimeRange,
        override val validityPeriod: DateRange
    ) : DailyServiceTimes(DailyServiceTimesType.REGULAR) {
        override fun asUpdateRow() = DailyServiceTimeUpdateRow(
            type = DailyServiceTimesType.REGULAR,
            regularTimes = regularTimes,
            mondayTimes = null,
            tuesdayTimes = null,
            wednesdayTimes = null,
            thursdayTimes = null,
            fridayTimes = null,
            saturdayTimes = null,
            sundayTimes = null,
            validityPeriod = validityPeriod
        )
    }

    @JsonTypeName("IRREGULAR")
    data class IrregularTimes(
        val monday: TimeRange?,
        val tuesday: TimeRange?,
        val wednesday: TimeRange?,
        val thursday: TimeRange?,
        val friday: TimeRange?,
        val saturday: TimeRange?,
        val sunday: TimeRange?,
        override val validityPeriod: DateRange
    ) : DailyServiceTimes(DailyServiceTimesType.IRREGULAR) {
        fun timesForDayOfWeek(dayOfWeek: DayOfWeek) = when (dayOfWeek) {
            DayOfWeek.MONDAY -> monday
            DayOfWeek.TUESDAY -> tuesday
            DayOfWeek.WEDNESDAY -> wednesday
            DayOfWeek.THURSDAY -> thursday
            DayOfWeek.FRIDAY -> friday
            DayOfWeek.SATURDAY -> saturday
            DayOfWeek.SUNDAY -> sunday
        }

        override fun asUpdateRow() = DailyServiceTimeUpdateRow(
            type = DailyServiceTimesType.IRREGULAR,
            regularTimes = null,
            mondayTimes = monday,
            tuesdayTimes = tuesday,
            wednesdayTimes = wednesday,
            thursdayTimes = thursday,
            fridayTimes = friday,
            saturdayTimes = saturday,
            sundayTimes = sunday,
            validityPeriod = validityPeriod
        )
    }

    @JsonTypeName("VARIABLE_TIME")
    data class VariableTimes(
        override val validityPeriod: DateRange
    ) : DailyServiceTimes(DailyServiceTimesType.VARIABLE_TIME) {
        override fun asUpdateRow() = DailyServiceTimeUpdateRow(
            type = DailyServiceTimesType.VARIABLE_TIME,
            regularTimes = null,
            mondayTimes = null,
            tuesdayTimes = null,
            wednesdayTimes = null,
            thursdayTimes = null,
            fridayTimes = null,
            saturdayTimes = null,
            sundayTimes = null,
            validityPeriod = validityPeriod
        )
    }

    fun withId(id: DailyServiceTimesId) = DailyServiceTimesWithId(id, this)
}

data class DailyServiceTimeRow(
    @PropagateNull
    val id: DailyServiceTimesId,
    val type: DailyServiceTimesType,

    val regularTimes: TimeRange?,

    val mondayTimes: TimeRange?,
    val tuesdayTimes: TimeRange?,
    val wednesdayTimes: TimeRange?,
    val thursdayTimes: TimeRange?,
    val fridayTimes: TimeRange?,
    val saturdayTimes: TimeRange?,
    val sundayTimes: TimeRange?,

    val validityPeriod: DateRange
)

fun Database.Read.getChildDailyServiceTimes(childId: ChildId): List<DailyServiceTimesWithId> {
    return this.createQuery(
        """
SELECT
    id,
    type,
    regular_times,
    monday_times,
    tuesday_times,
    wednesday_times,
    thursday_times,
    friday_times,
    saturday_times,
    sunday_times,
    validity_period
FROM daily_service_time
WHERE child_id = :childId
        """.trimIndent()
    )
        .bind("childId", childId)
        .mapTo<DailyServiceTimeRow>()
        .map { toDailyServiceTimes(it) }
        .toList()
}

data class ValidDailyServiceTimeRow(
    val childId: ChildId,
    val validityPeriod: DateRange
)

fun Database.Read.getChildDailyServiceTimeValidity(id: DailyServiceTimesId): ValidDailyServiceTimeRow? {
    return this.createQuery(
        """
SELECT child_id, validity_period
FROM daily_service_time
WHERE id = :id
        """.trimIndent()
    )
        .bind("id", id)
        .mapTo<ValidDailyServiceTimeRow>()
        .firstOrNull()
}

fun toDailyServiceTimes(row: DailyServiceTimeRow): DailyServiceTimesWithId {
    return when (row.type) {
        DailyServiceTimesType.REGULAR -> DailyServiceTimes.RegularTimes(
            regularTimes = row.regularTimes ?: throw IllegalStateException("Regular daily service times must have regular times"),
            validityPeriod = row.validityPeriod
        ).withId(row.id)
        DailyServiceTimesType.IRREGULAR -> DailyServiceTimes.IrregularTimes(
            monday = row.mondayTimes,
            tuesday = row.tuesdayTimes,
            wednesday = row.wednesdayTimes,
            thursday = row.thursdayTimes,
            friday = row.fridayTimes,
            saturday = row.saturdayTimes,
            sunday = row.sundayTimes,
            validityPeriod = row.validityPeriod
        ).withId(row.id)
        DailyServiceTimesType.VARIABLE_TIME -> DailyServiceTimes.VariableTimes(
            validityPeriod = row.validityPeriod
        ).withId(row.id)
    }.exhaust()
}

data class DailyServiceTimeUpdateRow(
    val type: DailyServiceTimesType,

    val regularTimes: TimeRange?,

    val mondayTimes: TimeRange?,
    val tuesdayTimes: TimeRange?,
    val wednesdayTimes: TimeRange?,
    val thursdayTimes: TimeRange?,
    val fridayTimes: TimeRange?,
    val saturdayTimes: TimeRange?,
    val sundayTimes: TimeRange?,

    val validityPeriod: DateRange
)

fun Database.Transaction.updateChildDailyServiceTimes(id: DailyServiceTimesId, times: DailyServiceTimes) {
    val sql = """
        UPDATE daily_service_time
        SET 
            type = :type,
            regular_times = :regularTimes,
            monday_times = :mondayTimes,
            tuesday_times = :tuesdayTimes,
            wednesday_times = :wednesdayTimes,
            thursday_times = :thursdayTimes,
            friday_times = :fridayTimes,
            saturday_times = :saturdayTimes,
            sunday_times = :sundayTimes
        WHERE id = :id
    """.trimIndent()

    this.createUpdate(sql)
        .bindKotlin(times.asUpdateRow())
        .bind("id", id)
        .execute()
}

fun Database.Transaction.addDailyServiceTimesNotification(
    id: DailyServiceTimesId,
    childId: ChildId,
    dateFrom: LocalDate,
    hasDeletedReservations: Boolean
) {
    val sql = """
        INSERT INTO daily_service_time_notification (guardian_id, daily_service_time_id, date_from, has_deleted_reservations)
        SELECT guardian_id, :id, :dateFrom, :hasDeletedReservations FROM guardian WHERE child_id = :childId
    """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("childId", childId)
        .bind("dateFrom", dateFrom)
        .bind("hasDeletedReservations", hasDeletedReservations)
        .execute()
}

fun Database.Transaction.deleteChildDailyServiceTimes(id: DailyServiceTimesId) {
    // language=sql
    val sql = """
        DELETE FROM daily_service_time
        WHERE id = :id
    """.trimIndent()

    this.createUpdate(sql).bind("id", id).execute()
}

fun Database.Transaction.createChildDailyServiceTimes(childId: ChildId, times: DailyServiceTimes): DailyServiceTimesId {
    val sql = """
        INSERT INTO daily_service_time (
            child_id, type, 
            regular_times, 
            monday_times, 
            tuesday_times,
            wednesday_times, 
            thursday_times, 
            friday_times,
            saturday_times,
            sunday_times,
            validity_period
        )
        VALUES (
            :childId, :type, 
            :regularTimes, 
            :mondayTimes,
            :tuesdayTimes,
            :wednesdayTimes,
            :thursdayTimes,
            :fridayTimes,
            :saturdayTimes,
            :sundayTimes,
            :validityPeriod
        )
        RETURNING id
    """.trimIndent()

    return createQuery(sql)
        .bindKotlin(times.asUpdateRow())
        .bind("childId", childId)
        .mapTo<DailyServiceTimesId>()
        .first()
}

data class DailyServiceTimesValidityWithId(val id: DailyServiceTimesId, val validityPeriod: DateRange)

fun Database.Read.getOverlappingChildDailyServiceTimes(
    childId: ChildId,
    range: DateRange
): List<DailyServiceTimesValidityWithId> {
    //language=sql
    val sql =
        """
        SELECT id, validity_period
        FROM daily_service_time
        WHERE child_id = :childId
          AND :range && validity_period
        """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .bind("range", range)
        .mapTo<DailyServiceTimesValidityWithId>()
        .list()
}

fun Database.Transaction.updateChildDailyServiceTimesValidity(id: DailyServiceTimesId, validityPeriod: DateRange) {
    val sql = """
        UPDATE daily_service_time
        SET validity_period = :validityPeriod
        WHERE id = :id
    """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("validityPeriod", validityPeriod)
        .execute()
}
