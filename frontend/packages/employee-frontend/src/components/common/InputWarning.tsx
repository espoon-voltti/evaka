// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasExclamationTriangle } from '~icon-set'
import React from 'react'

interface Props {
  text: string
  iconPosition?: 'before' | 'after'
}

export function InputWarning({ text, iconPosition }: Props) {
  return iconPosition === 'after' ? (
    <div>
      <WarningText margin={'right'}>{text}</WarningText>
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
  color: ${colors.greyscale.dark};
  font-style: italic;
  ${(props) =>
    props.margin === 'right' ? 'margin-right: 12px' : 'margin-left: 12px'};
  font-size: ${(props) => (props.smaller === true ? '0.8em' : '1em')};
`

const WarningIcon = () => (
  <FontAwesomeIcon
    icon={fasExclamationTriangle}
    size={'1x'}
    color={colors.accents.orange}
  />
)
