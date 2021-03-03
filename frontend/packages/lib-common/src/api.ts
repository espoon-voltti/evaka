// SPDX-FileCopyrightText: 2017-2020 City of Espoo
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

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  chain<A>(_f: (v: T) => Result<A>): Result<A> {
    return (this as unknown) as Loading<A>
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  map<A>(_f: (v: T) => A): Result<A> {
    return (this as unknown) as Loading<A>
  }

  getOrElse<A>(other: A): A | T {
    return other
  }
}

export class Failure<T> {
  readonly message: string
  readonly statusCode?: number

  readonly isLoading = false
  readonly isFailure = true
  readonly isSuccess = false

  private constructor(message: string, statusCode?: number) {
    this.message = message
    this.statusCode = statusCode
    return this
  }

  static of<T>(p: { message: string; statusCode?: number }): Failure<T> {
    return new Failure(p.message, p.statusCode)
  }

  static fromError<T>(e: Error): Failure<T> {
    if (axios.isAxiosError(e)) {
      return new Failure(e.message, e.response?.status)
    }
    return new Failure(e.message)
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  chain<A>(_f: (v: T) => Result<A>): Result<A> {
    return (this as unknown) as Result<A>
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  map<A>(_f: (v: T) => A): Result<A> {
    return (this as unknown) as Result<A>
  }

  getOrElse<A>(other: A): A | T {
    return other
  }
}

export class Success<T> {
  readonly value: T

  readonly isLoading = false
  readonly isFailure = false
  readonly isSuccess = true

  private constructor(value: T) {
    this.value = value
    return this
  }

  static of<T>(v: T): Success<T> {
    return new Success(v)
  }

  chain<A>(f: (v: T) => Result<A>): Result<A> {
    return f(this.value)
  }

  map<A>(f: (v: T) => A): Result<A> {
    return this.chain((v) => Success.of(f(v)))
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  getOrElse<A>(_other: A): A | T {
    return this.value
  }
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

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type ApiFunction = (...args: any[]) => Promise<Result<any>>
export type ApiResultOf<T extends ApiFunction> = ReturnType<T> extends Promise<
  Result<infer R>
>
  ? R
  : never

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

export interface Paged<T> {
  data: T[]
  total: number
  pages: number
}
