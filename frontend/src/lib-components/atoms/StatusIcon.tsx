// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasCheckCircle, fasExclamationTriangle } from 'lib-icons'
import React from 'react'
import styled, { useTheme } from 'styled-components'
import { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'

export const StatusIcon = styled.div`
  font-size: 15px;
  margin-left: ${defaultMargins.xs};
`

export type InfoStatus = 'warning' | 'success'

interface UnderRowStatusIconProps extends BaseProps {
  status?: InfoStatus
}

export default React.memo(function UnderRowStatusIcon({
  status
}: UnderRowStatusIconProps) {
  const { colors } = useTheme()
  return (
    <StatusIcon>
      {status === 'warning' && (
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={colors.accents.warningOrange}
        />
      )}
      {status === 'success' && (
        <FontAwesomeIcon
          icon={fasCheckCircle}
          color={colors.accents.successGreen}
        />
      )}
    </StatusIcon>
  )
})
