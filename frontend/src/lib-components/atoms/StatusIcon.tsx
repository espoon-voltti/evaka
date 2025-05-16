// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import type { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'
import { fasCheckCircle, fasExclamationTriangle } from 'lib-icons'

export const StatusIcon = styled(FontAwesomeIcon)`
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
  return status === 'warning' ? (
    <StatusIcon icon={fasExclamationTriangle} color={colors.status.warning} />
  ) : status === 'success' ? (
    <StatusIcon icon={fasCheckCircle} color={colors.status.success} />
  ) : null
})
