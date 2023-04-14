// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { css } from 'styled-components'

import { fontWeights } from '../../typography'

export const defaultButtonTextStyle = css`
  color: ${(p) => p.theme.colors.main.m2};
  font-family: 'Open Sans', sans-serif;
  font-size: 1em;
  line-height: normal;
  font-weight: ${fontWeights.semibold};
  white-space: nowrap;
  letter-spacing: 0;
`

export const buttonBorderRadius = '4px'
