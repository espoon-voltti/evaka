// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { desktopMin } from '@evaka/lib-components/src/breakpoints'
import Logo from './Logo'
import DesktopNav from './DesktopNav'
import MobileNav from './MobileNav'
import { enduserBaseUrl, headerHeight } from '~header/const'

export default React.memo(function Header() {
  const [showMenu, setShowMenu] = useState(false)

  return (
    <HeaderContainer fixed={showMenu}>
      <Logo />
      <DesktopNavContainer>
        <DesktopNav enduserBaseUrl={enduserBaseUrl} />
      </DesktopNavContainer>
      <MobileNavContainer>
        <MobileNav
          enduserBaseUrl={enduserBaseUrl}
          showMenu={showMenu}
          setShowMenu={setShowMenu}
        />
      </MobileNavContainer>
    </HeaderContainer>
  )
})

const HeaderContainer = styled.header<{ fixed: boolean }>`
  z-index: 99;
  color: ${colors.greyscale.white};
  background-color: ${colors.brandEspoo.espooBlue};
  position: ${({ fixed }) => (fixed ? 'fixed' : 'relative')};
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  height: ${headerHeight};
  width: 100%;

  @media (min-width: ${desktopMin}) {
    height: 64px;
    justify-content: flex-start;
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
