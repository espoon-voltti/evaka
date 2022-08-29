// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { NavLink } from 'react-router-dom'
import styled, { css } from 'styled-components'

import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

const dropDownButtonStyles = css`
  display: inline-flex;
  flex-direction: row;
  gap: ${defaultMargins.xs};
  align-items: center;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: Open Sans;
  color: ${colors.grayscale.g100};
  font-size: 1.125rem;
  font-weight: ${fontWeights.semibold};
  line-height: 2rem;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-bottom: 4px solid transparent;

  &:hover {
    color: ${colors.main.m2Hover};
  }

  &.active {
    color: ${colors.main.m2};
    border-bottom-color: ${colors.main.m2};
  }
`

export const DropDownButton = styled.button`
  ${dropDownButtonStyles}
`

export const DropDownLink = styled(NavLink)`
  ${dropDownButtonStyles}
`

export const CircledChar = styled.div.attrs({
  className: 'circled-char'
})`
  width: ${defaultMargins.s};
  height: ${defaultMargins.s};
  border: 1px solid ${colors.grayscale.g100};
  padding: 11px;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 100%;
  letter-spacing: 0;
`
