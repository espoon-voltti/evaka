// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'
import Colors from 'components/shared/Colors'
import classNames from 'classnames'
import { BaseProps } from 'components/shared/utils'
import { faTimes } from '@evaka/icons'

const StyledButton = styled.button`
  display: flex;
  justify-content: center;
  align-items: center;
  width: 34px;
  height: 34px;
  font-size: 24px;
  color: ${Colors.greyscale.dark};
  border: none;
  background: none;
  outline: none;
  padding: 0;
  margin: 0;
  cursor: pointer;

  &.active {
    color: ${Colors.greyscale.white};
    background-color: ${Colors.accents.red};
    border-radius: 100%;
  }
`

interface CrossIconButtonProps extends BaseProps {
  onClick?: () => void
  active: boolean
}

function CrossIconButton({
  className,
  dataQa,
  onClick,
  active
}: CrossIconButtonProps) {
  return (
    <StyledButton
      className={classNames(className, { active })}
      data-qa={dataQa}
      onClick={onClick}
    >
      <FontAwesomeIcon icon={faTimes} />
    </StyledButton>
  )
}

export default CrossIconButton
