// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { parse } from 'date-fns'

import LocalDate from 'lib-common/local-date'

export const SSN_REGEXP =
  /^[0-9]{6}[-+ABCDEFUVWXY][0-9]{3}[0-9ABCDEFHJKLMNPRSTUVWXY]$/

export const TIME_REGEXP = /^(?:[0-1][0-9]|2[0-3]):[0-5][0-9]$/

export const PHONE_REGEXP = /^[0-9 \-+()]{6,20}$/

export const EMAIL_REGEXP = /^\S+@\S+$/

export type ErrorKey =
  | 'required'
  | 'requiredSelection'
  | 'format'
  | 'integerFormat'
  | 'ssn'
  | 'phone'
  | 'email'
  | 'validDate'
  | 'dateTooEarly'
  | 'dateTooLate'
  | 'timeFormat'
  | 'timeRequired'
  | 'unitNotSelected'
  | 'preferredStartDate'
  | 'emailsDoNotMatch'
  | 'httpUrl'
  | 'openAttendance'
  | 'generic'

export const required = (
  val: unknown,
  err: ErrorKey = 'required'
): ErrorKey | undefined =>
  val === undefined ||
  val === null ||
  (typeof val === 'string' && val.trim().length === 0)
    ? err
    : undefined

export const requiredSelection = <T>(
  val: T | null | undefined,
  err: ErrorKey = 'requiredSelection'
): ErrorKey | undefined => (val === null || val === undefined ? err : undefined)

export const regexp = (
  val: string,
  exp: RegExp,
  err: ErrorKey = 'format'
): ErrorKey | undefined => (val.length > 0 && !exp.test(val) ? err : undefined)

export const ssn = (
  val: string,
  err: ErrorKey = 'ssn'
): ErrorKey | undefined => {
  if (val.length === 0) {
    return undefined
  }

  if (!SSN_REGEXP.test(val)) {
    return err
  }

  if (isNaN(parse(val.slice(0, 6), 'ddMMyy', new Date()).valueOf())) {
    return err
  }

  const remainder = parseInt(val.slice(0, 6) + val.slice(7, 10)) % 31
  const checkChars = '0123456789ABCDEFHJKLMNPRSTUVWXY'
  if (checkChars[remainder] !== val[10]) {
    return err
  }

  return undefined
}

export const phone = (
  val: string,
  err: ErrorKey = 'phone'
): ErrorKey | undefined =>
  val.length > 0 && !PHONE_REGEXP.test(val) ? err : undefined

export const email = (
  val: string,
  err: ErrorKey = 'email'
): ErrorKey | undefined =>
  val.length > 0 && !EMAIL_REGEXP.test(val) ? err : undefined

export const time = (val: string, err: ErrorKey = 'timeFormat') =>
  val.length > 0 && !TIME_REGEXP.test(val) ? err : undefined

export const validInt = (val: string, err: ErrorKey = 'integerFormat') =>
  regexp(val, /^[0-9]+$/, err)

export const validDate = (
  val: string,
  err: ErrorKey = 'validDate'
): ErrorKey | undefined => (!LocalDate.parseFiOrNull(val) ? err : undefined)

export const emailVerificationCheck =
  (verification: string): StandardValidator<string> =>
  (val, err: ErrorKey = 'emailsDoNotMatch') =>
    val === verification ? undefined : err

export const httpUrl = (
  val: string,
  err: ErrorKey = 'httpUrl'
): ErrorKey | undefined => {
  try {
    const url = new URL(val)
    const isValidHttpUrl = url.protocol === 'http:' || url.protocol === 'https:'
    return isValidHttpUrl ? undefined : err
  } catch (e) {
    return err
  }
}

type StandardValidator<T> = (val: T, err?: ErrorKey) => ErrorKey | undefined

export const validate = <T = string>(
  val: T,
  ...validators: StandardValidator<T>[]
): ErrorKey | undefined => {
  for (const validator of validators) {
    const err = validator(val)
    if (err) return err
  }
  return undefined
}

export const validateIf = <T = string>(
  condition: boolean,
  val: T,
  ...validators: StandardValidator<T>[]
): ErrorKey | undefined => {
  if (condition) return validate(val, ...validators)
  return undefined
}

export const throwIfNull = <T>(value: T | null) => {
  if (value === null) {
    throw new Error('Value cannot be null')
  }

  return value
}

type ArrayElement<ArrayType extends readonly unknown[]> = ArrayType[number]

type ErrorsOfArray<T extends unknown[]> = {
  arrayErrors: ErrorKey | undefined
  itemErrors: ErrorsOf<ArrayElement<T>>[]
}

export type ErrorsOf<T> = {
  [key in keyof T]?: T[key] extends unknown[]
    ? ErrorsOfArray<T[key]>
    : ErrorKey | undefined
}

function isArrayErrors<T extends unknown[]>(
  e: ErrorKey | undefined | ErrorsOfArray<T>
): e is ErrorsOfArray<T> {
  return e !== undefined && typeof e !== 'string'
}

function getArrayErrorCount<T extends unknown[]>(
  errors: ErrorsOfArray<T>
): number {
  const arrayErrors = errors.arrayErrors ? 1 : 0
  const itemErrors = errors.itemErrors.reduce(
    (acc, item) => acc + getErrorCount(item),
    0
  )
  return arrayErrors + itemErrors
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function getErrorCount<E extends ErrorsOf<any>>(errors: E): number {
  if (typeof errors !== 'object') {
    return 0
  }

  return (Object.keys(errors) as (keyof ErrorsOf<E>)[]).reduce<number>(
    (acc, key) => {
      const entry = errors[key]
      const isArr = isArrayErrors(entry)
      return acc + (isArr ? getArrayErrorCount(entry) : entry ? 1 : 0)
    },
    0
  )
}
