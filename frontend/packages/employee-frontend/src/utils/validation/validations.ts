// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '@evaka/lib-common/src/local-date'
import { DateRange, rangesOverlap } from '../../utils/date'

export const EMAIL_REGEX = /^([\w.%+-]+)@([\w-]+\.)+([\w]{2,})$/i

export const PHONE_REGEX = /^[0-9 \-+()]*$/

export const isEmailValid = (value: string) => EMAIL_REGEX.test(value)

export const isPhoneValid = (value: string) => PHONE_REGEX.test(value)

export const ssnRegex = /^\d{6}[A+-]\d{3}[\dA-Z]$/

export const isSsnValid = (value: string) => ssnRegex.test(value)

export const timeRegex = /^(([0-1][0-9])|(2[0-4])):[0-5][0-9]$/

export const isTimeValid = (value: string) => timeRegex.test(value)

export const allPropertiesTrue = (obj: Record<string, unknown>) => {
  return Object.values(obj).every((propValue) => propValue === true)
}

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
) => {
  return !endDate || !endDate.isBefore(startDate)
}

export interface FieldErrors {
  [error: string]: boolean
}

export const fieldHasErrors = (fieldErrors: FieldErrors) =>
  Object.values(fieldErrors).includes(true)

export interface FormErrors {
  [fieldOrError: string]: FieldErrors | boolean
}

export const formHasErrors = (formErrors: FormErrors) =>
  Object.values(formErrors)
    .map((e) => (typeof e == 'boolean' ? e : fieldHasErrors(e)))
    .includes(true)
