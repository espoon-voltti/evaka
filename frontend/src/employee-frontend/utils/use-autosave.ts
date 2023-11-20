// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'

import { ApiFunction, Failure, Result, Success } from 'lib-common/api'
import { isAutomatedTest } from 'lib-common/utils/helpers'
import { useDebouncedCallback } from 'lib-common/utils/useDebouncedCallback'
import { useRestApi } from 'lib-common/utils/useRestApi'

type State =
  | 'loading'
  | 'loading-dirty'
  | 'loading-error'
  | 'clean'
  | 'dirty'
  | 'saving'
  | 'saving-dirty'
  | 'save-error'

export interface AutosaveStatus {
  state: State
  savedAt?: Date
}

const debounceInterval = isAutomatedTest ? 200 : 2000

export type Autosave = {
  status: AutosaveStatus
  setStatus: React.Dispatch<React.SetStateAction<AutosaveStatus>>
  setDirty: () => void
  forceSave: () => Promise<Result<void>>
}

export type AutosaveParams<T, F extends ApiFunction> = {
  load: () => Promise<Result<T>>
  onLoaded: (loadedItem: T) => void
  save: F
  getSaveParameters: () => Parameters<F>
}

export function useAutosave<T, F extends ApiFunction>({
  load,
  onLoaded,
  save,
  getSaveParameters
}: AutosaveParams<T, F>): Autosave {
  const [status, setStatus] = useState<AutosaveStatus>({ state: 'loading' })

  const handleLoaded = useCallback(
    (res: Result<T>) => {
      res.mapAll({
        loading: () => null,
        failure: () => setStatus({ state: 'loading-error' }),
        success: (loadedItem: T) => {
          onLoaded && onLoaded(loadedItem)
          setStatus((prev) =>
            prev.state === 'loading-dirty'
              ? { ...prev, state: 'dirty' }
              : { state: 'clean' }
          )
        }
      })
    },
    [onLoaded]
  )

  useEffect(
    function loadItem() {
      setStatus({ state: 'loading' })
      void load().then(handleLoaded)
    },
    [load, handleLoaded]
  )

  const handleSaveResult = useCallback((res: Result<unknown>) => {
    res.mapAll({
      loading: () => null,
      failure: () => setStatus((prev) => ({ ...prev, state: 'save-error' })),
      success: () => {
        setStatus((prev) => {
          if (prev.state === 'saving-dirty') {
            return {
              ...prev,
              state: 'dirty'
            }
          }

          return { state: 'clean', savedAt: new Date() }
        })
      }
    })
  }, [])

  const internalSave = useRestApi(save, handleSaveResult)

  const saveNow = useCallback(() => {
    setStatus((prev) => ({ ...prev, state: 'saving' }))
    return internalSave(...getSaveParameters()).then((res) =>
      res.map(() => undefined)
    )
  }, [internalSave, getSaveParameters])

  const [debouncedSave] = useDebouncedCallback(saveNow, debounceInterval)

  const setDirty = useCallback(
    () =>
      setStatus((prev) => {
        const state =
          prev.state === 'loading'
            ? 'loading-dirty'
            : prev.state === 'saving'
              ? 'saving-dirty'
              : 'dirty'
        return { ...prev, state }
      }),
    []
  )

  useEffect(
    function saveDirtyContent() {
      if (status.state === 'dirty') {
        debouncedSave()
      }
    },
    [debouncedSave, status.state]
  )

  const [waitingForceSaves, setWaitingForceSaves] = useState<
    ((result: Result<void>) => void)[]
  >([])

  useEffect(() => {
    if (
      waitingForceSaves.length > 0 &&
      (status.state === 'clean' || status.state === 'save-error')
    ) {
      waitingForceSaves.forEach((fn) =>
        fn(
          status.state === 'clean'
            ? Success.of()
            : Failure.of({
                message: 'Save error'
              })
        )
      )
      setWaitingForceSaves([])
    }
  }, [status.state, waitingForceSaves])

  return {
    status,
    setStatus,
    setDirty,
    forceSave() {
      const promise = new Promise<Result<void>>((resolve) => {
        setWaitingForceSaves((s) => [...s, resolve])
      })
      void saveNow()
      return promise
    }
  }
}
