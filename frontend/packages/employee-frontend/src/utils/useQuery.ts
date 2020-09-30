// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'
import { useLocation } from 'react-router-dom'

/**
 * Wrapper for URLSearchParams that exposes only a read-only API
 */
export class ReadOnlySearchParams {
  constructor(private params: URLSearchParams) {}
  get(name: string): string | null {
    return this.params.get(name)
  }
  getAll(name: string): string[] {
    return this.params.getAll(name)
  }
  has(name: string): boolean {
    return this.params.has(name)
  }
  toString(): string {
    return this.params.toString()
  }
}

/**
 * Hook for accessing query parameters of the current location
 */
export function useQuery(): ReadOnlySearchParams {
  const location = useLocation()
  const query = useMemo(
    () => new ReadOnlySearchParams(new URLSearchParams(location.search)),
    [location.search]
  )
  return query
}
