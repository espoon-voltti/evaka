// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import React, { useMemo } from 'react'
import {
  LoadableContent,
  SpinnerSegment
} from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'

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

export function makeHelpers(useFailureMessage: () => string) {
  function UnwrapResult<T>({
    result,
    loading,
    failure,
    children
  }: UnwrapResultProps<T>) {
    const failureMessage = useFailureMessage()
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
        return <ErrorSegment title={failureMessage} />
      }
      if (!children) {
        return null
      }
      return children(result.value, result.isReloading)
    }, [failureMessage, result, loading, failure, children])
  }

  interface RenderResultProps<T> {
    result: Result<T>
    renderer: RenderResultFn<T>
  }

  function RenderResult<T>({ result, renderer }: RenderResultProps<T>) {
    const failureMessage = useFailureMessage()
    return useMemo(
      () =>
        result.isLoading ? (
          <SpinnerSegment />
        ) : (
          <LoadableContent loading={result.isSuccess && result.isReloading}>
            {result.isFailure ? (
              <ErrorSegment title={failureMessage} />
            ) : result.isSuccess ? (
              renderer(result.value, result.isReloading)
            ) : null}
          </LoadableContent>
        ),
      [result, renderer, failureMessage]
    )
  }

  function renderResult<T>(result: Result<T>, renderer: RenderResultFn<T>) {
    return <RenderResult result={result} renderer={renderer} />
  }

  return { UnwrapResult, renderResult }
}
