// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useParams } from 'react-router-dom'

export const INVALID: unique symbol = Symbol('invalid')

export type Parser<T> = (input: string) => T | typeof INVALID

export default function useRequiredParams<
  T extends (string | [string, Parser<unknown>])[]
>(...args: T): Process<T> {
  const params = useParams()
  return Object.fromEntries(
    args.map((spec) => {
      const [key, parser] = typeof spec === 'string' ? [spec, id] : spec
      const strValue = params[key]
      if (strValue === undefined) {
        throw new Error(`Route param ${key} is missing`)
      }
      const value = parser(strValue)
      if (value === INVALID) {
        throw new Error(`Route param ${key} is invalid`)
      }
      return [key, value]
    })
  ) as Process<T>
}

function id(x: string): string {
  return x
}

type Process<T> = T extends [infer U, ...infer Rest]
  ? ProcessOne<U> & Process<Rest>
  : unknown // T & unknown = T

type ProcessOne<T> = T extends string
  ? { [K in T]: string }
  : T extends [string, (input: string) => infer U]
    ? { [K in T[0]]: Exclude<U, typeof INVALID> }
    : never
