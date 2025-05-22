// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback } from 'react'

export type SetStateCallback<T> = (fn: (prev: T) => T) => void

export function useFieldSetState<T, K extends keyof T>(
  onChange: SetStateCallback<T>,
  key: K
): SetStateCallback<T[K]> {
  return useCallback<SetStateCallback<T[K]>>(
    (fn) => onChange((prev) => ({ ...prev, [key]: fn(prev[key]) })),
    [onChange, key]
  )
}

export function useFieldDispatch<T, K extends keyof T>(
  onChange: SetStateCallback<T>,
  key: K
): (value: T[K]) => void {
  const setState = useFieldSetState(onChange, key)
  return useCallback((value: T[K]) => setState(() => value), [setState])
}
