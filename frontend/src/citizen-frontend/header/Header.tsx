// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { desktopMin } from 'lib-components/breakpoints'
import EspooLogo from './EspooLogo'
import EvakaLogo from './EvakaLogo'
import DesktopNav from './DesktopNav'
import MobileNav from './MobileNav'
import { headerHeight } from './const'
import { useUser } from '../auth'
import { HeaderContext, HeaderState } from '../messages/state'

export default React.memo(function Header() {
  const [showMenu, setShowMenu] = useState(false)
  const user = useUser()

  const {
    unreadMessagesCount,
    refreshUnreadMessagesCount
  } = useContext<HeaderState>(HeaderContext)

  useEffect(() => {
    if (user) refreshUnreadMessagesCount()
  }, [user])

  return (
    <HeaderContainer fixed={showMenu}>
      <EspooLogo />
      <EvakaLogo />
      <DesktopNav unreadMessagesCount={unreadMessagesCount ?? 0} />
      <MobileNav
        showMenu={showMenu}
        setShowMenu={setShowMenu}
        unreadMessagesCount={unreadMessagesCount ?? 0}
      />
    </HeaderContainer>
  )
})

const HeaderContainer = styled.header<{ fixed: boolean }>`
  z-index: 99;
  color: ${colors.greyscale.white};
  background-color: ${colors.blues.primary};
  position: ${({ fixed }) => (fixed ? 'fixed' : 'relative')};
  display: grid;
  grid: minmax(60px, min-content) / repeat(3, minmax(100px, 1fr));
  height: 60px;
  width: 100%;

  @media (min-width: ${desktopMin}) {
    grid: minmax(${headerHeight}, min-content) / max-content max-content auto;
    height: ${headerHeight};
  }
`
