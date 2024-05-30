// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'
import { FormEvent, useCallback, useEffect, useRef, useState } from 'react'

import { Failure, Result } from 'lib-common/api'
import { isAutomatedTest } from 'lib-common/utils/helpers'

const onSuccessTimeout = isAutomatedTest ? 10 : 800
const clearStateTimeout = isAutomatedTest ? 25 : 3000

type ButtonState<T> =
  | { state: 'idle' | 'in-progress' | 'failure' }
  | { state: 'success'; value: T }

/* eslint-disable @typescript-eslint/no-explicit-any */

const idle: ButtonState<any> = { state: 'idle' }
const inProgress: ButtonState<any> = { state: 'in-progress' }
const failure: ButtonState<any> = { state: 'failure' }

/* eslint-enable @typescript-eslint/no-explicit-any */

export interface AsyncButtonBehaviorProps<T> {
  /** Return a promise to start an async action, or `undefined` to do a sync action (or nothing at all) */
  onClick: () => Promise<Result<T>> | void
  /** Called when the promise has resolved with a Success value and the success animation has finished */
  onSuccess: (value: T) => void
  /** Called immediately when the promise has resolved with a Failure value */
  onFailure?: (failure: Failure<T>) => void
  preventDefault?: boolean
  stopPropagation?: boolean
}

export function useAsyncButtonBehavior<T>({
  preventDefault = false,
  stopPropagation = false,
  onClick,
  onSuccess,
  onFailure
}: AsyncButtonBehaviorProps<T>) {
  const [buttonState, setButtonState] = useState<ButtonState<T>>(idle)
  const onSuccessRef = useRef(onSuccess)

  const mountedRef = useRef(true)
  useEffect(
    () => () => {
      mountedRef.current = false
    },
    []
  )

  const handleSuccess = useCallback((value: T) => {
    setButtonState({ state: 'success', value })
  }, [])

  const handleFailure = useCallback(
    (value: Failure<T> | undefined) => {
      if (!mountedRef.current) return
      setButtonState(failure)
      onFailure && value !== undefined && onFailure(value)
    },
    [onFailure]
  )

  const handleClick = useCallback(
    (e: FormEvent) => {
      if (preventDefault) e.preventDefault()
      if (stopPropagation) e.stopPropagation()

      if (!mountedRef.current) return
      if (
        buttonState.state === 'in-progress' ||
        buttonState.state === 'success'
      )
        return

      const maybePromise = onClick()
      if (maybePromise === undefined) {
        // The click handler didn't do an async call, nothing to do here
      } else {
        setButtonState(inProgress)
        maybePromise
          .then((result) => {
            if (!mountedRef.current) return
            if (result.isSuccess) {
              handleSuccess(result.value)
            }
            if (result.isLoading) {
              handleFailure(undefined)
              Sentry.captureMessage(
                'BUG: AsyncButton promise resolved to a Loading value',
                'error'
              )
            } else if (result.isFailure) {
              handleFailure(result)
            } else {
              handleSuccess(result.value)
            }
          })
          .catch((originalErr: unknown) => {
            handleFailure(undefined)
            if (originalErr instanceof Error) {
              if ('message' in originalErr && 'stack' in originalErr) {
                const err = new Error(
                  `AsyncButton promise was rejected: ${originalErr.message}`
                )
                err.stack = originalErr.stack
                Sentry.captureException(err)
              } else {
                Sentry.captureException(originalErr)
              }
            }
          })
      }
    },
    [
      buttonState.state,
      preventDefault,
      stopPropagation,
      onClick,
      handleSuccess,
      handleFailure
    ]
  )

  useEffect(() => {
    onSuccessRef.current = (value: T) => onSuccess(value)
  }, [onSuccess])

  useEffect(() => {
    if (buttonState.state === 'success') {
      const runOnSuccess = setTimeout(
        () => onSuccessRef.current(buttonState.value),
        onSuccessTimeout
      )
      const clearState = setTimeout(
        () => mountedRef.current && setButtonState(idle),
        clearStateTimeout
      )
      return () => {
        clearTimeout(runOnSuccess)
        clearTimeout(clearState)
      }
    } else if (buttonState.state === 'failure') {
      const clearState = setTimeout(
        () => mountedRef.current && setButtonState(idle),
        clearStateTimeout
      )
      return () => clearTimeout(clearState)
    }
    return undefined
  }, [buttonState])

  return { state: buttonState.state, handleClick }
}
