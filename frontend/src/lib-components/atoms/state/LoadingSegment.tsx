// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useIsOnline } from '../../utils/useIsOnline'
import type { SpacingSize } from '../../white-space'

import NetworkSegment from './NetworkSegment'
import { SpinnerSegment } from './Spinner'

interface LoadingSegmentProps {
  networkTitle: string
  networkInfo?: string
  size?: SpacingSize
  margin?: SpacingSize
}

export default React.memo(function LoadingSegment({
  networkTitle,
  networkInfo,
  size,
  margin
}: LoadingSegmentProps) {
  const online = useIsOnline()

  if (!online) {
    return <NetworkSegment title={networkTitle} info={networkInfo} />
  }

  return <SpinnerSegment size={size} margin={margin} />
})
