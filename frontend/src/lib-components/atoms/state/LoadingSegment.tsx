// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useIsOnline } from '../../utils/useIsOnline'

import type { FailureMessage } from './ErrorSegment'
import OfflineSegment from './OfflineSegment'
import { SpinnerSegment } from './Spinner'

interface LoadingSegmentProps {
  network: FailureMessage
}

export default React.memo(function LoadingSegment({
  network
}: LoadingSegmentProps) {
  const online = useIsOnline()

  if (!online) {
    return <OfflineSegment {...network} />
  }

  return <SpinnerSegment />
})
