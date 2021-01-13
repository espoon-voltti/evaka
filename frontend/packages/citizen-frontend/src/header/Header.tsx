// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { desktopMin } from '@evaka/lib-components/src/breakpoints'
import CityLogo from '@evaka/frontend-customization/assets/city-logo.svg'
import DesktopNav from './DesktopNav'
import MobileNav from './MobileNav'

const enduserBaseUrl =
  window.location.host === 'localhost:9094' ? 'http://localhost:9091' : ''

export default React.memo(function Header() {
  return (
    <HeaderContainer>
      <LogoContainer>
        <Logo src={CityLogo} alt="City logo" />
      </LogoContainer>
      <DesktopNavContainer>
        <DesktopNav enduserBaseUrl={enduserBaseUrl} />
      </DesktopNavContainer>
      <MobileNavContainer>
        <MobileNav enduserBaseUrl={enduserBaseUrl} />
      </MobileNavContainer>
    </HeaderContainer>
  )
})

const HeaderContainer = styled.header`
  z-index: 99;
  color: ${colors.greyscale.white};
  background-color: ${colors.cityBrandColors.primary};
  position: relative;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  height: 52px;

  @media (min-width: ${desktopMin}) {
    height: 64px;
    justify-content: flex-start;
  }
`

const LogoContainer = styled.div`
  flex-grow: 0;
  flex-shrink: 1;
  flex-basis: 20%;
  min-width: 180px;
`

const Logo = styled.img`
  padding: 0;
  margin-left: 1rem;
  max-width: 150px;
  width: auto;
  height: 100%;

  @media (min-width: ${desktopMin}) {
    padding: 0 1.5rem;
  }
`

const DesktopNavContainer = styled.div`
  display: none;

  @media (min-width: ${desktopMin}) {
    display: flex;
    flex-direction: row;
    flex-grow: 1;
  }
`

const MobileNavContainer = styled.div`
  display: flex;
  flex-direction: row;

  @media (min-width: ${desktopMin}) {
    display: none;
  }
`
