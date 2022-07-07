// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { useTranslation } from 'citizen-frontend/localization'
import { EvakaLogo } from 'lib-components/atoms/EvakaLogo'
import { desktopMin, desktopSmall } from 'lib-components/breakpoints'

export default React.memo(function Logo() {
  const t = useTranslation()

  return (
    <Container to="/" aria-label={t.header.goToHomepage}>
      <EvakaLogo />
    </Container>
  )
})

const Container = styled(Link)`
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
