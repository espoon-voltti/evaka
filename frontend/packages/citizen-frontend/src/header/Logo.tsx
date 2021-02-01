// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { desktopMin } from '@evaka/lib-components/src/breakpoints'
import EspooLogo from '../espoo-logo.svg'

export default React.memo(function Logo() {
  return (
    <LogoContainer>
      <Img src={EspooLogo} alt="Espoo logo" />
    </LogoContainer>
  )
})

const LogoContainer = styled.div`
  flex-grow: 0;
  flex-shrink: 1;
  flex-basis: 20%;
  min-width: 180px;
`

const Img = styled.img`
  padding: 0;
  margin-left: 16px;
  max-width: 150px;
  width: auto;
  height: 100%;

  @media (min-width: ${desktopMin}) {
    padding: 0 24px;
  }
`
