// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { defaultMargins } from '../white-space'
import colors from '../colors'
import { tabletMin } from '../breakpoints'

const HorizontalLine = styled.hr`
  width: 100%;
  margin-block-start: ${defaultMargins.XL};
  margin-block-end: ${defaultMargins.XL};
  border: none;
  border-bottom: 1px solid ${colors.greyscale.lighter};

  @media (max-width: ${tabletMin}) {
    margin-block-start: ${defaultMargins.L};
    margin-block-end: ${defaultMargins.L};
  }
`

export default HorizontalLine
