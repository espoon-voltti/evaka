// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DefaultTheme } from 'styled-components'
import { fontWeights } from '../../typography'

export const defaultButtonTextStyle = ({ colors }: DefaultTheme) => `
  color: ${colors.main.primary};
  font-family: 'Open Sans', sans-serif;
  font-size: 1em;
  line-height: normal;
  font-weight: ${fontWeights.semibold};
  white-space: nowrap;
  letter-spacing: 0;
`
