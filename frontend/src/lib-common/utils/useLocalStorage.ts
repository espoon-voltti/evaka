// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Dispatch } from 'react'
import { useEffect, useState } from 'react'

export default function useLocalStorage(
  key: string,
  initialValue: string
): [string, Dispatch<string>]
export default function useLocalStorage<T extends string>(
  key: string,
  initialValue: string,
  validateStoredValue: (v: string | null) => v is T
): [T, Dispatch<T>]
export default function useLocalStorage<T extends string>(
  key: string,
  initialValue: T,
  validateStoredValue?: (v: string | null) => v is T
): [T, Dispatch<T>] {
  const [value, setValue] = useState<T>((): T => {
    try {
      const storedValue = window.localStorage?.getItem(key)
      return validateStoredValue?.(storedValue) ? storedValue : initialValue
    } catch (e) {
      return initialValue
    }
  })

  useEffect(() => {
    try {
      window.localStorage?.setItem(key, value)
    } catch (e) {
      // do nothing
    }
  }, [key, value])

  return [value, setValue]
}
