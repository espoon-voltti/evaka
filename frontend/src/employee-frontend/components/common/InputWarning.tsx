// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { fasExclamationTriangle } from 'lib-icons'

interface Props {
  text: string
  iconPosition?: 'before' | 'after'
}

export function InputWarning({ text, iconPosition }: Props) {
  return iconPosition === 'after' ? (
    <div>
      <WarningText margin="right">{text}</WarningText>
      <WarningIcon />
    </div>
  ) : (
    <div>
      <WarningIcon />
      <WarningText>{text}</WarningText>
    </div>
  )
}

const WarningText = styled.span<{
  smaller?: boolean
  margin?: 'left' | 'right'
}>`
  color: ${colors.grayscale.g70};
  font-style: italic;
  ${(props) =>
    props.margin === 'right' ? 'margin-right: 12px' : 'margin-left: 12px'};
  font-size: ${(props) => (props.smaller === true ? '0.8em' : '1em')};
`

const WarningIcon = () => (
  <FontAwesomeIcon
    icon={fasExclamationTriangle}
    size="1x"
    color={colors.status.warning}
  />
)
