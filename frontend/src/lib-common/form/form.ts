// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { memoizeLast } from './memoize'
import {
  AnyForm,
  ErrorOf,
  Form,
  ObjectFieldError,
  OutputOf,
  StateOf,
  ValidationError,
  ValidationResult,
  ValidationSuccess
} from './types'

export function value<T>(): Form<T, never, T, unknown> {
  return {
    validate: memoizeLast((state: T) => ValidationSuccess.of(state)),
    shape: undefined
  }
}

type AnyObjectFields = { [K in string]: AnyForm }
type ObjectOutput<Fields extends AnyObjectFields> = {
  [K in keyof Fields]: OutputOf<Fields[K]>
}
type ObjectError<Fields extends AnyObjectFields> =
  | ErrorOf<Fields[keyof Fields]>
  | ObjectFieldError
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
    validate: memoizeLast((state: ObjectState<Fields>) => {
      const allFields = Object.entries(fields)
      const validValues = allFields.flatMap(([k, field]) => {
        const validationResult = field.validate(state[k])
        return validationResult.isValid
          ? [[k, validationResult.value] as const]
          : []
      })
      if (validValues.length === allFields.length) {
        return ValidationSuccess.of(
          Object.fromEntries(validValues) as {
            [K in keyof Fields]: OutputOf<Fields[K]>
          }
        )
      } else {
        return ValidationError.objectFieldError
      }
    }),
    shape: fields
  }
}

export function array<Elem extends AnyForm>(
  elem: Elem
): Form<
  OutputOf<Elem>[],
  ErrorOf<Elem> | ObjectFieldError,
  StateOf<Elem>[],
  Elem
> {
  return {
    validate: memoizeLast((state: StateOf<Elem>[]) => {
      const valid = state.flatMap((elemState) => {
        const validationResult = elem.validate(elemState)
        return validationResult.isValid
          ? ([validationResult.value] as const)
          : []
      })
      if (valid.length === state.length) {
        return ValidationSuccess.of(valid as OutputOf<Elem>)
      } else {
        return ValidationError.objectFieldError
      }
    }),
    shape: elem
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

type UnionError<Fields extends AnyObjectFields> =
  | ErrorOf<Fields[keyof Fields]>
  | ObjectFieldError

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
      const validationResult = activeBranch.validate(state.state)
      if (validationResult.isValid) {
        return ValidationSuccess.of({
          branch: state.branch,
          // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
          value: validationResult.value
        } as UnionOutput<Fields, keyof Fields>)
      } else {
        return ValidationError.objectFieldError
      }
    }),
    shape: fields
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
  validator: (output: Output) => VError | undefined
): Form<Output, Error | VError, State, Shape> {
  return {
    validate: memoizeLast((state: State) =>
      form.validate(state).chain((value) => {
        const validationError = validator(value)
        if (validationError !== undefined) {
          return ValidationError.of(validationError)
        }
        return ValidationSuccess.of(value)
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

export type OneOf<Output, Error extends string = string> = Form<
  Output | undefined,
  Error,
  { domValue: string; options: OneOfOption<Output>[] },
  unknown
>

export function oneOf<Output>(): OneOf<Output, never> {
  return {
    validate: memoizeLast((state) =>
      ValidationSuccess.of(
        state.options.find((o) => o.domValue === state.domValue)?.value
      )
    ),
    shape: undefined
  }
}
