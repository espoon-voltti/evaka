// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isValidIdentityNumber, isValidTimeString } from './validators'

export type ValidationState = 'error' | 'success'

export class ValidationError {
  constructor(
    public readonly key,
    public readonly message,
    public readonly validationName,
    public readonly simpleMessage = message,
    public readonly kind: ValidationState = 'error'
  ) {}
}

export class ValidationSuccess {
  constructor(
    public readonly key,
    public readonly kind: ValidationState = 'success'
  ) {}
}

export type Validation = ValidationError | ValidationSuccess

export type Validator<T = string, M = string, V = string> = (
  name: T,
  validationName: M
) => (value: V) => Validation

export const date: Validator = (name, validationName) => (value) =>
  Number.isFinite(Date.parse(value))
    ? new ValidationSuccess(name)
    : new ValidationError(name, 'validation.errors.date', validationName)

export const postalCode: Validator = (name, validationName) => (value) =>
  /^[0-9]{5}$/.test(value) || value === ''
    ? new ValidationSuccess(name)
    : new ValidationError(name, 'validation.errors.postalcode', validationName)

export const required: Validator<string, string, string | boolean> = (
  name,
  validationName
) => (value) =>
  !!value
    ? new ValidationSuccess(name)
    : new ValidationError(
        name,
        'validation.errors.required-field',
        validationName,
        'validation.errors.is-required-field'
      )

export const numeric: Validator<string, string, string | number> = (
  name,
  validationName
) => (value) =>
  !!Number.isFinite(Number(value))
    ? new ValidationSuccess(name)
    : new ValidationError(name, 'validation.errors.not-numeric', validationName)

export const email: Validator = (name, validationName) => (value) => {
  if (
    (value && value.includes('@') && value.includes('.')) ||
    value.length === 0
  ) {
    return new ValidationSuccess(name)
  }
  return new ValidationError(
    name,
    'validation.errors.invalid-email',
    validationName
  )
}

export const tel: Validator = (name, validationName) => (value) =>
  /^\+?\d{6,}$/.test(value) || !value
    ? new ValidationSuccess(name)
    : new ValidationError(name, 'validation.errors.invalid-tel', validationName)

export const ssn: Validator = (name, validationName) => (value) =>
  isValidIdentityNumber(value)
    ? new ValidationSuccess(name)
    : new ValidationError(name, 'validation.errors.invalid-ssn', validationName)

export const timeString: Validator = (name, validationName) => (value) =>
  isValidTimeString(value)
    ? new ValidationSuccess(name)
    : new ValidationError(
        name,
        'validation.errors.invalid-timestring',
        validationName
      )

export const validate = (
  name,
  validationName,
  value: any,
  v: Validator[] | undefined
) => (v ? v.map((validator) => validator(name, validationName)(value)) : [])
