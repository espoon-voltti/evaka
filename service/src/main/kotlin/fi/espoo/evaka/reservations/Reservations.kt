// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.time.LocalTime

data class DailyReservationRequest(
    val childId: ChildId,
    val date: LocalDate,
    val reservations: List<TimeRange>?
)

fun List<DailyReservationRequest>.validate(reservableDates: FiniteDateRange? = null): List<DailyReservationRequest> {
    if (reservableDates != null && this.any { !reservableDates.includes(it.date) }) {
        throw BadRequest("Some days are not reservable", "NON_RESERVABLE_DAYS")
    }

    return this.map {
        it.copy(reservations = it.reservations?.map(::convertMidnightEndTime)?.onEach(::validateReservationTimeRange))
    }
}

@JsonSerialize(using = TimeRangeSerializer::class)
data class TimeRange(
    val startTime: LocalTime,
    val endTime: LocalTime,
)

class TimeRangeSerializer : JsonSerializer<TimeRange>() {
    override fun serialize(value: TimeRange, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("startTime", value.startTime.format())
        gen.writeObjectField("endTime", value.endTime.format())
        gen.writeEndObject()
    }

    private fun LocalTime.format() = "${padWithZeros(hour)}:${padWithZeros(minute)}"
    private fun padWithZeros(hoursOrMinutes: Int) = hoursOrMinutes.toString().padStart(2, '0')
}

fun validateReservationTimeRange(timeRange: TimeRange) {
    if (timeRange.endTime <= timeRange.startTime) {
        throw BadRequest("Reservation start (${timeRange.startTime}) must be before end (${timeRange.endTime})")
    }
}

fun convertMidnightEndTime(timeRange: TimeRange) =
    if (timeRange.endTime == LocalTime.of(0, 0).withNano(0).withSecond(0))
        timeRange.copy(endTime = LocalTime.of(23, 59))
    else timeRange

fun Database.Transaction.clearOldAbsences(childDatePairs: List<Pair<ChildId, LocalDate>>) {
    val batch = prepareBatch(
        "DELETE FROM absence WHERE child_id = :childId AND date = :date"
    )

    childDatePairs.forEach { (childId, date) ->
        batch.bind("childId", childId).bind("date", date).add()
    }

    batch.execute()
}

fun Database.Transaction.clearOldReservations(reservations: List<Pair<ChildId, LocalDate>>) {
    val batch = prepareBatch("DELETE FROM attendance_reservation WHERE child_id = :childId AND date = :date")

    reservations.forEach { (childId, date) ->
        batch
            .bind("childId", childId)
            .bind("date", date)
            .add()
    }

    batch.execute()
}
