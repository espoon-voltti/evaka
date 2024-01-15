// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable @typescript-eslint/no-explicit-any */

export type Form<Output, Error extends string, State, Shape> = {
  validate: (state: State) => ValidationResult<Output, Error>
  shape: () => Shape
}

export type AnyForm = Form<any, string, any, any>

export type OutputOf<F extends Form<any, any, any, any>> =
  F extends Form<infer Output, any, any, any> ? Output : never
export type ErrorOf<F extends Form<any, any, any, any>> =
  F extends Form<any, infer Error, any, any> ? Error : never
export type StateOf<F extends Form<any, any, any, any>> =
  F extends Form<any, any, infer State, any> ? State : never
export type ShapeOf<F extends AnyForm> =
  F extends Form<any, any, any, infer Shape> ? Shape : never

export class ValidationSuccess<Output, Error> {
  readonly isValid = true

  private constructor(public value: Output) {}

  static of<Output>(value: Output): ValidationResult<Output, never> {
    return new ValidationSuccess(value)
  }

  map<T>(fn: (value: Output) => T): ValidationResult<T, Error> {
    return ValidationSuccess.of(fn(this.value))
  }

  apply<T, E>(
    f: ValidationResult<(v: Output) => T, E>
  ): ValidationResult<T, Error | E> {
    if (f.isValid) {
      return new ValidationSuccess(f.value(this.value))
    }
    return f as unknown as ValidationResult<T, Error | E>
  }

  chain<T, E>(
    fn: (value: Output) => ValidationResult<T, E>
  ): ValidationResult<T, Error | E> {
    return fn(this.value)
  }
}

export interface FieldErrors<Error> {
  [key: string]: Error | FieldErrors<Error> | undefined
}

export class ValidationError<Output, Error> {
  readonly isValid = false

  private constructor(public error: FieldErrors<Error> | Error) {}

  static of<Error>(validationError: Error): ValidationResult<never, Error> {
    return new ValidationError(validationError)
  }

  static fromFieldErrors<Error>(
    fieldErrors: FieldErrors<Error>
  ): ValidationResult<never, Error> {
    return new ValidationError(fieldErrors)
  }

  static field<Error>(
    fieldName: string,
    validationError: Error
  ): ValidationResult<never, Error> {
    return ValidationError.fromFieldErrors({ [fieldName]: validationError })
  }

  map<T>(_fn: (value: Output) => T): ValidationError<T, Error> {
    return this as unknown as ValidationError<T, Error>
  }

