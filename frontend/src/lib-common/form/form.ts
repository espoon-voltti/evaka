// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { memoizeLast } from './memoize'
import {
  AnyForm,
  ErrorOf,
  FieldErrors,
  Form,
  OutputOf,
  StateOf,
  ValidationError,
  ValidationResult,
  ValidationSuccess
} from './types'

export function value<T>(): Form<T, never, T, unknown> {
  return {
    validate: memoizeLast((state: T) => ValidationSuccess.of(state)),
    shape: () => undefined
  }
}

type AnyObjectFields = Record<string, AnyForm>
type ObjectOutput<Fields extends AnyObjectFields> = {
  [K in keyof Fields]: OutputOf<Fields[K]>
}
type ObjectError<Fields extends AnyObjectFields> = ErrorOf<Fields[keyof Fields]>
type ObjectState<Fields extends AnyObjectFields> = {
  [K in keyof Fields]: StateOf<Fields[K]>
}

export function object<Fields extends AnyObjectFields>(
  fields: Fields
): Form<
  ObjectOutput<Fields>,
  ObjectError<Fields>,
  ObjectState<Fields>,
  Fields
> {
  return {
    validate: memoizeLast(
      (
        state: ObjectState<Fields>
      ): ValidationResult<ObjectOutput<Fields>, ObjectError<Fields>> => {
        const valid = {} as ObjectOutput<Fields>
        let fieldErrors: FieldErrors<ObjectError<Fields>> | undefined =
          undefined
        Object.entries(fields).forEach(([k, field]) => {
          const validationResult = field.validate(state[k]) as ValidationResult<
            OutputOf<Fields[keyof Fields]>,
            ObjectError<Fields>
          >
          if (validationResult.isValid) {
            if (!fieldErrors) {
              valid[k as keyof Fields] = validationResult.value
            }
          } else {
            if (!fieldErrors) fieldErrors = {}
            fieldErrors[k] = validationResult.error
          }
        })
        if (fieldErrors) return ValidationError.fromFieldErrors(fieldErrors)
        return ValidationSuccess.of(valid)
      }
    ),
    shape: () => fields
  }
}

export function array<Elem extends AnyForm>(
  elem: Elem
): Form<OutputOf<Elem>[], ErrorOf<Elem>, StateOf<Elem>[], Elem> {
  return {
    validate: memoizeLast(
      (
        state: StateOf<Elem>[]
      ): ValidationResult<OutputOf<Elem>[], ErrorOf<Elem>> => {
        const valid: OutputOf<Elem>[] = []
        let fieldErrors: FieldErrors<ErrorOf<Elem>> | undefined = undefined
        state.forEach((elemState, index) => {
          const validationResult = elem.validate(elemState) as ValidationResult<
            OutputOf<Elem>,
            ErrorOf<Elem>
          >
          if (validationResult.isValid) {
            if (!fieldErrors) {
              valid.push(validationResult.value)
            }
          } else {
            if (!fieldErrors) fieldErrors = {}
            fieldErrors[index.toString()] = validationResult.error
          }
        })
        if (fieldErrors) return ValidationError.fromFieldErrors(fieldErrors)
        return ValidationSuccess.of(valid)
      }
    ),
    shape: () => elem
  }
}

export function recursive<Output, Error extends string, State, Shape>(
  fn: () => Form<Output, Error, State, Shape>
): Form<Output, Error, State, Shape> {
  let form: Form<Output, Error, State, Shape> | undefined = undefined

  const get = () => {
    if (!form) form = fn()
    return form
  }

  return {
    validate: memoizeLast((state: State) => get().validate(state)),
    shape: () => get().shape()
  }
}

type UnionOutput<
  Fields extends AnyObjectFields,
  K extends keyof Fields
> = K extends unknown // trigger distributive conditional types
  ? {
      branch: K
      value: OutputOf<Fields[K]>
    }
  : never

type UnionError<Fields extends AnyObjectFields> = ErrorOf<Fields[keyof Fields]>

type UnionState<
  Fields extends AnyObjectFields,
  K extends keyof Fields
