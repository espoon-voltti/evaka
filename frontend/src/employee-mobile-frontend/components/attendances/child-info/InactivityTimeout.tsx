{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import { useCallback, useEffect } from 'react'

export default function useInactivityTimeout(
  timeout: number,
  onTimeOut: () => void
) {
  const timeoutCallBack = useCallback(() => {
    clearTimeout()
    onTimeOut && onTimeOut()
  }, [onTimeOut])

  useEffect(() => {
    let timeoutHandle: number

    const setTimeouts = () => {
      timeoutHandle = setTimeout(timeoutCallBack, timeout)
    }

    const clearTimeouts = () => {
      if (timeoutHandle) clearTimeout(timeoutHandle)
    }
    const events = [
      'load',
      'mousemove',
      'mousedown',
      'click',
      'scroll',
      'keypress',
      'touch'
    ]

    const resetTimeout = () => {
      clearTimeouts()
      setTimeouts()
    }

    const onBlur = () => {
      clearTimeouts()
      timeoutCallBack()
    }

    window.addEventListener('blur', onBlur, { passive: true })
    for (const event of events) {
      window.addEventListener(event, resetTimeout, { passive: true })
    }

    setTimeouts()
    return () => {
      window.removeEventListener('blur', onBlur)
      for (const event of events) {
        window.removeEventListener(event, resetTimeout)
        clearTimeouts()
      }
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return
}
