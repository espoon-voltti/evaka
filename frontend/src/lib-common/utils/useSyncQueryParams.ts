// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import partition from 'lodash/partition'
import { useEffect } from 'react'
import { useSearchParams } from 'wouter'

/**
 * Keeps current URL's query params up to date with the queryParams object
 */
export function useSyncQueryParams(queryParams: Record<string, string>) {
  const [currentParams, setSearchParams] = useSearchParams()

  useEffect(() => {
    const [relevantParams, extraParams] = partition(
      Array.from(currentParams.entries()),
      ([key]) => Object.keys(queryParams).includes(key)
    )
    if (!isEqual(Object.fromEntries(relevantParams), queryParams)) {
      setSearchParams(
        { ...Object.fromEntries(extraParams), ...queryParams },
        {
          replace: true
        }
      )
    }
  }, [currentParams, queryParams, setSearchParams])
}
