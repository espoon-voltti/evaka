// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { desktopMin } from '@evaka/lib-components/breakpoints'
import { defaultMargins } from '@evaka/lib-components/white-space'
import { cityLogo } from '@evaka/lib-customizations/citizen'

export default React.memo(function Logo() {
  return (
    <Container>
      <Img src={cityLogo.src} alt={cityLogo.alt} />
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
