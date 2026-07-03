// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTheme } from 'styled-components'

import { faGlobe } from 'lib-icons'

import type { FailureMessage } from './ErrorSegment'
import ErrorSegment from './ErrorSegment'

export default React.memo(function OfflineSegment({
  title,
  info
}: FailureMessage) {
  const { colors } = useTheme()
  return (
    <ErrorSegment
      title={title}
      info={info}
      icon={faGlobe}
      iconColor={colors.status.warning}
    />
  )
})