> = K extends unknown // trigger distributive conditional types
  ? {
      branch: K
      state: StateOf<Fields[K]>
    }
  : never

export function union<Fields extends AnyObjectFields>(
  fields: Fields
): Form<
  UnionOutput<Fields, keyof Fields>,
  UnionError<Fields>,
  UnionState<Fields, keyof Fields>,
  Fields
> {
  return {
    validate: memoizeLast((state: UnionState<Fields, keyof Fields>) => {
      const activeBranch = fields[state.branch as keyof Fields]
      const validationResult = activeBranch.validate(
        state.state
      ) as ValidationResult<
        UnionOutput<Fields, keyof Fields>,
        UnionError<Fields>
      >
      if (validationResult.isValid) {
        return ValidationSuccess.of({
          branch: state.branch,
          value: validationResult.value
        } as UnionOutput<Fields, keyof Fields>)
      } else {
        return ValidationError.fromFieldErrors({
          [state.branch]: validationResult.error
        })
      }
    }),
    shape: () => fields
  }
}

export function chained<
  Output,
  Error extends string,
  State,
  Shape,
  VOutput,
  VError extends string
>(
  form: Form<Output, Error, State, Shape>,
  mapper: (
    form: Form<Output, Error, State, Shape>,
    state: State
  ) => ValidationResult<VOutput, VError>
): Form<VOutput, Error | VError, State, Shape> {
  return {
    validate: memoizeLast((state: State) => mapper(form, state)),
    shape: form.shape
  }
}

export function transformed<
  Output,
  Error extends string,
  State,
  Shape,
  VOutput,
  VError extends string
>(
  form: Form<Output, Error, State, Shape>,
  transform: (output: Output) => ValidationResult<VOutput, VError>
): Form<VOutput, Error | VError, State, Shape> {
  return {
    validate: memoizeLast((state: State) =>
      form.validate(state).chain(transform)
    ),
    shape: form.shape
  }
}

export function mapped<Output, Error extends string, State, Shape, VOutput>(
  form: Form<Output, Error, State, Shape>,
  map: (output: Output) => VOutput
): Form<VOutput, Error, State, Shape> {
  return transformed(form, (output) => ValidationSuccess.of(map(output)))
}

export function validated<
  Output,
  Error extends string,
  State,
  Shape,
  VError extends string
>(
  form: Form<Output, Error, State, Shape>,
  validator: (output: Output) => VError | FieldErrors<VError> | undefined
): Form<Output, Error | VError, State, Shape> {
  return {
    validate: memoizeLast((state: State) =>
      form.validate(state).chain((value) => {
        const validationError = validator(value)
        if (validationError === undefined) {
          return ValidationSuccess.of(value)
        } else if (typeof validationError === 'string') {
          return ValidationError.of(validationError)
        } else {
          return ValidationError.fromFieldErrors(validationError)
        }
      })
    ),
    shape: form.shape
  }
}

export function required<Output, Error extends string, State, Shape>(
  form: Form<Output | undefined, Error, State, Shape>
): Form<Output, Error | 'required', State, Shape> {
  return transformed(form, (value) =>
    value === undefined
      ? ValidationError.of('required')
      : ValidationSuccess.of(value)
  )
}

export interface OneOfOption<Output> {
  domValue: string
  label: string
  dataQa?: string | undefined
  value: Output
}

export interface OneOfState<Output> {
  domValue: string
  options: OneOfOption<Output>[]
}

export type OneOf<Output, Error extends string = string> = Form<
  Output | undefined,
  Error,
  OneOfState<Output>,
  unknown
>

export function oneOf<Output>(): OneOf<Output, never> {
  return {
    validate: memoizeLast((state: OneOfState<Output>) =>
      ValidationSuccess.of(
        state.options.find((o) => o.domValue === state.domValue)?.value
      )
    ),
    shape: () => undefined
  }
}

export function nullBlank<Output, Error extends string, State, Shape>(
  form: Form<Output | undefined, Error, State, Shape>
): Form<Output | null, Error, State, Shape> {
  return mapped(form, (value) => value ?? null)
}
