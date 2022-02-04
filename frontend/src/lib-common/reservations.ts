// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from './finite-date-range'
import { ErrorKey, regexp, TIME_REGEXP } from './form-validation'
import {
  DailyReservationRequest,
  TimeRange
} from './generated/api-types/reservations'
import LocalDate from './local-date'
import { UUID } from './types'

export type Repetition = 'DAILY' | 'WEEKLY' | 'IRREGULAR'

export interface ReservationFormData {
  selectedChildren: UUID[]
  startDate: string
  endDate: string
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

export function validateForm(
  reservableDays: FiniteDateRange | null,
  formData: ReservationFormData
): ValidationResult {
  const errors: ReservationErrors = {}

  if (formData.selectedChildren.length < 1) {
    errors['selectedChildren'] = 'required'
  }

  const startDate = LocalDate.parseFiOrNull(formData.startDate)
  if (startDate === null || reservableDays === null) {
    errors['startDate'] = 'validDate'
  } else if (startDate.isBefore(reservableDays.start)) {
    errors['startDate'] = 'dateTooEarly'
  }

  const endDate = LocalDate.parseFiOrNull(formData.endDate)
  if (endDate === null || reservableDays === null) {
    errors['endDate'] = 'validDate'
  } else if (startDate && endDate.isBefore(startDate)) {
    errors['endDate'] = 'dateTooEarly'
  } else if (endDate.isAfter(reservableDays.end)) {
    errors['endDate'] = 'dateTooLate'
  }

  if (formData.repetition === 'DAILY') {
    errors['dailyTimes'] = formData.dailyTimes.map(validateTimeRange)
  }

  if (formData.repetition === 'WEEKLY') {
    errors['weeklyTimes'] = formData.weeklyTimes.map((times) =>
      times ? times.map(validateTimeRange) : undefined
    )
  }

  if (formData.repetition === 'IRREGULAR') {
    errors['irregularTimes'] = Object.fromEntries(
      Object.entries(formData.irregularTimes).map(([date, times]) => [
        date,
        times ? times.map(validateTimeRange) : undefined
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

export function validateTimeRange(timeRange: TimeRange): TimeRangeErrors {
  let startTime: ErrorKey | undefined
  if (timeRange.startTime === '' && timeRange.endTime !== '') {
    startTime = 'timeRequired'
  }
  startTime =
    startTime ?? regexp(timeRange.startTime, TIME_REGEXP, 'timeFormat')

  let endTime: ErrorKey | undefined
  if (timeRange.endTime === '' && timeRange.startTime !== '') {
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
