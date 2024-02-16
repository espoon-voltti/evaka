// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Reservation } from './generated/api-types/reservations'
import LocalTime from './local-time'
import TimeInterval from './time-interval'

export function reservationHasTimes(
  reservation: Reservation
): reservation is Reservation.Times {
  return reservation.type === 'TIMES'
}

export type Repetition = 'DAILY' | 'WEEKLY' | 'IRREGULAR'

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
function attendanceTimeDiffers(
  first: LocalTime | null | undefined,
  second: LocalTime | null | undefined
): boolean {
  const thresholdMinutes = 15
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

    const matchingAttendance = attendances.find((attendance) =>
      attendance.overlaps(reservation.range)
    )

    if (!matchingAttendance) return false

    return (
      attendanceTimeDiffers(
        reservation.range.start.asLocalTime(),
        matchingAttendance.start.asLocalTime()
      ) ||
      attendanceTimeDiffers(
        matchingAttendance.end?.asLocalTime(),
        reservation.range.end.asLocalTime()
      )
    )
  })
}
