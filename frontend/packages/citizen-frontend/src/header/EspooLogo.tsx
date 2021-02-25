// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { desktopMin } from '@evaka/lib-components/src/breakpoints'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import EspooLogo from '../espoo-logo.svg'

export default React.memo(function Logo() {
  return <Img src={EspooLogo} alt="Espoo logo" />
})

const Img = styled.img`
  padding: ${defaultMargins.xs} 0;
  margin-left: ${defaultMargins.xs};
  max-width: 150px;
  width: auto;
  height: 100%;

  @media (min-width: ${desktopMin}) {
    margin-left: ${defaultMargins.L};
  }
`
