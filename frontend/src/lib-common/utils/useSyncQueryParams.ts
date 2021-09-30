// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'
import { useHistory } from 'react-router-dom'

/**
 * Keeps current URL's query params up to date with the queryParams object
 */
export function useSyncQueryParams(queryParams: Record<string, string>) {
  const history = useHistory()
  const query = new URLSearchParams(queryParams).toString()
  useEffect(() => {
    const { pathname } = history.location
    const url = query ? `${pathname}?${query}` : pathname
    history.replace(url)
  }, [history, query])
}
