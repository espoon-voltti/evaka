// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable @typescript-eslint/no-explicit-any */

export type Form<Output, Error extends string, State, Shape> = {
  validate: (state: State) => ValidationResult<Output, Error>
  shape: Shape
}

export type AnyForm = Form<any, string, any, any>

export type OutputOf<F extends Form<any, any, any, any>> = F extends Form<
  infer Output,
  any,
  any,
  any
>
  ? Output
  : never
export type ErrorOf<F extends Form<any, any, any, any>> = F extends Form<
  any,
  infer Error,
  any,
  any
>
  ? Error
  : never
export type StateOf<F extends Form<any, any, any, any>> = F extends Form<
  any,
  any,
  infer State,
  any
>
  ? State
  : never
export type ShapeOf<F extends AnyForm> = F extends Form<
  any,
  any,
  any,
  infer Shape
>
  ? Shape
  : never

export const ObjectFieldError = 'objectFieldError'
export type ObjectFieldError = typeof ObjectFieldError

export class ValidationSuccess<Output, Error> {
  readonly isValid = true

  private constructor(public value: Output) {}

  static of<Output>(value: Output): ValidationResult<Output, never> {
    return new ValidationSuccess(value)
  }

  map<T>(fn: (value: Output) => T): ValidationResult<T, Error> {
    return ValidationSuccess.of(fn(this.value))
  }

  chain<T, E>(
    fn: (value: Output) => ValidationResult<T, E>
  ): ValidationResult<T, Error | E> {
    return fn(this.value)
  }
}

export class ValidationError<Output, Error> {
  readonly isValid = false
  static objectFieldError: ValidationResult<never, ObjectFieldError> =
    ValidationError.of(ObjectFieldError)

  private constructor(public validationError: Error) {}

  static of<Error>(validationError: Error): ValidationResult<never, Error> {
    return new ValidationError(validationError)
  }

  map<T>(_fn: (value: Output) => T): ValidationError<T, Error> {
    return this as unknown as ValidationError<T, Error>
  }

  chain<T, E>(
    _fn: (value: Output) => ValidationResult<T, E>
  ): ValidationResult<T, Error | E> {
    return this as unknown as ValidationResult<T, Error | E>
  }
}

export type ValidationResult<Output, Error> =
  | ValidationSuccess<Output, Error>
  | ValidationError<Output, Error>
