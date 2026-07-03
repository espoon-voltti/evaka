// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { onlineManager } from '@tanstack/react-query'
import React, { useMemo } from 'react'
import styled, { useTheme } from 'styled-components'

import type { Failure, Result } from 'lib-common/api'
import type { Theme } from 'lib-common/theme'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import LoadingSegment from 'lib-components/atoms/state/LoadingSegment'
import NetworkSegment from 'lib-components/atoms/state/NetworkSegment'
import {
  SpinnerOverlay,
  SpinnerSegment
} from 'lib-components/atoms/state/Spinner'
import { faExclamationTriangle, faGear, faLockAlt } from 'lib-icons'

import type { SpacingSize } from './white-space'

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

export interface FailureMessage {
  title: string
  text?: string
}

export interface FailureMessages {
  generic: FailureMessage
  http403: FailureMessage
  endpointDisabled: FailureMessage
  network: FailureMessage
}

interface FailureContent extends FailureMessage {
  icon: IconDefinition
  iconColor: string
}

function failureContent(
  result: Failure<unknown>,
  messages: FailureMessages,
  colors: Theme['colors']
): FailureContent {
  if (result.errorCode === 'ENDPOINT_DISABLED') {
    return {
      ...messages.endpointDisabled,
      icon: faGear,
      iconColor: colors.grayscale.g70
    }
  }

  if (result.statusCode === 403) {
    return { ...messages.http403, icon: faLockAlt, iconColor: colors.main.m2 }
  }

  return {
    ...messages.generic,
    icon: faExclamationTriangle,
    iconColor: colors.grayscale.g70
  }
}

function FailureSegment({
  result,
  messages
}: {
  result: Failure<unknown>
  messages: FailureMessages
}) {
  const { colors } = useTheme()

  if (!onlineManager.isOnline()) {
    return (
      <NetworkSegment
        title={messages.network.title}
        info={messages.network.text}
      />
    )
  }

  const { title, text, icon, iconColor } = failureContent(
    result,
    messages,
    colors
  )
  return (
    <ErrorSegment title={title} info={text} icon={icon} iconColor={iconColor} />
  )
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
      if (result.isLoading) {
        return loading ? (
          loading()
        ) : (
          <LoadingSegment
            networkTitle={failureMessages.network.title}
            networkInfo={failureMessages.network.text}
          />
        )
      }

      if (
        result.isSuccess &&
        result.isReloading &&
        (!children || children.length === 1)
      ) {
        return loading ? loading() : <SpinnerSegment />
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

  const empty: SpinnerOptions = {}

  function RenderResult<T>({
    result,
    renderer,
    spinnerOptions = empty
  }: RenderResultProps<T>) {
    const failureMessages = useFailureMessage()
    return useMemo(
      () =>
        result.isLoading ? (
          <LoadingSegment
            networkTitle={failureMessages.network.title}
            networkInfo={failureMessages.network.text}
            size={spinnerOptions.size}
            margin={spinnerOptions.margin}
          />
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
