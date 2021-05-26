// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useRef, useCallback } from 'react'

export function useDebouncedCallback<
  T extends (...args: never[]) => ReturnType<T>
>(fn: T, delay: number): (...args: Parameters<T>) => void {
  // ref stores the timeout between renders and prevents re-renders on changes
  const timeout = useRef<ReturnType<typeof setTimeout>>()

  return useCallback(
    (...args) => {
      const functionToBeExecutedLater = () => {
        if (timeout.current) {
          clearTimeout(timeout.current)
        }
        fn(...args)
      }

      if (timeout.current) {
        clearTimeout(timeout.current)
      }
      timeout.current = setTimeout(functionToBeExecutedLater, delay)
    },
    [fn, delay]
  )
}
