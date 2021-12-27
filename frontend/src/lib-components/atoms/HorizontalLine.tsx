// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { defaultMargins } from '../white-space'
import { tabletMin } from '../breakpoints'

const HorizontalLine = styled.hr<{ slim?: boolean; dashed?: boolean }>`
  width: 100%;
  margin-block-start: ${(p) => (p.slim ? defaultMargins.m : defaultMargins.XL)};
  margin-block-end: ${(p) => (p.slim ? defaultMargins.m : defaultMargins.XL)};
  border: none;
  border-bottom-width: 1px;
  border-bottom-style: ${(p) => (p.dashed ? 'dashed' : 'solid')};
  border-bottom-color: ${(p) => p.theme.colors.greyscale.lighter};

  @media (max-width: ${tabletMin}) {
    margin-block-start: ${(p) =>
      p.slim ? defaultMargins.s : defaultMargins.L};
    margin-block-end: ${(p) => (p.slim ? defaultMargins.s : defaultMargins.L)};
  }
`

export default HorizontalLine
