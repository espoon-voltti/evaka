{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'
import { defaultMargins } from 'lib-components/white-space'
import styled, { useTheme } from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasCheckCircle, fasExclamationTriangle } from 'lib-icons'
import { BaseProps } from 'lib-components/utils'

export const StatusIcon = styled.div`
  font-size: 15px;
  margin-left: ${defaultMargins.xs};
`

export type InfoStatus = 'warning' | 'success'

interface UnderRowStatusIconProps extends BaseProps {
  status?: InfoStatus
}

function UnderRowStatusIcon({ status }: UnderRowStatusIconProps) {
  const { colors } = useTheme()
  return (
    <StatusIcon>
      {status === 'warning' && (
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={colors.accents.orange}
        />
      )}
      {status === 'success' && (
        <FontAwesomeIcon icon={fasCheckCircle} color={colors.accents.green} />
      )}
    </StatusIcon>
  )
}

export default UnderRowStatusIcon
