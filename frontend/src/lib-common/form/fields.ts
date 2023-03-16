// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from '../finite-date-range'
import LocalDate from '../local-date'
import LocalTime from '../local-time'

import { mapped, object, transformed, value } from './form'
import {
  ObjectFieldError,
  ValidationError,
  ValidationResult,
  ValidationSuccess
} from './types'

export const string = mapped(value<string>(), (s) => s.trim())
export const boolean = value<boolean>()
export const number = value<number>()

// TODO: This is always required for now, because <DatePicker /> manages validation errors internally
export const localDate = transformed(
  value<LocalDate | null>(),
  (s): ValidationResult<LocalDate, ObjectFieldError> =>
    s !== null ? ValidationSuccess.of(s) : ValidationError.objectFieldError
)

export const localDateRange = transformed(
  object({
    startDate: localDate,
    endDate: localDate
  }),
  ({ startDate, endDate }) => {
    if (startDate === undefined && endDate === undefined) {
      return ValidationSuccess.of(undefined)
    }
    if (
      startDate === undefined ||
      endDate === undefined ||
      endDate.isBefore(startDate)
    ) {
      return ValidationError.of('timeFormat')
    } else {
      return ValidationSuccess.of(new FiniteDateRange(startDate, endDate))
    }
  }
)

export const localTime = transformed(
  string,
  (s): ValidationResult<LocalTime | undefined, 'timeFormat'> => {
    if (s === '') return ValidationSuccess.of(undefined)
    const parsed = LocalTime.tryParse(s)
    if (parsed === undefined) {
      return ValidationError.of('timeFormat')
    }
    return ValidationSuccess.of(parsed)
  }
)

export const localTimeRange = transformed(
  object({
    startTime: localTime,
    endTime: localTime
  }),
  ({
    startTime,
    endTime
  }): ValidationResult<
    { startTime: LocalTime; endTime: LocalTime } | undefined,
    ObjectFieldError | 'timeFormat'
  > => {
    if (startTime === undefined && endTime === undefined) {
      return ValidationSuccess.of(undefined)
    }
    if (
      startTime === undefined ||
      endTime === undefined ||
      endTime.isBefore(startTime)
    ) {
      return ValidationError.of('timeFormat')
    } else {
      return ValidationSuccess.of({ startTime, endTime })
    }
  }
)
