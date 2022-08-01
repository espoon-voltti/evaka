// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import { useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'

/**
 * Keeps current URL's query params up to date with the queryParams object
 */
export function useSyncQueryParams(queryParams: Record<string, string>) {
  const [currentParams, setSearchParams] = useSearchParams()

  useEffect(() => {
    if (
      !isEqual(
        Object.fromEntries(Array.from(currentParams.entries())),
        queryParams
      )
    ) {
      setSearchParams(queryParams, {
        replace: true
      })
    }
  }, [currentParams, queryParams, setSearchParams])
}
