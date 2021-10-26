// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { memo } from 'lib-common/memo'
import { desktopMin } from 'lib-components/breakpoints'
import { defaultMargins } from 'lib-components/white-space'
import { cityLogo } from 'lib-customizations/citizen'

export default memo(function Logo() {
  return (
    <Container>
      <Img src={cityLogo.src} alt={cityLogo.alt} data-qa="header-city-logo" />
    </Container>
  )
})

const Container = styled.div`
  padding: ${defaultMargins.xs} 0;
  margin-left: ${defaultMargins.xs};

  @media (min-width: ${desktopMin}) {
    margin-left: ${defaultMargins.L};
  }
`

const Img = styled.img`
  max-width: 150px;
  width: auto;
  height: 100%;
`
