/*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/

import styled from 'styled-components'
import { defaultMargins } from '../white-space'
import { BaseProps } from '../utils'

type Sizes =
  | '100%'
  | '50%'
  | '33.3333%'
  | '25%'
  | '20%'
  | '16.6666%'
  | '66.6666%'
  | '75%'
  | '80%'
  | '83.3333%'

type ColumnProps = BaseProps & {
  desktopWidth?: Sizes
  tabletWidth?: Sizes
}

export const Column = styled.div<ColumnProps>`
  display: block;
  padding: ${defaultMargins.s};
  width: ${(p: ColumnProps) => p.desktopWidth ?? '100%'};

  @media screen and (min-width: 769px) {
    width: ${(p: ColumnProps) => p.tabletWidth ?? '100%'};
  }
`

export default Column
