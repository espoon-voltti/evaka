// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { onlineManager } from '@tanstack/react-query'
import React, { useSyncExternalStore } from 'react'

import type { SpacingSize } from '../../white-space'

import NetworkSegment from './NetworkSegment'
import { SpinnerSegment } from './Spinner'

interface LoadingSegmentProps {
  networkTitle: string
  networkInfo?: string
  size?: SpacingSize
  margin?: SpacingSize
}

const subscribe = (onChange: () => void) => onlineManager.subscribe(onChange)
const getOnline = () => onlineManager.isOnline()

export default React.memo(function LoadingSegment({
  networkTitle,
  networkInfo,
  size,
  margin
}: LoadingSegmentProps) {
  const online = useSyncExternalStore(subscribe, getOnline)

  if (!online) {
    return <NetworkSegment title={networkTitle} info={networkInfo} />
  }

  return <SpinnerSegment size={size} margin={margin} />
})
