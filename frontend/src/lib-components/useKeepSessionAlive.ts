import throttle from 'lodash/throttle'
import { useState, useEffect, useMemo } from 'react'

export function useKeepSessionAlive(sessionKeepAlive: () => Promise<void>) {
  const [showSessionExpiredModal, setShowSessionExpiredModal] = useState(false)

  const keepSessionAlive = useMemo(
    () =>
      throttle(
        async () => {
          try {
            await sessionKeepAlive()
          } catch (error) {
            setShowSessionExpiredModal(true)
          }
        },
        // Default to 10 minutes (1/3 of the session TTL) and allow overriding this in automated tests
        window.evaka?.keep_session_alive_throttle_time ?? 600000,
        {
          leading: true,
          trailing: true
        }
      ),
    [sessionKeepAlive]
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
