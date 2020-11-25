{/*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/}

import styled from 'styled-components'
import { DefaultMargins } from 'components/shared/layout/white-space'
import { BaseProps } from 'components/shared/utils'

export const Container = styled.div`
  margin: 0 auto;
  position: relative;

  @media screen and (min-width: 1024px) {
    max-width: 960px;
    width: 960px;
  }
  @media screen and (max-width: 1215px) {
    max-width: 1152px;
    width: auto;
  }
  @media screen and (max-width: 1407px) {
    max-width: 1344px;
    width: auto;
  }
  @media screen and (min-width: 1216px) {
    max-width: 1152px;
    width: 1152px;
  }
  @media screen and (min-width: 1408px) {
    max-width: 1344px;
    width: 1344px;
  }
`

type sizes =
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

interface ColumnProps extends BaseProps {
  desktopWidth?: sizes
  tabletWidth?: sizes
}

export const Column = styled.div<ColumnProps>`
  display: block;
  padding: ${DefaultMargins.s};
  width: ${(p: ColumnProps) => p.desktopWidth ?? '100%'};

  @media screen and (min-width: 769px) {
    width: ${(p: ColumnProps) => p.tabletWidth ?? '100%'};
  }
`

export default Column
