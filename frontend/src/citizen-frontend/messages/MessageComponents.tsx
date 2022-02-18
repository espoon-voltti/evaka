// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { desktopMin } from 'lib-components/breakpoints'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

export const MessageContainer = styled.div`
  background-color: ${colors.grayscale.g0};
  padding: ${defaultMargins.s};

  @media (min-width: ${desktopMin}) {
    padding: ${defaultMargins.L};
  }

  margin-top: ${defaultMargins.s};

  h2 {
    margin: 0;
  }
`
