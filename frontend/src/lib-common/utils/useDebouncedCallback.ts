// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useRef, useCallback } from 'react'

interface PendingCall {
  timeout: ReturnType<typeof setTimeout>
  closure: () => void
}

/**
 * Return a function `callback` that calls `fn` with the last provided
 * parameters after `delay` ms has passed since the last call of `callback`.
 *
 * @param fn The function to call
 * @param delay Time to wait before calling, in ms
 * @returns [callback, cancelTimeout, runImmediately] `callback` triggers
 *   calling the function, `cancelTimeout` cancels a queued call,
 *   `runImmediately` runs the possible pending call immediately
 */
export function useDebouncedCallback<
  T extends (...args: never[]) => ReturnType<T>
>(
  fn: T,
  delay: number
): [(...args: Parameters<T>) => void, () => void, () => void] {
  const pendingCall = useRef<PendingCall>(undefined)

  const cancelTimeout = useCallback(() => {
    if (pendingCall.current) {
      clearTimeout(pendingCall.current.timeout)
      pendingCall.current = undefined
    }
  }, [])

  const callback = useCallback(
    (...args: Parameters<T>) => {
      cancelTimeout()
      pendingCall.current = {
        timeout: setTimeout(() => pendingCall.current?.closure(), delay),
        closure: () => {
          pendingCall.current = undefined
          fn(...args)
        }
      }
    },
    [cancelTimeout, delay, fn]
  )

  const callImmediately = useCallback(() => {
    if (pendingCall.current) {
      pendingCall.current.closure()
      cancelTimeout()
    }
  }, [cancelTimeout])

  return [callback, cancelTimeout, callImmediately]
}