  apply<T, E>(
    _f: ValidationResult<(v: Output) => T, E>
  ): ValidationResult<T, Error | E> {
    return this as unknown as ValidationResult<T, Error | E>
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

export function map<AO, BO, O, AE, BE>(
  ar: ValidationResult<AO, AE>,
  br: ValidationResult<BO, BE>,
  fn: (a: AO, b: BO) => O
): ValidationResult<O, AE | BE>
export function map<AO, BO, CO, O, AE, BE, CE>(
  ar: ValidationResult<AO, AE>,
  br: ValidationResult<BO, BE>,
  cr: ValidationResult<CO, CE>,
  fn: (a: AO, b: BO, c: CO) => O
): ValidationResult<O, AE | BE | CE>
export function map<AO, BO, CO, DO, O, AE, BE, CE, DE>(
  ar: ValidationResult<AO, AE>,
  br: ValidationResult<BO, BE>,
  cr: ValidationResult<CO, CE>,
  dr: ValidationResult<DO, DE>,
  fn: (a: AO, b: BO, c: CO, d: DO) => O
): ValidationResult<O, AE | BE | CE | DE>
export function map<AO, BO, CO, DO, EO, O, AE, BE, CE, DE, EE>(
  ar: ValidationResult<AO, AE>,
  br: ValidationResult<BO, BE>,
  cr: ValidationResult<CO, CE>,
  dr: ValidationResult<DO, DE>,
  er: ValidationResult<EO, EE>,
  fn: (a: AO, b: BO, c: CO, d: DO, e: EO) => O
): ValidationResult<O, AE | BE | CE | DE | EE>
export function map<AO, BO, CO, DO, EO, O, AE, BE, CE, DE, EE>(
  ...args: unknown[]
): ValidationResult<unknown, unknown> {
  switch (args.length) {
    case 3: {
      const [ar, br, fn] = args as [
        ValidationResult<AO, AE>,
        ValidationResult<BO, BE>,
        (a: AO, b: BO) => O
      ]
      const f = (a: AO) => (b: BO) => fn(a, b)
      return br.apply(ar.map(f))
    }
    case 4: {
      const [ar, br, cr, fn] = args as [
        ValidationResult<AO, AE>,
        ValidationResult<BO, BE>,
        ValidationResult<CO, CE>,
        (a: AO, b: BO, c: CO) => O
      ]
      const f = (a: AO) => (b: BO) => (c: CO) => fn(a, b, c)
      return cr.apply(br.apply(ar.map(f)))
    }
    case 5: {
      const [ar, br, cr, dr, fn] = args as [
        ValidationResult<AO, AE>,
        ValidationResult<BO, BE>,
        ValidationResult<CO, CE>,
        ValidationResult<DO, DE>,
        (a: AO, b: BO, c: CO, d: DO) => O
      ]
      const f = (a: AO) => (b: BO) => (c: CO) => (d: DO) => fn(a, b, c, d)
      return dr.apply(cr.apply(br.apply(ar.map(f))))
    }
    case 6: {
      const [ar, br, cr, dr, er, fn] = args as [
        ValidationResult<AO, AE>,
        ValidationResult<BO, BE>,
        ValidationResult<CO, CE>,
        ValidationResult<DO, DE>,
        ValidationResult<EO, EE>,
        (a: AO, b: BO, c: CO, d: DO, e: EO) => O
      ]
      const f = (a: AO) => (b: BO) => (c: CO) => (d: DO) => (e: EO) =>
        fn(a, b, c, d, e)
      return er.apply(dr.apply(cr.apply(br.apply(ar.map(f)))))
    }
  }
  throw new Error('not reached')
}

export function combine<AO, BO, AE, BE>(
  ar: ValidationResult<AO, AE>,
  br: ValidationResult<BO, BE>
): ValidationResult<[AO, BO], AE | BE>
export function combine<AO, BO, CO, AE, BE, CE>(
  ar: ValidationResult<AO, AE>,
  br: ValidationResult<BO, BE>,
  cr: ValidationResult<CO, CE>
): ValidationResult<[AO, BO, CO], AE | BE | CE>
export function combine<AO, BO, CO, DO, AE, BE, CE, DE>(
  ar: ValidationResult<AO, AE>,
  br: ValidationResult<BO, BE>,
  cr: ValidationResult<CO, CE>,
  dr: ValidationResult<DO, DE>
): ValidationResult<[AO, BO, CO, DO], AE | BE | CE | DE>
export function combine<AO, BO, CO, DO, EO, AE, BE, CE, DE, EE>(
  ar: ValidationResult<AO, AE>,
  br: ValidationResult<BO, BE>,
  cr: ValidationResult<CO, CE>,
  dr: ValidationResult<DO, DE>,
  er: ValidationResult<EO, EE>
): ValidationResult<[AO, BO, CO, DO, EO], AE | BE | CE | DE | EE>
export function combine<AO, BO, CO, DO, EO, AE, BE, CE, DE, EE>(
  ...args: unknown[]
): ValidationResult<unknown, unknown> {
  switch (args.length) {
    case 2: {
      const [ar, br] = args as [
        ValidationResult<AO, AE>,
        ValidationResult<BO, BE>
      ]
      return map(ar, br, (a, b): [AO, BO] => [a, b])
    }
    case 3: {
      const [ar, br, cr] = args as [
        ValidationResult<AO, AE>,
        ValidationResult<BO, BE>,
        ValidationResult<CO, CE>
      ]
      return map(ar, br, cr, (a, b, c): [AO, BO, CO] => [a, b, c])
    }
    case 4: {
      const [ar, br, cr, dr] = args as [
        ValidationResult<AO, AE>,
        ValidationResult<BO, BE>,
        ValidationResult<CO, CE>,
        ValidationResult<DO, DE>
      ]
      return map(ar, br, cr, dr, (a, b, c, d): [AO, BO, CO, DO] => [a, b, c, d])
    }
    case 5: {
      const [ar, br, cr, dr, er] = args as [
        ValidationResult<AO, AE>,
        ValidationResult<BO, BE>,
        ValidationResult<CO, CE>,
        ValidationResult<DO, DE>,
        ValidationResult<EO, EE>
      ]
      return map(ar, br, cr, dr, er, (a, b, c, d, e): [AO, BO, CO, DO, EO] => [
        a,
        b,
        c,
        d,
        e
      ])
    }
  }
  throw new Error('not reached')
}
