// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'

/**
 * Keeps current URL's query params up to date with the queryParams object
 */
export function useSyncQueryParams(queryParams: Record<string, string>) {
  const [, setSearchParams] = useSearchParams()
  const newSearchParams = new URLSearchParams(queryParams).toString()

  useEffect(() => {
    setSearchParams(newSearchParams)
  }, [newSearchParams, setSearchParams])
}
