import React, { useCallback, useEffect } from 'react'

interface Props {
  timeout: number
  onTimeOut: () => void
}

export default function AutoLogout({ timeout, onTimeOut }: Props) {
  const logout = useCallback(() => {
    clearTimeout()
    onTimeOut && onTimeOut()
  }, [onTimeOut])

  useEffect(() => {
    let logoutTimeout: number

    const setTimeouts = () => {
      logoutTimeout = setTimeout(logout, timeout)
    }

    const clearTimeouts = () => {
      if (logoutTimeout) clearTimeout(logoutTimeout)
    }
    const events = [
      'load',
      'mousemove',
      'mousedown',
      'click',
      'scroll',
      'keypress'
    ]

    const resetTimeout = () => {
      clearTimeouts()
      setTimeouts()
    }

    for (const event of events) {
      window.addEventListener(event, resetTimeout)
    }

    setTimeouts()
    return () => {
      for (const event of events) {
        window.removeEventListener(event, resetTimeout)
        clearTimeouts()
      }
    }
  }, [])

  return <></>
}
