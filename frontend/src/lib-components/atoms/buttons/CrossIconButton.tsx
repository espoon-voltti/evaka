// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React from 'react'
import styled from 'styled-components'

import { faTimes } from 'lib-icons'

import type { BaseProps } from '../../utils'

const StyledButton = styled.button`
  display: flex;
  justify-content: center;
  align-items: center;
  width: 34px;
  height: 34px;
  font-size: 24px;
  color: ${(p) => p.theme.colors.grayscale.g70};
  border: none;
  background: none;
  outline: none;
  padding: 0;
  margin: 0;
  cursor: pointer;

  &.active {
    color: ${(p) => p.theme.colors.grayscale.g0};
    background-color: ${(p) => p.theme.colors.status.danger};
    border-radius: 100%;
  }
`

interface CrossIconButtonProps extends BaseProps {
  onClick?: () => void
  active: boolean
}

export default React.memo(function CrossIconButton({
  className,
  'data-qa': dataQa,
  onClick,
  active
}: CrossIconButtonProps) {
  return (
    <StyledButton
      type="button"
      className={classNames(className, { active })}
      data-qa={dataQa}
      onClick={onClick}
    >
      <FontAwesomeIcon icon={faTimes} />
    </StyledButton>
  )
})
