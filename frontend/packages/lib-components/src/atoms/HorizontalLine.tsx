// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { defaultMargins } from '../white-space'
import colors from '../colors'

const HorizontalLine = styled.hr`
  width: 100%;
  margin-block-start: ${defaultMargins.XL};
  margin-block-end: ${defaultMargins.XL};
  border: none;
  border-bottom: 1px solid ${colors.greyscale.lighter};
`

export default HorizontalLine
