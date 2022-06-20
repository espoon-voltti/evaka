// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from './finite-date-range'
import { ErrorKey, regexp, TIME_REGEXP } from './form-validation'
import {
  DailyReservationRequest,
  OpenTimeRange,
  TimeRange
} from './generated/api-types/reservations'
import LocalDate from './local-date'
import { UUID } from './types'

export type Repetition = 'DAILY' | 'WEEKLY' | 'IRREGULAR'

export interface ReservationFormData {
  selectedChildren: UUID[]
  startDate: LocalDate | null
  endDate: LocalDate | null
  repetition: Repetition
  dailyTimes: TimeRanges
  weeklyTimes: Array<TimeRanges | undefined>
  irregularTimes: Record<string, TimeRanges | undefined>
}

export type TimeRanges = [TimeRange] | [TimeRange, TimeRange]

export interface TimeRangeErrors {
  startTime: ErrorKey | undefined
  endTime: ErrorKey | undefined
}

type ReservationErrors = Partial<
  Record<
    keyof Omit<
      ReservationFormData,
      'dailyTimes' | 'weeklyTimes' | 'irregularTimes'
    >,
    ErrorKey
  > & {
    dailyTimes: TimeRangeErrors[]
  } & {
    weeklyTimes: Array<TimeRangeErrors[] | undefined>
  } & {
    irregularTimes: Record<string, TimeRangeErrors[] | undefined>
  }
>

export type ValidationResult =
  | { errors: ReservationErrors }
  | { errors: undefined; requestPayload: DailyReservationRequest[] }

export type ReservationFormDataForValidation = Omit<
  ReservationFormData,
  'endDate' | 'startDate'
> & { endDate: LocalDate | null; startDate: LocalDate | null }

export function validateForm(
  reservableDays: FiniteDateRange[],
  { startDate, endDate, ...formData }: ReservationFormDataForValidation
): ValidationResult {
  const errors: ReservationErrors = {}

  if (formData.selectedChildren.length < 1) {
    errors['selectedChildren'] = 'required'
  }

  if (
    startDate === null ||
    !reservableDays.some((r) => r.includes(startDate))
  ) {
    errors['startDate'] = 'validDate'
  }

  if (endDate === null || !reservableDays.some((r) => r.includes(endDate))) {
    errors['endDate'] = 'validDate'
  } else if (startDate && endDate.isBefore(startDate)) {
    errors['endDate'] = 'dateTooEarly'
  }

  if (formData.repetition === 'DAILY') {
    errors['dailyTimes'] = formData.dailyTimes.map((timeRange) =>
      validateTimeRange(timeRange)
    )
  }

  if (formData.repetition === 'WEEKLY') {
    errors['weeklyTimes'] = formData.weeklyTimes.map((times) =>
      times ? times.map((timeRange) => validateTimeRange(timeRange)) : undefined
    )
  }

  if (formData.repetition === 'IRREGULAR') {
    errors['irregularTimes'] = Object.fromEntries(
      Object.entries(formData.irregularTimes).map(([date, times]) => [
        date,
        times
          ? times.map((timeRange) => validateTimeRange(timeRange))
          : undefined
      ])
    )
  }

  if (errorsExist(errors)) {
    return { errors }
  }

  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const dateRange = new FiniteDateRange(startDate!, endDate!)
  const dates = [...dateRange.dates()]

  return {
    errors: undefined,
    requestPayload: formData.selectedChildren.flatMap((childId) => {
      switch (formData.repetition) {
        case 'DAILY':
          return dates.map((date) => ({
            childId,
            date,
            reservations: filterEmpty(formData.dailyTimes)
          }))
        case 'WEEKLY':
          return dates.map((date) => ({
            childId,
            date,
            reservations: filterEmpty(
              formData.weeklyTimes[date.getIsoDayOfWeek() - 1]
            )
          }))
        case 'IRREGULAR':
          return Object.entries(formData.irregularTimes)
            .filter(([isoDate]) => {
              const date = LocalDate.tryParseIso(isoDate)
              return date && dateRange.includes(date)
            })
            .map(([isoDate, times]) => ({
              childId,
              date: LocalDate.parseIso(isoDate),
              reservations: filterEmpty(times)
            }))
      }
    })
  }
}

function filterEmpty(times: TimeRanges | undefined) {
  return times?.filter(({ startTime, endTime }) => startTime && endTime) ?? null
}

function errorsExist(errors: ReservationErrors): boolean {
  const {
    dailyTimes: dailyErrors,
    weeklyTimes: weeklyErrors,
    irregularTimes: shiftCareErrors,
    ...otherErrors
  } = errors

  for (const error of Object.values(otherErrors)) {
    if (error) return true
  }

  if (dailyErrors?.some((error) => error.startTime || error.endTime)) {
    return true
  }

  for (const errors of weeklyErrors ?? []) {
    if (errors?.some((error) => error.startTime || error.endTime)) return true
  }

  for (const errors of Object.values(shiftCareErrors ?? {})) {
    if (errors?.some((error) => error.startTime || error.endTime)) return true
  }

  return false
}

export function validateTimeRange(
  timeRange: TimeRange,
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

function timeToMinutes(expected: string): number {
  const [hours, minutes] = expected.split(':').map(Number)
  return hours * 60 + minutes
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
  first: string | null | undefined,
  second: string | null | undefined,
  thresholdMinutes = 15
): boolean {
  if (!(first && second)) {
    return false
  }
  return timeToMinutes(first) - timeToMinutes(second) > thresholdMinutes
}

export function reservationsAndAttendancesDiffer(
  reservations: TimeRange[],
  attendances: OpenTimeRange[]
): boolean {
  return reservations.some((reservation) => {
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
