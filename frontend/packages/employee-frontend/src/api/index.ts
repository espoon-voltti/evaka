// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AxiosError } from 'axios'

export interface Response<T> {
  data: T
}

export interface Loading {
  done: false
}

export interface Failure {
  done: true
  error: {
    statusCode?: number
    message?: string
  }
}

export interface Success<T> {
  done: true
  data: T
}

export type Result<T> = Loading | Failure | Success<T>

export interface Cancelled {
  cancelled: true
}

const loading = { done: false } as const
export function Loading(): Loading {
  return loading
}

export function Success<T>(data: T): Success<T> {
  return { done: true, data }
}

function isAxiosError(error: Error): error is AxiosError {
  return !!(error && (error as AxiosError).isAxiosError)
}

export function Failure<T>(error: Error): Failure {
  return {
    done: true,
    error: {
      statusCode: isAxiosError(error)
        ? error.response && error.response.status
        : undefined,
      message: error.message
    }
  }
}

export function Cancelled(): Cancelled {
  return { cancelled: true }
}

export function isLoading<T>(value: Result<T>): value is Loading {
  return value.done === false
}

export function isFailure<T>(value: Result<T>): value is Failure {
  return value.done && 'error' in value
}

export function isSuccess<T>(value: Result<T>): value is Success<T> {
  return value.done && 'data' in value
}

export function mapResult<A, B>(value: Result<A>, f: (v: A) => B): Result<B> {
  if (isSuccess(value)) {
    return Success(f(value.data))
  }
  return value
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
