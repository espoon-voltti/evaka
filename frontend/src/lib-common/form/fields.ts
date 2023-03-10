// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalTime from '../local-time'

import { mapped, object, transformed, value } from './form'
import {
  ObjectFieldError,
  ValidationError,
  ValidationResult,
  ValidationSuccess
} from './types'

export const string = () => mapped(value<string>(), (s) => s.trim())
export const boolean = () => value<boolean>()
export const number = () => value<number>()

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
      // Allow midnight as the end time, even though it's "before" all other times
      (endTime.isBefore(startTime) && !endTime.isEqual(midnight))
    ) {
      return ValidationError.of('timeFormat')
    } else {
      return ValidationSuccess.of({ startTime, endTime })
    }
  }
)

const midnight = LocalTime.of(0, 0)
