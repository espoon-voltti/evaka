// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import { useTranslation } from 'citizen-frontend/localization'
import { EvakaLogo } from 'lib-components/atoms/EvakaLogo'
import { desktopMin } from 'lib-components/breakpoints'
import colors from 'lib-customizations/common'

export default React.memo(function Logo() {
  const t = useTranslation()

  return (
    <Container to="/" aria-label={t.header.goToHomepage}>
      <EvakaLogo color={colors.main.m2} />
    </Container>
  )
})

const Container = styled(Link)`
  display: flex;
  flex-direction: row;
  justify-content: center;

  svg {
    max-width: 120px;
    width: auto;
    height: 100%;
  }

  @media (max-width: calc(${desktopMin} - 1px)) {
    svg {
      max-width: 80px;
    }
  }
`
