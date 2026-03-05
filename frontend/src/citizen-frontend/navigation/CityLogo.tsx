// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { zoomedMobileMax } from 'lib-components/breakpoints'
import { defaultMargins } from 'lib-components/white-space'
import { cityLogo } from 'lib-customizations/citizen'

export default React.memo(function Logo() {
  return (
    <Container>
      <Img src={cityLogo.src} alt={cityLogo.alt} data-qa="header-city-logo" />
    </Container>
  )
})

const Container = styled.div`
  padding: ${defaultMargins.xs} 0;
  width: 150px;
`

const Img = styled.img`
  max-width: 150px;
  width: auto;
  height: 100%;
  @media (max-width: ${zoomedMobileMax}) {
    max-width: 60px;
    height: auto;
  }
`
