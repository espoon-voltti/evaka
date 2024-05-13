// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useState } from 'react'

const getWindowSize = () => ({
  width: window.innerWidth,
  height: window.innerHeight
})

const DEFAULT_DEBOUNCE_MS = 200

/**
 * A hook that returns the latest inner size of the window and observes resize events
 */
export const useWindowSize = (props?: {
  debounceMs?: number
}): {
  width: number
  height: number
} => {
  const debounceMs = props?.debounceMs ?? DEFAULT_DEBOUNCE_MS
  const [debouncedSize, setDebouncedSize] = useState(getWindowSize())

  useEffect(() => {
    let timeoutId: number | NodeJS.Timeout | undefined = undefined

    const updateSize = () => setDebouncedSize(getWindowSize())
    const onResize = () => {
      clearTimeout(timeoutId)
      timeoutId = setTimeout(updateSize, debounceMs)
    }

    window.addEventListener('resize', onResize)
    return () => {
      clearTimeout(timeoutId)
      window.removeEventListener('resize', onResize)
    }
  }, [debounceMs])

  return debouncedSize
}
