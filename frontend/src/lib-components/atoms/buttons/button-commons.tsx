// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { css } from 'styled-components'

import { isAutomatedTest } from 'lib-common/utils/helpers'

import { fontWeights } from '../../typography'

export const defaultButtonTextStyle = css`
  color: ${(p) => p.theme.colors.main.m2};
  font-family: 'Open Sans', sans-serif;
  font-size: 1em;
  line-height: normal;
  font-weight: ${fontWeights.semibold};
  white-space: nowrap;
  letter-spacing: 0;
`

export const buttonBorderRadius = '4px'

/**
 * Adds throttling to an event handler.
 *
 * Returns an event handler wrapper that passes events to the given event handler *only* if enough time has passed
 * before the previous event. This throttling is lossy because it will simply drop events.
 */
export function useThrottledEventHandler<
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  E extends React.SyntheticEvent<any>
>(
  onEvent: React.EventHandler<E> | undefined,
  timeoutMs = 300
): React.EventHandler<E> {
  const [ignoreEvent, setIgnoreEvent] = React.useState(false)
  React.useEffect(() => {
    if (ignoreEvent) {
      const id = setTimeout(() => setIgnoreEvent(false), timeoutMs)
      return () => clearTimeout(id)
    }
    return undefined
  }, [ignoreEvent, timeoutMs])

  return useCallback<React.EventHandler<E>>(
    (e) => {
      if (!ignoreEvent) {
        if (!isAutomatedTest) setIgnoreEvent(true)
        if (onEvent) onEvent(e)
      }
    },
    [ignoreEvent, onEvent]
  )
}
