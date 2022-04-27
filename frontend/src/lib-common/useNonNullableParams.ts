// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'
import { useParams } from 'react-router-dom'

export default function useNonNullableParams<
  T extends Record<string, string>
>() {
  const params = useParams<T>()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined) {
      Sentry.captureMessage(
        `Route param ${key} is undefined`,
        Sentry.Severity.Error
      )
    }
  })
  return params as T
}
