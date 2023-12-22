// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SSN_REGEXP } from 'lib-common/form-validation'
import LocalDate from 'lib-common/local-date'

import { DateRange, rangesOverlap } from '../date'

export const EMAIL_REGEX = /^([\w.%+-]+)@([\w-]+\.)+([\w]{2,})$/i

export const UUID_REGEX =
  /[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/

export const PHONE_REGEX = /^[0-9 \-+()]*$/

export const isEmailValid = (value: string) => EMAIL_REGEX.test(value)

export const isPhoneValid = (value: string) => PHONE_REGEX.test(value)

export const isSsnValid = (value: string) => SSN_REGEXP.test(value)

export const timeRegex = /^(([0-1][0-9])|(2[0-4])):[0-5][0-9]$/

export const isTimeValid = (value: string) => timeRegex.test(value)

export const allPropertiesTrue = (obj: Record<string, unknown>) =>
  Object.values(obj).every((propValue) => propValue === true)

export function isDateRangeInverted(dateRange: DateRange): boolean {
  return (
    dateRange.endDate.isBefore(dateRange.startDate) &&
    !dateRange.endDate.isEqual(dateRange.startDate)
  )
}

export function isDateRangeOverlappingWithExisting(
  dateRange: DateRange,
  existing: DateRange[]
): boolean {
  return !!existing.find((ex: DateRange) => rangesOverlap(dateRange, ex))
}

export const isDateRangeValid = (
  startDate: LocalDate,
  endDate: LocalDate | null
) => !endDate || !endDate.isBefore(startDate)

export type FieldErrors = Record<string, boolean>

export const fieldHasErrors = (fieldErrors: FieldErrors) =>
  Object.values(fieldErrors).includes(true)

export type FormErrors = Record<string, FieldErrors | boolean>

export const formHasErrors = (formErrors: FormErrors) =>
  Object.values(formErrors)
    .map((e) => (typeof e == 'boolean' ? e : fieldHasErrors(e)))
    .includes(true)
