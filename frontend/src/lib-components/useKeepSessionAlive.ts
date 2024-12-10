// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import throttle from 'lodash/throttle'
import { useState, useEffect, useMemo } from 'react'

export function useKeepSessionAlive(
  sessionKeepAlive: () => Promise<boolean>,
  enabled: boolean
) {
  const [sessionExpirationDetected, setSessionExpirationDetected] =
    useState(false)
  // Default to 2 minutes and allow overriding this in automated tests
  const throttleTime =
    window.evaka?.keepSessionAliveThrottleTime ?? 2 * 60 * 1000

  const throttledKeepAlive = useMemo(
    () =>
      throttle(
        async () => {
          try {
            const sessionAlive = await sessionKeepAlive()
            if (!sessionAlive) {
              setSessionExpirationDetected(true)
            }
          } catch (error) {
            // ignore errors that might happen e.g. because client is offline or server temporarily unreachable
          }
        },
        throttleTime,
        {
          leading: true,
          trailing: true
        }
      ),
    [sessionKeepAlive, throttleTime]
  )

  useEffect(() => {
    if (!enabled) return

    const eventListenerOptions = { capture: true, passive: true }
    const userActivityEvents = ['keydown', 'mousedown', 'wheel', 'touchstart']

    userActivityEvents.forEach((event) => {
      document.addEventListener(event, throttledKeepAlive, eventListenerOptions)
    })

    return () => {
      userActivityEvents.forEach((event) => {
        document.removeEventListener(
          event,
          throttledKeepAlive,
          eventListenerOptions
        )
      })
      throttledKeepAlive.cancel()
    }
  }, [throttledKeepAlive, enabled])

  return {
    sessionExpirationDetected,
    dismissSessionExpiredDetection: () => setSessionExpirationDetected(false)
  }
}
