// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck } from '@evaka/lib-icons'
import colors from '../../colors'
import { BaseProps } from '../../utils'

const StyledButton = styled.button`
  display: flex;
  justify-content: center;
  align-items: center;
  width: 34px;
  height: 34px;
  font-size: 24px;
  color: ${colors.greyscale.dark};
  border: none;
  background: none;
  outline: none;
  padding: 0;
  margin: 0;
  cursor: pointer;

  &.active {
    color: ${colors.greyscale.white};
    background-color: ${colors.accents.green};
    border-radius: 100%;
  }
`

interface CheckIconButtonProps extends BaseProps {
  onClick?: () => void
  active: boolean
}

function CheckIconButton({
  className,
  dataQa,
  onClick,
  active
}: CheckIconButtonProps) {
  return (
    <StyledButton
      className={classNames(className, { active })}
      data-qa={dataQa}
      onClick={onClick}
    >
      <FontAwesomeIcon icon={faCheck} />
    </StyledButton>
  )
}

export default CheckIconButton
