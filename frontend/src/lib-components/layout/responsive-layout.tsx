// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { desktopMin, tabletMin } from '../breakpoints'

export const MobileOnly = styled.div`
  display: block;
  @media (min-width: ${tabletMin}) {
    display: none;
  }
`

export const MobileAndTablet = styled.div`
  display: block;
  @media (min-width: ${desktopMin}) {
    display: none;
  }
`

export const Desktop = styled.div`
  display: none;
  @media (min-width: ${desktopMin}) {
    display: block;
  }
`
