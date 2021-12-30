// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useState } from 'react'
import { useDebouncedCallback } from './useDebouncedCallback'

type SaveState = 'clean' | 'dirty' | 'saving:clean' | 'saving:dirty'

interface UseDebouncedSave<T> {
  value: T
  setValue: (value: T) => void
  saveImmediately: () => void
  state: SaveState
}

export function useDebouncedSave<T>(
  initialValue: T,
  saveFn: (value: T) => Promise<void>,
  delay: number
): UseDebouncedSave<T> {
  const [value, setValueState] = useState<T>(initialValue)
  const [state, setState] = useState<SaveState>('clean')

  const setValue = useCallback((newValue: T) => {
    setValueState(newValue)
    setState((prev) => (prev === 'saving:clean' ? 'saving:dirty' : 'dirty'))
  }, [])

  const save = useCallback(
    async (value: T) => {
      setState('saving:clean')
      await saveFn(value)
      setState((prev) => (prev === 'saving:dirty' ? 'dirty' : 'clean'))
    },
    [saveFn]
  )

  const [debouncedSave, _, callImmediately] = useDebouncedCallback(save, delay)

  useEffect(() => {
    if (state === 'dirty') {
      debouncedSave(value)
    }
  }, [debouncedSave, state, value])

  return { value, setValue, saveImmediately: callImmediately, state }
}
