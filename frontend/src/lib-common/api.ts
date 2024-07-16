// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'

export interface Response<T> {
  data: T
}

export type Result<T> = Loading<T> | Failure<T> | Success<T>

export class Loading<T> {
  private static _instance: Loading<unknown>

  readonly isLoading = true
  readonly isFailure = false
  readonly isSuccess = false

  private constructor() {
    if (!Loading._instance) {
      Loading._instance = this
    }
    return Loading._instance as Loading<T>
  }

  static of<T>(): Loading<T> {
    return new Loading()
  }

  chain<A>(_f: (v: T) => Result<A>): Result<A> {
    return this as unknown as Result<A>
  }

  apply<A>(f: Result<(v: T) => A>): Result<A> {
    if (f.isFailure) return f as unknown as Result<A>
    return this as unknown as Result<A>
  }

  map<A>(_f: (v: T) => A): Result<A> {
    return this as unknown as Result<A>
  }

  mapAll<A>(fs: {
    loading: () => A
    failure: (v: Failure<T>) => A
    success: (v: T, isReloading: boolean) => A
  }): A {
    return fs.loading()
  }

  getOrElse<A>(other: A): A | T {
    return other
  }
}

export class Failure<T> {
  readonly message: string
  readonly statusCode?: number
  readonly errorCode?: string

  readonly isLoading = false
  readonly isFailure = true
  readonly isSuccess = false

  private constructor(
    message: string,
    statusCode?: number,
    errorCode?: string
  ) {
    this.message = message
    this.statusCode = statusCode
    this.errorCode = errorCode
    return this
  }

  static of<T>(p: {
    message: string
    statusCode?: number
    errorCode?: string
  }): Failure<T> {
    return new Failure(p.message, p.statusCode, p.errorCode)
  }

  static fromError<T>(e: unknown): Failure<T> {
    if (axios.isAxiosError(e)) {
      const data = e.response?.data as { errorCode?: string } | undefined
      const errorCode = data?.errorCode
      return new Failure(
        e.message,
        e.response?.status,
        typeof errorCode === 'string' ? errorCode : undefined
      )
    } else if (e instanceof Error) {
      return new Failure(e.message)
    } else {
      return new Failure(String(e))
    }
  }

  chain<A>(_f: (v: T) => Result<A>): Result<A> {
    return this as unknown as Result<A>
  }

  apply<A>(_f: Result<(v: T) => A>): Result<A> {
    return this as unknown as Result<A>
  }

  map<A>(_f: (v: T) => A): Result<A> {
    return this as unknown as Result<A>
  }

  mapAll<A>(fs: {
    loading: () => A
    failure: (v: Failure<T>) => A
    success: (v: T, isReloading: boolean) => A
  }): A {
    return fs.failure(this)
  }

  getOrElse<A>(other: A): A | T {
    return other
  }
}

export class Success<T> {
  readonly value: T
  readonly isReloading: boolean

  readonly isLoading = false
  readonly isFailure = false
  readonly isSuccess = true

  private constructor(value: T, isReloading: boolean) {
    this.value = value
    this.isReloading = isReloading
  }

  static of(): Success<void>
  static of<T>(v: T): Success<T>
  static of<T>(v?: T): Success<T | undefined> {
    return new Success(v, false)
  }

  chain<A>(f: (v: T) => Result<A>): Result<A> {
    return f(this.value)
  }

  apply<A>(f: Result<(v: T) => A>): Result<A> {
    if (f.isSuccess) {
      return new Success(f.value(this.value), this.isReloading || f.isReloading)
    }
    return f as unknown as Result<A>
  }

  map<A>(f: (v: T) => A): Result<A> {
    return this.chain((v) => new Success(f(v), this.isReloading))
  }

  mapAll<A>(fs: {
    loading: () => A
    failure: (v: Failure<T>) => A
    success: (v: T, isReloading: boolean) => A
  }): A {
    return fs.success(this.value, this.isReloading)
  }

  getOrElse<A>(_other: A): A | T {
    return this.value
  }

  reloading(): Result<T> {
    return new Success(this.value, true)
  }
}

