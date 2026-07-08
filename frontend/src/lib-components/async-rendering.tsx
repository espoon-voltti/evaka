// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled, { useTheme } from 'styled-components'

import type { Failure, Result } from 'lib-common/api'
import type { FailureMessage } from 'lib-components/atoms/state/ErrorSegment'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import {
  SpinnerOverlay,
  SpinnerSegment
} from 'lib-components/atoms/state/Spinner'
import { useIsOnline } from 'lib-components/utils/useIsOnline'
import { faGear, faGlobe, faLockAlt } from 'lib-icons'

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

export interface FailureMessages {
  generic: FailureMessage
  http403: FailureMessage
  endpointDisabled: FailureMessage
  network: FailureMessage
}

function FailureSegment({
  result,
  messages
}: {
  result: Failure<unknown>
  messages: FailureMessages
}) {
  const { colors } = useTheme()
  const online = useIsOnline()

  if (!online) {
    return (
      <ErrorSegment
        {...messages.network}
        icon={faGlobe}
        iconColor={colors.status.warning}
      />
    )
  }

  if (result.errorCode === 'ENDPOINT_DISABLED') {
    return <ErrorSegment {...messages.endpointDisabled} icon={faGear} />
  }

  if (result.statusCode === 403) {
    return (
      <ErrorSegment
        {...messages.http403}
        icon={faLockAlt}
        iconColor={colors.main.m2}
      />
    )
  }

  return <ErrorSegment {...messages.generic} />
}

export function makeHelpers(useFailureMessage: () => FailureMessages) {
  function UnwrapResult<T>({
    result,
    loading,
    failure,
    children
  }: UnwrapResultProps<T>) {
    const failureMessages = useFailureMessage()
    const online = useIsOnline()
    const { colors } = useTheme()
    return useMemo(() => {
      if (
        result.isLoading ||
        (result.isSuccess &&
          result.isReloading &&
          (!children || children.length === 1))
      ) {
        if (loading) return loading()
        return online ? (
          <SpinnerSegment />
        ) : (
          <ErrorSegment
            {...failureMessages.network}
            icon={faGlobe}
            iconColor={colors.status.warning}
          />
        )
      }

      if (result.isFailure) {
        return failure ? (
          failure()
        ) : (
          <FailureSegment result={result} messages={failureMessages} />
        )
      }

      if (!children) {
        return null
      }

      return children(result.value, result.isReloading)
    }, [failureMessages, online, colors, result, loading, failure, children])
  }

  interface RenderResultProps<T> {
    result: Result<T>
    renderer: RenderResultFn<T>
  }

  const Relative = styled.div`
    position: relative;
  `

  function RenderResult<T>({ result, renderer }: RenderResultProps<T>) {
    const failureMessages = useFailureMessage()
    const online = useIsOnline()
    const { colors } = useTheme()
    return useMemo(
      () =>
        result.isLoading ? (
          online ? (
            <SpinnerSegment />
          ) : (
            <ErrorSegment
              {...failureMessages.network}
              icon={faGlobe}
              iconColor={colors.status.warning}
            />
          )
        ) : (
          <Relative>
            {result.isSuccess && result.isReloading && <SpinnerOverlay />}
            {result.isFailure ? (
              <FailureSegment result={result} messages={failureMessages} />
            ) : result.isSuccess ? (
              renderer(result.value, result.isReloading)
            ) : null}
          </Relative>
        ),
      [result, renderer, failureMessages, online, colors]
    )
  }

  function renderResult<T>(result: Result<T>, renderer: RenderResultFn<T>) {
    return <RenderResult result={result} renderer={renderer} />
  }

  return { UnwrapResult, renderResult }
}
