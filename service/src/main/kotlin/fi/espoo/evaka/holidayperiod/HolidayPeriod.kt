// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import fi.espoo.evaka.reservations.ReservationEnabledPlacementRange
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.config.SealedSubclassSimpleName
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@JsonTypeIdResolver(SealedSubclassSimpleName::class)
sealed interface HolidayPeriodEffect {
    /** Reservations cannot be made: the holiday period is not yet open for reservations */
    data class NotYetReservable(val period: FiniteDateRange, val reservationsOpenOn: LocalDate) :
        HolidayPeriodEffect

    /**
     * Reservations are open: Can make PRESENT/ABSENT reservations, i.e. reservations without time
     */
    data object ReservationsOpen : HolidayPeriodEffect

    /**
     * Deadline has passed: Reservations cannot be made, but PRESENT reservations can be changed to
     * reservations with time
     */
    data object ReservationsClosed : HolidayPeriodEffect
}

data class HolidayPeriod(
    val id: HolidayPeriodId,
    val period: FiniteDateRange,
    val reservationsOpenOn: LocalDate,
    val reservationDeadline: LocalDate,
) {
    /**
     * Returns the effect of the holiday period for all dates inside `period`, when the *current*
     * date is `today`. Returns `null` if the holiday period doesn't have an effect.
     */
    fun effect(
        today: LocalDate,
        reservationEnabledPlacementRanges: List<ReservationEnabledPlacementRange>,
    ): HolidayPeriodEffect? =
        when {
            reservationEnabledPlacementRanges.none {
                it.range.overlaps(FiniteDateRange(reservationsOpenOn, reservationDeadline))
            } -> null
            today < reservationsOpenOn ->
                HolidayPeriodEffect.NotYetReservable(period, reservationsOpenOn)
            today in reservationsOpenOn..reservationDeadline -> HolidayPeriodEffect.ReservationsOpen
            // Reservation period is over and a placement was active when it was still open
            else -> {
                val holidayPeriodPlacements =
                    reservationEnabledPlacementRanges
                        .filter { it.range.overlaps(period) }
                        .sortedBy { it.range.start }
                val hasRelevantGap =
                    holidayPeriodPlacements.zipWithNext().any { (first, second) ->
                        val hasGap = first.range.end.plusDays(1) != second.range.start
                        hasGap && second.created > reservationDeadline
                    }
                if (hasRelevantGap) {
                    // Gap between placements AND the later placement was created after reservation
                    // period => no effect
                    null
                } else {
                    HolidayPeriodEffect.ReservationsClosed
                }
            }
        }
}

data class HolidayPeriodCreate(
    val period: FiniteDateRange,
    val reservationsOpenOn: LocalDate,
    val reservationDeadline: LocalDate,
)

data class HolidayPeriodUpdate(
    val reservationsOpenOn: LocalDate,
    val reservationDeadline: LocalDate,
)
