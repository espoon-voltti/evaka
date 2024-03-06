// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useParams } from 'react-router-dom'

export default function useRouteParams<
  const Required extends string[],
  const Optional extends string[]
>(
  required: Required,
  optional?: Optional
): RequiredObject<Required> & OptionalObject<Optional> {
  const params = useParams()
  return Object.fromEntries([
    ...required.map((key) => {
      const value = params[key]
      if (value === undefined) {
        throw new Error(`Route param ${key} is missing`)
      }
      return [key, value]
    }),
    ...(optional || []).flatMap((key) => {
      const value = params[key]
      if (value === undefined) {
        return []
      } else {
        return [[key, value]]
      }
    })
  ]) as RequiredObject<Required> & OptionalObject<Optional>
}

type RequiredObject<T> = T extends [infer First, ...infer Rest]
  ? (First extends string ? { [K in First]: string } : never) &
      RequiredObject<Rest>
  : unknown // T & unknown = T

type OptionalObject<T> = T extends [infer First, ...infer Rest]
  ? (First extends string ? { [K in First]?: string } : never) &
      OptionalObject<Rest>
  : unknown // T & unknown = T
