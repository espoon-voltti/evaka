// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'
import { useParams } from 'react-router'

import type { Id } from './id-type'
import { fromUuid } from './id-type'

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
        Sentry.captureMessage(`Route param ${key} is missing`, 'error')
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

type RequiredObject<T extends string[]> = Record<T[number], string>
type OptionalObject<T extends string[]> = Partial<Record<T[number], string>>

export function useIdRouteParam<T extends Id<string>>(paramName: string): T {
  const params = useRouteParams([paramName])
  return fromUuid<T>(params[paramName])
}
