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
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.DayOfWeek
import java.time.LocalDate
import org.jdbi.v3.core.mapper.PropagateNull

enum class DailyServiceTimesType : DatabaseEnum {
    REGULAR,
    IRREGULAR,
    VARIABLE_TIME;

    override val sqlType: String = "daily_service_time_type"
}

data class DailyServiceTimes(
    val id: DailyServiceTimesId,
    val childId: ChildId,
    val times: DailyServiceTimesValue
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class DailyServiceTimesValue(
    open val validityPeriod: DateRange,
    val type: DailyServiceTimesType
) {
    abstract fun asUpdateRow(): DailyServiceTimeUpdateRow

    abstract fun equalsIgnoringValidity(other: DailyServiceTimesValue): Boolean

    abstract fun getTimesOnDate(date: LocalDate): TimeRange?

    @JsonTypeName("REGULAR")
    data class RegularTimes(override val validityPeriod: DateRange, val regularTimes: TimeRange) :
        DailyServiceTimesValue(validityPeriod, DailyServiceTimesType.REGULAR) {
        override fun asUpdateRow() =
            DailyServiceTimeUpdateRow(
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

        override fun equalsIgnoringValidity(other: DailyServiceTimesValue): Boolean =
            other is RegularTimes && regularTimes == other.regularTimes

        override fun getTimesOnDate(date: LocalDate): TimeRange? {
            if (!validityPeriod.includes(date)) return null

            return regularTimes
        }
    }

    @JsonTypeName("IRREGULAR")
    data class IrregularTimes(
        override val validityPeriod: DateRange,
        val monday: TimeRange?,
        val tuesday: TimeRange?,
        val wednesday: TimeRange?,
        val thursday: TimeRange?,
        val friday: TimeRange?,
        val saturday: TimeRange?,
        val sunday: TimeRange?
    ) : DailyServiceTimesValue(validityPeriod, DailyServiceTimesType.IRREGULAR) {
        fun timesForDayOfWeek(dayOfWeek: DayOfWeek) =
            when (dayOfWeek) {
                DayOfWeek.MONDAY -> monday
                DayOfWeek.TUESDAY -> tuesday
                DayOfWeek.WEDNESDAY -> wednesday
                DayOfWeek.THURSDAY -> thursday
                DayOfWeek.FRIDAY -> friday
                DayOfWeek.SATURDAY -> saturday
                DayOfWeek.SUNDAY -> sunday
            }

        override fun asUpdateRow() =
            DailyServiceTimeUpdateRow(
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

        override fun equalsIgnoringValidity(other: DailyServiceTimesValue): Boolean =
            other is IrregularTimes &&
                monday == other.monday &&
                tuesday == other.tuesday &&
                wednesday == other.wednesday &&
                thursday == other.thursday &&
                friday == other.friday &&
                saturday == other.saturday &&
                sunday == other.sunday

        fun isEmpty() =
            monday == null &&
                tuesday == null &&
                wednesday == null &&
                thursday == null &&
                friday == null &&
                saturday == null &&
                sunday == null

        override fun getTimesOnDate(date: LocalDate): TimeRange? {
            if (!validityPeriod.includes(date)) return null

            return timesForDayOfWeek(date.dayOfWeek)
        }
    }

    @JsonTypeName("VARIABLE_TIME")
    data class VariableTimes(override val validityPeriod: DateRange) :
        DailyServiceTimesValue(validityPeriod, DailyServiceTimesType.VARIABLE_TIME) {
        override fun asUpdateRow() =
            DailyServiceTimeUpdateRow(
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

        override fun equalsIgnoringValidity(other: DailyServiceTimesValue): Boolean =
            other is VariableTimes

        override fun getTimesOnDate(date: LocalDate): TimeRange? {
            return null
        }
    }

    fun withId(id: DailyServiceTimesId, childId: ChildId) = DailyServiceTimes(id, childId, this)
}

data class DailyServiceTimeRow(
    @PropagateNull val id: DailyServiceTimesId,
    val childId: ChildId,
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

fun Database.Read.getChildDailyServiceTimes(childId: ChildId): List<DailyServiceTimes> {
    @Suppress("DEPRECATION")
    return this.createQuery(
            """
SELECT
    id,
    child_id,
    validity_period,
    type,
    regular_times,
    monday_times,
    tuesday_times,
    wednesday_times,
    thursday_times,
    friday_times,
    saturday_times,
    sunday_times
FROM daily_service_time
WHERE child_id = :childId
ORDER BY lower(validity_period) DESC
        """
                .trimIndent()
        )
        .bind("childId", childId)
        .mapTo<DailyServiceTimeRow>()
        .useIterable { rows -> rows.map { toDailyServiceTimes(it) }.toList() }
}

fun Database.Read.getDailyServiceTimesForChildren(
    childIds: Set<ChildId>
): Map<ChildId, List<DailyServiceTimesValue>> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT
    id,
    child_id,
    validity_period,
    type,
    regular_times,
    monday_times,
    tuesday_times,
    wednesday_times,
    thursday_times,
    friday_times,
    saturday_times,
    sunday_times
FROM daily_service_time
WHERE child_id = ANY(:childIds)
        """
                .trimIndent()
        )
        .bind("childIds", childIds)
        .mapTo<DailyServiceTimeRow>()
        .useIterable { rows ->
            rows.map { toDailyServiceTimes(it) }.groupBy({ it.childId }, { it.times })
        }

data class DailyServiceTimesValidity(val childId: ChildId, val validityPeriod: DateRange)

fun Database.Read.getDailyServiceTimesValidity(
    id: DailyServiceTimesId
): DailyServiceTimesValidity? {
    @Suppress("DEPRECATION")
    return this.createQuery(
            """
SELECT child_id, validity_period
FROM daily_service_time
WHERE id = :id
        """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOneOrNull<DailyServiceTimesValidity>()
}

fun toDailyServiceTimes(row: DailyServiceTimeRow): DailyServiceTimes {
    return when (row.type) {
        DailyServiceTimesType.REGULAR ->
            DailyServiceTimesValue.RegularTimes(
                    validityPeriod = row.validityPeriod,
                    regularTimes =
                        row.regularTimes
                            ?: throw IllegalStateException(
                                "Regular daily service times must have regular times"
                            )
                )
                .withId(row.id, row.childId)
        DailyServiceTimesType.IRREGULAR ->
            DailyServiceTimesValue.IrregularTimes(
                    validityPeriod = row.validityPeriod,
                    monday = row.mondayTimes,
                    tuesday = row.tuesdayTimes,
                    wednesday = row.wednesdayTimes,
                    thursday = row.thursdayTimes,
                    friday = row.fridayTimes,
                    saturday = row.saturdayTimes,
                    sunday = row.sundayTimes
                )
                .withId(row.id, row.childId)
        DailyServiceTimesType.VARIABLE_TIME ->
            DailyServiceTimesValue.VariableTimes(validityPeriod = row.validityPeriod)
                .withId(row.id, row.childId)
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

fun Database.Transaction.updateChildDailyServiceTimes(
    id: DailyServiceTimesId,
    times: DailyServiceTimesValue
) {
    val sql =
        """
        UPDATE daily_service_time
        SET 
            validity_period = :validityPeriod,
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
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    this.createUpdate(sql).bindKotlin(times.asUpdateRow()).bind("id", id).execute()
}

fun Database.Transaction.addDailyServiceTimesNotification(
    today: LocalDate,
    id: DailyServiceTimesId,
    childId: ChildId,
    dateFrom: LocalDate,
    hasDeletedReservations: Boolean
) {
    val sql =
        """
WITH recipient AS (
    SELECT guardian_id AS id FROM guardian WHERE child_id = :childId
    UNION
    SELECT parent_id AS id FROM foster_parent WHERE child_id = :childId AND valid_during @> :today
)
INSERT INTO daily_service_time_notification (guardian_id, daily_service_time_id, date_from, has_deleted_reservations)
SELECT recipient.id, :id, :dateFrom, :hasDeletedReservations FROM recipient
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    this.createUpdate(sql)
        .bind("today", today)
        .bind("id", id)
        .bind("childId", childId)
        .bind("dateFrom", dateFrom)
        .bind("hasDeletedReservations", hasDeletedReservations)
        .execute()
}

fun Database.Transaction.deleteChildDailyServiceTimes(id: DailyServiceTimesId) {
    // language=sql
    val sql =
        """
        DELETE FROM daily_service_time
        WHERE id = :id
    """
            .trimIndent()

    @Suppress("DEPRECATION") this.createUpdate(sql).bind("id", id).execute()
}

fun Database.Transaction.createChildDailyServiceTimes(
    childId: ChildId,
    times: DailyServiceTimesValue
): DailyServiceTimesId {
    val sql =
        """
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
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bindKotlin(times.asUpdateRow())
        .bind("childId", childId)
        .exactlyOne<DailyServiceTimesId>()
}

data class DailyServiceTimesValidityWithId(
    val id: DailyServiceTimesId,
    val validityPeriod: DateRange
)

fun Database.Read.getOverlappingChildDailyServiceTimes(
    childId: ChildId,
    range: DateRange
): List<DailyServiceTimesValidityWithId> {
    // language=sql
    val sql =
        """
        SELECT id, validity_period
        FROM daily_service_time
        WHERE child_id = :childId
          AND :range && validity_period
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("childId", childId)
        .bind("range", range)
        .toList<DailyServiceTimesValidityWithId>()
}

fun Database.Transaction.updateChildDailyServiceTimesValidity(
    id: DailyServiceTimesId,
    validityPeriod: DateRange
) {
    val sql =
        """
        UPDATE daily_service_time
        SET validity_period = :validityPeriod
        WHERE id = :id
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    this.createUpdate(sql).bind("id", id).bind("validityPeriod", validityPeriod).execute()
}
