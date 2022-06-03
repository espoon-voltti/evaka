// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { tabletMin } from 'lib-components/breakpoints'
import { defaultMargins } from 'lib-components/white-space'

export const Mobile = styled.div`
  display: none;

  @media (max-width: ${tabletMin}) {
    display: block;
  }
`

export const Desktop = styled.div`
  display: none;

  @media (min-width: ${tabletMin}) {
    display: block;
  }
`

export const PaddedDiv = styled.div`
  padding: 0 ${defaultMargins.s};
`