export function map<A, B, C>(
  ar: Result<A>,
  br: Result<B>,
  fn: (a: A, b: B) => C
): Result<C>
export function map<A, B, C, D>(
  ar: Result<A>,
  br: Result<B>,
  cr: Result<C>,
  fn: (a: A, b: B, c: C) => D
): Result<D>
export function map<A, B, C, D, E>(
  ar: Result<A>,
  br: Result<B>,
  cr: Result<C>,
  dr: Result<D>,
  fn: (a: A, b: B, c: C, d: D) => E
): Result<D>
export function map<A, B, C, D, E, F>(
  ar: Result<A>,
  br: Result<B>,
  cr: Result<C>,
  dr: Result<D>,
  er: Result<E>,
  fn: (a: A, b: B, c: C, d: D, e: E) => F
): Result<D>
export function map<A, B, C, D, E, F>(...args: unknown[]): Result<unknown> {
  switch (args.length) {
    case 3: {
      const [ar, br, fn] = args as [Result<A>, Result<B>, (a: A, b: B) => C]
      const f = (a: A) => (b: B) => fn(a, b)
      return br.apply(ar.map(f))
    }
    case 4: {
      const [ar, br, cr, fn] = args as [
        Result<A>,
        Result<B>,
        Result<C>,
        (a: A, b: B, c: C) => D
      ]
      const f = (a: A) => (b: B) => (c: C) => fn(a, b, c)
      return cr.apply(br.apply(ar.map(f)))
    }
    case 5: {
      const [ar, br, cr, dr, fn] = args as [
        Result<A>,
        Result<B>,
        Result<C>,
        Result<D>,
        (a: A, b: B, c: C, d: D) => E
      ]
      const f = (a: A) => (b: B) => (c: C) => (d: D) => fn(a, b, c, d)
      return dr.apply(cr.apply(br.apply(ar.map(f))))
    }
    case 6: {
      const [ar, br, cr, dr, er, fn] = args as [
        Result<A>,
        Result<B>,
        Result<C>,
        Result<D>,
        Result<E>,
        (a: A, b: B, c: C, d: D, e: E) => F
      ]
      const f = (a: A) => (b: B) => (c: C) => (d: D) => (e: E) =>
        fn(a, b, c, d, e)
      return er.apply(dr.apply(cr.apply(br.apply(ar.map(f)))))
    }
  }
  throw new Error('not reached')
}

export function combine<A, B>(ar: Result<A>, br: Result<B>): Result<[A, B]>
export function combine<A, B, C>(
  ar: Result<A>,
  br: Result<B>,
  cr: Result<C>
): Result<[A, B, C]>
export function combine<A, B, C, D>(
  ar: Result<A>,
  br: Result<B>,
  cr: Result<C>,
  dr: Result<D>
): Result<[A, B, C, D]>
export function combine<A, B, C, D, E>(
  ar: Result<A>,
  br: Result<B>,
  cr: Result<C>,
  dr: Result<D>,
  er: Result<E>
): Result<[A, B, C, D, E]>
export function combine<A, B, C, D, E>(...args: unknown[]): Result<unknown> {
  switch (args.length) {
    case 2: {
      const [ar, br] = args as [Result<A>, Result<B>]
      return map(ar, br, (a, b): [A, B] => [a, b])
    }
    case 3: {
      const [ar, br, cr] = args as [Result<A>, Result<B>, Result<C>]
      return map(ar, br, cr, (a, b, c): [A, B, C] => [a, b, c])
    }
    case 4: {
      const [ar, br, cr, dr] = args as [
        Result<A>,
        Result<B>,
        Result<C>,
        Result<D>
      ]
      return map(ar, br, cr, dr, (a, b, c, d): [A, B, C, D] => [a, b, c, d])
    }
    case 5: {
      const [ar, br, cr, dr, er] = args as [
        Result<A>,
        Result<B>,
        Result<C>,
        Result<D>,
        Result<E>
      ]
      return map(ar, br, cr, dr, er, (a, b, c, d, e): [A, B, C, D, E] => [
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

export interface Cancelled {
  cancelled: true
}

export function Cancelled(): Cancelled {
  return { cancelled: true }
}

export function isCancelled<T>(
  value: Result<T> | Cancelled
): value is Cancelled {
  return 'cancelled' in value && value.cancelled
}

export function isLoading(value: Result<unknown>) {
  return value.isLoading || (value.isSuccess && value.isReloading)
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type ApiFunction = (...args: any[]) => Promise<Result<any>>
export type ApiResultOf<T extends ApiFunction> =
  ReturnType<T> extends Promise<Result<infer R>> ? R : never

/**
 * Converts an API function into another that cancels stale responses
 *
 * When you call the returned function, all results returned by earlier
 * invocations are replaced with Cancelled.
 */
export function withStaleCancellation<F extends ApiFunction>(
  f: F
): (...args: Parameters<F>) => Promise<Result<ApiResultOf<F>> | Cancelled> {
  let globalRequestId = 0
  return (...args: Parameters<F>) => {
    const requestId = ++globalRequestId
    return new Promise<Result<ApiResultOf<F>> | Cancelled>(
      (resolve, reject) => {
        try {
          f(...args)
            // cancel if another request has been started
            .then((result) =>
              resolve(globalRequestId === requestId ? result : Cancelled())
            )
            .catch(reject)
        } catch (e) {
          reject(e)
        }
      }
    )
  }
}

export function createUrlSearchParams(
  ...nameValuePairs: [string, string | null | undefined][]
): URLSearchParams {
  const params = new URLSearchParams()
  for (const [name, value] of nameValuePairs) {
    if (value != null) params.append(name, value)
  }
  return params
}

export const wrapResult =
  <Args extends any[], R>( // eslint-disable-line @typescript-eslint/no-explicit-any
    apiCall: (...args: Args) => Promise<R>
  ): ((...args: Args) => Promise<Result<R>>) =>
  (...args) =>
    apiCall(...args)
      .then((res) => Success.of(res))
      .catch((e) => Failure.fromError(e))
