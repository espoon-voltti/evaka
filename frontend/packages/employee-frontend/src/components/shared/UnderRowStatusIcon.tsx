{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasCheckCircle, fasExclamationTriangle } from 'icon-set'
import { BaseProps } from 'components/shared/utils'
import colors from '@evaka/lib-components/src/colors'

export const StatusIcon = styled.div`
  font-size: 15px;
  margin-left: ${defaultMargins.xs};
`

export type InfoStatus = 'warning' | 'success'

interface UnderRowStatusIconProps extends BaseProps {
  status?: InfoStatus
}

function UnderRowStatusIcon({ status }: UnderRowStatusIconProps) {
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
