// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  TimeInterval,
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

export type Repetition = 'DAILY' | 'WEEKLY' | 'IRREGULAR'

export function parseReservation(
  reservation: JsonOf<Reservation>
): Reservation {
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
  attendances: TimeInterval[]
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
