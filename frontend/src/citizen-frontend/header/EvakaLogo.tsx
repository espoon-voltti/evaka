// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { EvakaLogo } from 'lib-components/atoms/EvakaLogo'
import { desktopMin, desktopSmall } from 'lib-components/breakpoints'

export default React.memo(function Logo() {
  return (
    <Container>
      <EvakaLogo />
    </Container>
  )
})

const Container = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: center;

  svg {
    max-width: 150px;
    width: auto;
    height: 100%;
  }

  @media (min-width: ${desktopMin}) and (max-width: calc(${desktopSmall} - 1px)) {
    svg {
      display: none;
    }
  }
`
