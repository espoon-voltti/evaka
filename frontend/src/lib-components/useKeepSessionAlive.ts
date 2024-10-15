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
        5000,
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
