// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import throttle from 'lodash/throttle'
import { useState, useEffect, useMemo } from 'react'

export function useKeepSessionAlive(sessionKeepAlive: () => Promise<boolean>) {
  const [showSessionExpiredModal, setShowSessionExpiredModal] = useState(false)
  const throttleTime =
    window.evaka?.keepSessionAliveThrottleTime ?? 2 * 60 * 1000
  const keepSessionAlive = useMemo(
    () =>
      throttle(
        async () => {
          try {
            const sessionAlive = await sessionKeepAlive()
            if (!sessionAlive) {
              setShowSessionExpiredModal(true)
            }
          } catch (error) {
            // ignore errors that might happen e.g. because client is offline or server temporarily unreachable
            console.error('Error occurred while keeping session alive', error)
          }
        },
        // Default to 2 minutes and allow overriding this in automated tests
        throttleTime,
        {
          leading: true,
          trailing: true
        }
      ),
    [sessionKeepAlive, throttleTime]
  )

  useEffect(() => {
    return () => keepSessionAlive.cancel()
  }, [keepSessionAlive])

  return {
    keepSessionAlive,
    showSessionExpiredModal,
    setShowSessionExpiredModal
  }
}
