// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import {
  SpinnerOverlay,
  SpinnerSegment
} from 'lib-components/atoms/state/Spinner'

import { SpacingSize } from './white-space'

export type RenderResultFn<T> = (
  value: T,
  isReloading: boolean
) => React.ReactElement | null

export interface UnwrapResultProps<T> {
  result: Result<T>
  loading?: () => React.ReactElement | null
  failure?: () => React.ReactElement | null
  children?: RenderResultFn<T>
}

export interface SpinnerOptions {
  size?: SpacingSize
  margin?: SpacingSize
}

interface FailureMessages {
  generic: string
  http403: string
}

export function makeHelpers(useFailureMessage: () => FailureMessages) {
  function UnwrapResult<T>({
    result,
    loading,
    failure,
    children
  }: UnwrapResultProps<T>) {
    const failureMessages = useFailureMessage()
    return useMemo(() => {
      if (
        result.isLoading ||
        (result.isSuccess &&
          result.isReloading &&
          (!children || children.length === 1))
      ) {
        if (loading) return loading()
        return <SpinnerSegment />
      }
      if (result.isFailure) {
        if (failure) return failure()
        return (
          <ErrorSegment
            title={
              result.statusCode === 403
                ? failureMessages.http403
                : failureMessages.generic
            }
          />
        )
      }
      if (!children) {
        return null
      }
      return children(result.value, result.isReloading)
    }, [failureMessages, result, loading, failure, children])
  }

  interface RenderResultProps<T> {
    result: Result<T>
    renderer: RenderResultFn<T>
    spinnerOptions?: SpinnerOptions
  }

  const Relative = styled.div`
    position: relative;
  `

  function RenderResult<T>({
    result,
    renderer,
    spinnerOptions = {}
  }: RenderResultProps<T>) {
    const failureMessages = useFailureMessage()
    return useMemo(
      () =>
        result.isLoading ? (
          <SpinnerSegment
            size={spinnerOptions.size}
            margin={spinnerOptions.margin}
          />
        ) : (
          <Relative>
            {result.isSuccess && result.isReloading && <SpinnerOverlay />}
            {result.isFailure ? (
              <ErrorSegment
                title={
                  result.statusCode === 403
                    ? failureMessages.http403
                    : failureMessages.generic
                }
              />
            ) : result.isSuccess ? (
              renderer(result.value, result.isReloading)
            ) : null}
          </Relative>
        ),
      [result, renderer, failureMessages, spinnerOptions]
    )
  }

  function renderResult<T>(
    result: Result<T>,
    renderer: RenderResultFn<T>,
    spinnerOptions?: SpinnerOptions
  ) {
    return (
      <RenderResult
        result={result}
        renderer={renderer}
        spinnerOptions={spinnerOptions}
      />
    )
  }

  return { UnwrapResult, renderResult }
}
