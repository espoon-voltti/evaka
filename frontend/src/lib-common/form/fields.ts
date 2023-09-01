// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from '../date-range'
import FiniteDateRange from '../finite-date-range'
import { TimeRange } from '../generated/api-types/shared'
import LocalDate from '../local-date'
import LocalTime from '../local-time'

import { mapped, object, transformed, value } from './form'
import {
  AnyForm,
  FieldErrors,
  Form,
  ShapeOf,
  StateOf,
  ValidationError,
  ValidationResult,
  ValidationSuccess
} from './types'

export type FieldType<F extends () => AnyForm> = Form<
  unknown,
  string,
  StateOf<ReturnType<F>>,
  ShapeOf<ReturnType<F>>
>

export const string = () => mapped(value<string>(), (s) => s.trim())
export const boolean = () => value<boolean>()
export const number = () => value<number>()

export interface LocalDateConfig {
  minDate?: LocalDate | undefined
  maxDate?: LocalDate | undefined
}

const validateLocalDate = (
  value: string,
  config: LocalDateConfig | undefined
): ValidationResult<
  LocalDate,
  'timeFormat' | 'dateTooEarly' | 'dateTooLate'
> => {
  const date = LocalDate.parseFiOrNull(value)
  if (date === null) return ValidationError.of('timeFormat')
  if (config?.minDate && date.isBefore(config.minDate)) {
    return ValidationError.of('dateTooEarly')
  }
  if (config?.maxDate && date.isAfter(config.maxDate)) {
    return ValidationError.of('dateTooLate')
  }
  return ValidationSuccess.of(date)
}

export const localDate2 = () =>
  transformed(
    object({
      value: string(),
      config: value<LocalDateConfig | undefined>()
    }),
    ({ value, config }) => {
      if (value === '') return ValidationSuccess.of(undefined)
      return validateLocalDate(value, config)
    }
  )

export type LocalDate2Field = FieldType<typeof localDate2>

export const localDateRange2 = () =>
  transformed(
    object({
      start: string(),
      end: string(),
      config: value<LocalDateConfig | undefined>()
    }),
    ({
      start,
      end,
      config
    }): ValidationResult<
      FiniteDateRange | undefined,
      'required' | 'timeFormat' | 'dateTooEarly' | 'dateTooLate'
    > => {
      if (start === '' && end === '') return ValidationSuccess.of(undefined)

      const startDateResult = validateLocalDate(start, config)
      const endDateResult = validateLocalDate(end, config)
      if (!startDateResult.isValid || !endDateResult.isValid) {
        const errors: FieldErrors<
          'required' | 'timeFormat' | 'dateTooEarly' | 'dateTooLate'
        > = {}
        if (start === '') {
          errors.start = 'required'
        } else if (!startDateResult.isValid) {
          errors.start = startDateResult.error
        }
        if (end === '') {
          errors.end = 'required'
        } else if (!endDateResult.isValid) {
          errors.end = endDateResult.error
        }
        return ValidationError.fromFieldErrors(errors)
      }

      const startDate = startDateResult.value
      const endDate = endDateResult.value

      if (endDate.isBefore(startDate)) {
        return ValidationError.of('timeFormat')
      }

      return ValidationSuccess.of(new FiniteDateRange(startDate, endDate))
    }
  )

export type LocalDateRange2Field = FieldType<typeof localDateRange2>

export const openEndedLocalDateRange = () =>
  transformed(
    object({
      start: string(),
      end: string(),
      config: value<LocalDateConfig | undefined>()
    }),
    ({ start, end, config }) => {
      if (start === '' && end === '') return ValidationSuccess.of(undefined)

      const startDateResult = validateLocalDate(start, config)
      const endDateResult =
        end === '' ? ValidationSuccess.of(null) : validateLocalDate(end, config)
      if (!startDateResult.isValid || !endDateResult.isValid) {
        const errors: FieldErrors<
          'required' | 'timeFormat' | 'dateTooEarly' | 'dateTooLate'
        > = {}
        if (start === '') {
          errors.start = 'required'
        } else if (!startDateResult.isValid) {
          errors.start = startDateResult.error
        }
        if (!endDateResult.isValid) {
          errors.end = endDateResult.error
        }
        return ValidationError.fromFieldErrors(errors)
      }

      const startDate = startDateResult.value
      const endDate = endDateResult.value

      if (endDate !== null && endDate.isBefore(startDate)) {
        return ValidationError.of('timeFormat')
      }

      return ValidationSuccess.of(new DateRange(startDate, endDate))
    }
  )

export const localTime = transformed(
  string(),
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
  }): ValidationResult<TimeRange | undefined, 'timeFormat'> => {
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
      return ValidationSuccess.of({ start: startTime, end: endTime })
    }
  }
)
