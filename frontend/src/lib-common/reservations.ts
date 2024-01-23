// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorKey, regexp, TIME_REGEXP } from './form-validation'
import {
  OpenTimeRange,
  Reservation,
  ReservationResponse
} from './generated/api-types/reservations'
import { JsonOf } from './json'
import LocalTime from './local-time'

export function reservationHasTimes(
  reservation: Reservation
): reservation is Reservation.Times {
  return reservation.type === 'TIMES'
}

export function reservationHasNoTimes(
  reservation: Reservation
): reservation is Reservation.Times {
  return reservation.type === 'NO_TIMES'
}

export type Repetition = 'DAILY' | 'WEEKLY' | 'IRREGULAR'

export interface TimeRange {
  startTime: LocalTime
  endTime: LocalTime
}

export interface TimeRangeErrors {
  startTime: ErrorKey | undefined
  endTime: ErrorKey | undefined
}

export function validateTimeRange(
  timeRange: JsonOf<TimeRange>,
  openEnd = false
): TimeRangeErrors {
  let startTime: ErrorKey | undefined
  if (timeRange.startTime === '' && timeRange.endTime !== '') {
    startTime = 'timeRequired'
  }
  startTime =
    startTime ?? regexp(timeRange.startTime, TIME_REGEXP, 'timeFormat')

  let endTime: ErrorKey | undefined
  if (!openEnd && timeRange.endTime === '' && timeRange.startTime !== '') {
    endTime = 'timeRequired'
  }
  endTime = endTime ?? regexp(timeRange.endTime, TIME_REGEXP, 'timeFormat')

  if (timeRange.startTime === '00:00' && timeRange.endTime === '00:00')
    endTime = 'timeFormat'

  if (
    !startTime &&
    !endTime &&
    timeRange.startTime !== '' &&
    timeRange.endTime !== '' &&
    timeRange.endTime !== '00:00' &&
    timeRange.endTime <= timeRange.startTime
  ) {
    endTime = 'timeFormat'
  }

  return { startTime, endTime }
}

export function parseReservationDto(
  reservation: JsonOf<ReservationResponse>
): ReservationResponse {
  if (reservation.type === 'TIMES') {
    return {
      ...reservation,
      startTime: LocalTime.parseIso(reservation.startTime),
      endTime: LocalTime.parseIso(reservation.endTime)
    }
  } else {
    return reservation
  }
}

function timeToMinutes(value: LocalTime): number {
  return value.hour * 60 + value.minute
}

/**
 * "Best effort" time difference calculation based on two "HH:mm" local time
 * strings without date part. This is only used for highlighting in the calendar
 * so edge cases are expected and might not matter much.
 *
 * The functionality can be improved once the reservation data model supports
 * reservations that span multiple days.
 */
export function attendanceTimeDiffers(
  first: LocalTime | null | undefined,
  second: LocalTime | null | undefined,
  thresholdMinutes = 15
): boolean {
  if (!(first && second)) {
    return false
  }
  return timeToMinutes(first) - timeToMinutes(second) > thresholdMinutes
}

export function reservationsAndAttendancesDiffer(
  reservations: Reservation[],
  attendances: OpenTimeRange[]
): boolean {
  return reservations.some((reservation) => {
    if (reservation.type === 'NO_TIMES') return false

    const matchingAttendance = attendances.find(
      (attendance) =>
        attendance.startTime <= reservation.endTime &&
        (!attendance.endTime || reservation.startTime <= attendance.endTime)
    )

    if (!matchingAttendance) return false

    return (
      attendanceTimeDiffers(
        reservation.startTime,
        matchingAttendance.startTime
      ) ||
      attendanceTimeDiffers(matchingAttendance.endTime, reservation.endTime)
    )
  })
}
