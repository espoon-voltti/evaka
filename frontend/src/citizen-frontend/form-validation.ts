import LocalDate from '@evaka/lib-common/local-date'
import { InputInfo } from '@evaka/lib-components/atoms/form/InputField'
import { Translations } from './localization'
import { parse } from 'date-fns'

export const SSN_REGEXP = /^[0-9]{6}[+\-A][0-9]{3}[0-9ABCDEFHJKLMNPRSTUVWXY]$/

export const TIME_REGEXP = /^(?:[0-1][0-9]|2[0-3]):[0-5][0-9]$/

export const PHONE_REGEXP = /^[0-9 \-+()]{6,20}$/

export const EMAIL_REGEXP = /^\S+@\S+$/

export type ErrorKey =
  | 'required'
  | 'requiredSelection'
  | 'format'
  | 'ssn'
  | 'phone'
  | 'email'
  | 'validDate'
  | 'timeFormat'
  | 'unitNotSelected'
  | 'preferredStartDate'

export const required = (
  val: string,
  err: ErrorKey = 'required'
): ErrorKey | undefined => (val.trim().length === 0 ? err : undefined)

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

export const validDate = (
  val: string,
  err: ErrorKey = 'validDate'
): ErrorKey | undefined => (!LocalDate.parseFiOrNull(val) ? err : undefined)

type StandardValidator = (val: string, err?: ErrorKey) => ErrorKey | undefined
export const validate = (
  val: string,
  ...validators: StandardValidator[]
): ErrorKey | undefined => {
  for (const validator of validators) {
    const err = validator(val)
    if (err) return err
  }
  return undefined
}

type ArrayElement<ArrayType extends readonly unknown[]> = ArrayType[number]

// eslint-disable-next-line @typescript-eslint/ban-types
type ErrorsOfArray<T extends object[]> = {
  arrayErrors: ErrorKey | undefined
  itemErrors: ErrorsOf<ArrayElement<T>>[]
}

// eslint-disable-next-line @typescript-eslint/ban-types
export type ErrorsOf<T extends object> = {
  // eslint-disable-next-line @typescript-eslint/ban-types
  [key in keyof T]?: T[key] extends object[]
    ? ErrorsOfArray<T[key]>
    : ErrorKey | undefined
}

export function errorToInputInfo(
  error: ErrorKey | undefined,
  localization: Translations['validationErrors']
): InputInfo | undefined {
  return (
    error && {
      text: localization[error],
      status: 'warning'
    }
  )
}

// eslint-disable-next-line @typescript-eslint/ban-types
function isArrayErrors<T extends object[]>(
  e: ErrorKey | undefined | ErrorsOfArray<T>
): e is ErrorsOfArray<T> {
  return e !== undefined && typeof e !== 'string'
}

// eslint-disable-next-line @typescript-eslint/ban-types
function getArrayErrorCount<T extends object[]>(
  errors: ErrorsOfArray<T>
): number {
  const arrayErrors = errors.arrayErrors ? 1 : 0
  const itemErrors = errors.itemErrors.reduce(
    (acc, item) => acc + getErrorCount(item),
    0
  )
  return arrayErrors + itemErrors
}

// eslint-disable-next-line @typescript-eslint/ban-types
export function getErrorCount<T extends object>(errors: ErrorsOf<T>): number {
  return Object.keys(errors).reduce<number>((acc, key) => {
    const isArr = isArrayErrors(errors[key])
    return acc + (isArr ? getArrayErrorCount(errors[key]) : errors[key] ? 1 : 0)
  }, 0)
}
