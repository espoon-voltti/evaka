// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { AxiosInstance } from 'axios'
import { useCallback, useState } from 'react'

import { useAxiosResponseInterceptor } from './useAxiosResponseInterceptor'
import { useInterval } from './useInterval'

const TEN_SECONDS_MILLIS = 10 * 1000

const calculateNextThreshold = (minutes: number) =>
  Date.now() + minutes * 60 * 1000

export interface PeriodicRefreshOptions {
  thresholdInMinutes: number
}

// triggers after the given threshold in minutes
// if there has also been axios activity after the threshold
export function usePeriodicRefresh(
  client: AxiosInstance,
  callback: () => void,
  { thresholdInMinutes }: PeriodicRefreshOptions
) {
  const [lastResponse, setLastResponse] = useState(Date.now())
  const updateLastResponse = useCallback(() => setLastResponse(Date.now()), [])
  useAxiosResponseInterceptor(client, updateLastResponse)

  const [nextThreshold, setNextThreshold] = useState(
    calculateNextThreshold(thresholdInMinutes)
  )

  const executeOnExceededThreshold = useCallback(() => {
    if (Date.now() >= nextThreshold && lastResponse >= nextThreshold) {
      setNextThreshold(calculateNextThreshold(thresholdInMinutes))
      callback()
    }
  }, [callback, lastResponse, nextThreshold, thresholdInMinutes])

  useInterval(executeOnExceededThreshold, TEN_SECONDS_MILLIS)
}
