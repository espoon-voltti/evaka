// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.time.LocalTime

fun convertMidnightEndTime(timeRange: TimeRange) =
    if (timeRange.endTime == LocalTime.of(0, 0).withNano(0).withSecond(0))
        timeRange.copy(endTime = LocalTime.of(23, 59))
    else timeRange

fun validateReservationTimeRange(timeRange: TimeRange) {
    if (timeRange.endTime <= timeRange.startTime) {
        throw BadRequest("Reservation start (${timeRange.startTime}) must be before end (${timeRange.endTime})")
    }
}

data class DailyReservationRequest(
    val childId: ChildId,
    val date: LocalDate,
    val reservations: List<TimeRange>?
)

fun List<DailyReservationRequest>.validate(reservableDates: List<FiniteDateRange>? = null): List<DailyReservationRequest> {
    if (reservableDates != null && this.any { request -> !reservableDates.any { it.includes(request.date) } }) {
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
}

@JsonSerialize(using = OpenTimeRangeSerializer::class)
data class OpenTimeRange(
    val startTime: LocalTime,
    val endTime: LocalTime?,
)

private fun padWithZeros(hoursOrMinutes: Int) = hoursOrMinutes.toString().padStart(2, '0')
private fun LocalTime.format() = "${padWithZeros(hour)}:${padWithZeros(minute)}"
class OpenTimeRangeSerializer : JsonSerializer<OpenTimeRange>() {
    override fun serialize(value: OpenTimeRange, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("startTime", value.startTime.format())
        gen.writeObjectField("endTime", value.endTime?.format())
        gen.writeEndObject()
    }
}

fun createReservations(tx: Database.Transaction, userId: EvakaUserId, reservations: List<DailyReservationRequest>) {
    tx.clearOldCitizenEditableAbsences(reservations.filter { it.reservations != null }.map { it.childId to it.date })
    tx.clearOldReservations(reservations.map { it.childId to it.date })
    tx.insertValidReservations(userId, reservations)
}
